package com.ringoftime;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentOrientation;

/**
 * Hosts one or more timer rings in a resizable wrapping overlay.
 */
final class TimerGroupOverlay extends OverlayPanel
{
	static final String DETACH_OPTION = "Detach";
	static final String FLIP_OPTION = "Flip";

	private static final int DEFAULT_WRAP_COUNT = 4;

	private final Client client;
	private final RingOfTimeConfig config;
	private final TimerGroupManager manager;
	private final String name;
	private final List<TimerCircleOverlay> members = new CopyOnWriteArrayList<>();
	private final Map<TimerCircleOverlay, Rectangle> visibleMemberBounds = new LinkedHashMap<>();

	private ComponentOrientation orientation;
	private TimerCircleOverlay hoveredTimer;
	private Point lastAutomaticLocation;
	private boolean automaticLayoutEnabled;

	TimerGroupOverlay(
		Client client,
		RingOfTimePlugin plugin,
		RingOfTimeConfig config,
		TimerGroupManager manager,
		String name,
		ComponentOrientation orientation)
	{
		super(plugin);
		this.client = client;
		this.config = config;
		this.manager = manager;
		this.name = name;
		this.orientation = orientation;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_MED);
		setMovable(true);
		setSnappable(true);
		setResettable(true);
		setResizable(true);
		setMinimumSize(32);
		setDragTargetable(true);
		setClearChildren(false);

		panelComponent.setBackgroundColor(null);
		panelComponent.setBorder(new Rectangle());
		panelComponent.setWrap(true);
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final boolean menuOpen = client.isMenuOpen();
		if (!menuOpen)
		{
			hoveredTimer = null;
		}

		final Dimension cellSize = TimerLabelLayout.estimateMaximumDimension(config);
		final int horizontalGap = config.ringGroupHorizontalSpacing();
		final int verticalGap = config.ringGroupVerticalSpacing();
		final int uiPadding = config.ringGroupUiPadding();
		final List<TimerCircleComponent> visibleComponents = new ArrayList<>();
		for (TimerCircleOverlay member : members)
		{
			if (member.isTimerActive())
			{
				final TimerCircleComponent component = new TimerCircleComponent(member);
				visibleComponents.add(component);
				panelComponent.getChildren().add(component);
			}
		}

		if (visibleComponents.isEmpty())
		{
			synchronized (visibleMemberBounds)
			{
				visibleMemberBounds.clear();
			}
			panelComponent.getChildren().clear();
			return null;
		}

		panelComponent.setPreferredSize(new Dimension(
			DEFAULT_WRAP_COUNT * (cellSize.width + horizontalGap) + uiPadding * 2,
			DEFAULT_WRAP_COUNT * (cellSize.height + verticalGap) + uiPadding * 2
		));
		panelComponent.setBorder(new Rectangle(uiPadding, uiPadding, uiPadding, uiPadding));
		panelComponent.setGap(new Point(horizontalGap, verticalGap));
		panelComponent.setOrientation(orientation);

		final Dimension dimension = super.render(graphics);
		try
		{
			final net.runelite.api.Point clientMouse = client.getMouseCanvasPosition();
			final Point mouse = new Point(clientMouse.getX(), clientMouse.getY());
			final Map<TimerCircleOverlay, Rectangle> renderedBounds = new LinkedHashMap<>();
			boolean foundHoveredTimer = false;
			for (TimerCircleComponent component : visibleComponents)
			{
				final Rectangle childBounds = new Rectangle(component.getBounds());
				childBounds.translate(getBounds().x, getBounds().y);
				renderedBounds.put(component.getTimer(), childBounds);
				if (!foundHoveredTimer && childBounds.contains(mouse))
				{
					foundHoveredTimer = true;
					if (!menuOpen)
					{
						hoveredTimer = component.getTimer();
					}
				}
			}

			synchronized (visibleMemberBounds)
			{
				visibleMemberBounds.clear();
				visibleMemberBounds.putAll(renderedBounds);
			}
		}
		finally
		{
			panelComponent.getChildren().clear();
		}
		return dimension;
	}

	@Override
	public List<OverlayMenuEntry> getMenuEntries()
	{
		if (hoveredTimer == null)
		{
			return Collections.emptyList();
		}

		final List<OverlayMenuEntry> entries = new ArrayList<>(2);
		if (members.size() > 1)
		{
			entries.add(new OverlayMenuEntry(
				MenuAction.RUNELITE_OVERLAY,
				DETACH_OPTION,
				hoveredTimer.getName()
			));
		}
		if (getActiveMemberCount() > 1)
		{
			entries.add(new OverlayMenuEntry(
				MenuAction.RUNELITE_OVERLAY,
				FLIP_OPTION,
				"Ring of Time group"
			));
		}
		return entries;
	}

	@Override
	public boolean onDrag(Overlay source)
	{
		if (!(source instanceof TimerGroupOverlay))
		{
			return false;
		}

		return manager.merge((TimerGroupOverlay) source, this);
	}

	void initializeAutomaticLayout()
	{
		automaticLayoutEnabled = getPreferredLocation() == null
			&& getPreferredPosition() == null
			&& getPreferredSize() == null;
		lastAutomaticLocation = null;
	}

	boolean applyAutomaticLocation(Point location)
	{
		if (!automaticLayoutEnabled)
		{
			return false;
		}

		final Point currentLocation = getPreferredLocation();
		if (getPreferredSize() != null
			|| getPreferredPosition() != null
			|| (lastAutomaticLocation == null && currentLocation != null)
			|| (lastAutomaticLocation != null && !lastAutomaticLocation.equals(currentLocation)))
		{
			automaticLayoutEnabled = false;
			return false;
		}

		final Point locationCopy = new Point(location);
		setPreferredPosition(null);
		setPreferredLocation(locationCopy);
		lastAutomaticLocation = new Point(locationCopy);
		return true;
	}

	void markManaged()
	{
		automaticLayoutEnabled = false;
	}

	void placeDetachedAt(Point location)
	{
		markManaged();
		setPreferredPosition(null);
		setPreferredLocation(new Point(location));
	}

	void addMember(TimerCircleOverlay timer)
	{
		if (!members.contains(timer))
		{
			members.add(timer);
		}
	}

	void removeMember(TimerCircleOverlay timer)
	{
		members.remove(timer);
		if (hoveredTimer == timer)
		{
			hoveredTimer = null;
		}
	}

	List<TimerCircleOverlay> getMembers()
	{
		return new ArrayList<>(members);
	}

	/**
	 * Returns the visible ring under a canvas point when this group has enough
	 * visible members to reorder.
	 */
	MemberHit findMemberAt(Point canvasPoint)
	{
		synchronized (visibleMemberBounds)
		{
			if (visibleMemberBounds.size() < 2)
			{
				return null;
			}

			for (Map.Entry<TimerCircleOverlay, Rectangle> entry : visibleMemberBounds.entrySet())
			{
				if (entry.getValue().contains(canvasPoint))
				{
					return new MemberHit(this, entry.getKey());
				}
			}
		}

		return null;
	}

	/**
	 * Inserts a member immediately before or after another group member.
	 */
	boolean moveMember(
		TimerCircleOverlay dragged,
		TimerCircleOverlay target,
		boolean insertAfter)
	{
		if (dragged == null || target == null || dragged == target)
		{
			return false;
		}

		final List<TimerCircleOverlay> originalOrder = new ArrayList<>(members);
		final int draggedIndex = members.indexOf(dragged);
		int targetIndex = members.indexOf(target);
		if (draggedIndex < 0 || targetIndex < 0)
		{
			return false;
		}

		members.remove(draggedIndex);
		targetIndex = members.indexOf(target);
		members.add(targetIndex + (insertAfter ? 1 : 0), dragged);
		return !members.equals(originalOrder);
	}

	DragLayout captureDragLayout()
	{
		synchronized (visibleMemberBounds)
		{
			return visibleMemberBounds.size() < 2
				? null
				: new DragLayout(visibleMemberBounds);
		}
	}

	/**
	 * Applies known saved names first and leaves newly introduced timers in
	 * their normal order at the end.
	 */
	void restoreMemberOrder(List<String> savedOrder)
	{
		if (savedOrder == null || savedOrder.isEmpty() || members.size() < 2)
		{
			return;
		}

		final List<TimerCircleOverlay> remaining = new ArrayList<>(members);
		final List<TimerCircleOverlay> restored = new ArrayList<>(members.size());
		for (String name : savedOrder)
		{
			for (int index = 0; index < remaining.size(); index++)
			{
				final TimerCircleOverlay member = remaining.get(index);
				if (member.getName().equals(name))
				{
					restored.add(member);
					remaining.remove(index);
					break;
				}
			}
		}

		restored.addAll(remaining);
		members.clear();
		members.addAll(restored);
	}

	TimerCircleOverlay getHoveredTimer()
	{
		return hoveredTimer;
	}

	boolean isTimerActive()
	{
		return getActiveMemberCount() > 0;
	}

	int estimateAutomaticHeight(Dimension cellSize)
	{
		final int count = getActiveMemberCount();
		if (count == 0)
		{
			return 0;
		}

		final int rows = orientation == ComponentOrientation.VERTICAL
			? Math.min(count, DEFAULT_WRAP_COUNT)
			: (count + DEFAULT_WRAP_COUNT - 1) / DEFAULT_WRAP_COUNT;
		return rows * cellSize.height
			+ Math.max(0, rows - 1) * config.ringGroupVerticalSpacing()
			+ config.ringGroupUiPadding() * 2;
	}

	ComponentOrientation flip()
	{
		orientation = orientation == ComponentOrientation.HORIZONTAL
			? ComponentOrientation.VERTICAL
			: ComponentOrientation.HORIZONTAL;
		return orientation;
	}

	ComponentOrientation getOrientation()
	{
		return orientation;
	}

	private int getActiveMemberCount()
	{
		int count = 0;
		for (TimerCircleOverlay member : members)
		{
			if (member.isTimerActive())
			{
				count++;
			}
		}
		return count;
	}

	/**
	 * Identifies both the group and ring found during mouse hit testing.
	 */
	static final class MemberHit
	{
		private final TimerGroupOverlay group;
		private final TimerCircleOverlay member;

		private MemberHit(TimerGroupOverlay group, TimerCircleOverlay member)
		{
			this.group = group;
			this.member = member;
		}

		TimerGroupOverlay getGroup()
		{
			return group;
		}

		TimerCircleOverlay getMember()
		{
			return member;
		}
	}

	static final class InsertionTarget
	{
		private final TimerCircleOverlay member;
		private final boolean insertAfter;

		private InsertionTarget(TimerCircleOverlay member, boolean insertAfter)
		{
			this.member = member;
			this.insertAfter = insertAfter;
		}

		TimerCircleOverlay getMember()
		{
			return member;
		}

		boolean isInsertAfter()
		{
			return insertAfter;
		}
	}

	static final class DragLayout
	{
		private final Map<TimerCircleOverlay, Rectangle> slots;

		private DragLayout(Map<TimerCircleOverlay, Rectangle> visibleMemberBounds)
		{
			this.slots = new LinkedHashMap<>();
			for (Map.Entry<TimerCircleOverlay, Rectangle> entry : visibleMemberBounds.entrySet())
			{
				this.slots.put(entry.getKey(), new Rectangle(entry.getValue()));
			}
		}

		InsertionTarget findInsertionAt(Point canvasPoint, TimerCircleOverlay dragged)
		{
			int nearestSlotIndex = -1;
			int slotIndex = 0;
			double nearestDistance = Double.MAX_VALUE;
			for (Map.Entry<TimerCircleOverlay, Rectangle> entry : slots.entrySet())
			{
				final Rectangle bounds = entry.getValue();
				final double xDistance = canvasPoint.x - bounds.getCenterX();
				final double yDistance = canvasPoint.y - bounds.getCenterY();
				final double distance = xDistance * xDistance + yDistance * yDistance;
				if (distance < nearestDistance)
				{
					nearestDistance = distance;
					nearestSlotIndex = slotIndex;
				}
				slotIndex++;
			}

			final List<TimerCircleOverlay> remaining = new ArrayList<>(slots.keySet());
			if (nearestSlotIndex < 0 || !remaining.remove(dragged) || remaining.isEmpty())
			{
				return null;
			}

			if (nearestSlotIndex >= remaining.size())
			{
				return new InsertionTarget(remaining.get(remaining.size() - 1), true);
			}

			return new InsertionTarget(remaining.get(nearestSlotIndex), false);
		}
	}
}
