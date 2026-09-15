package com.ringoftime;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.components.ComponentOrientation;

/**
 * Owns ring-group membership, overlay registration, and saved layout metadata.
 */
final class TimerGroupManager
{
	private static final String LAYOUT_CONFIG_GROUP = "ring-of-time-layout";
	private static final String MEMBER_PREFIX = "member_";
	private static final String ORIENTATION_PREFIX = "orientation_";
	private static final String ORDER_PREFIX = "order_";
	private static final String SIZE_PREFIX = "size_";
	private static final String NEXT_GROUP_NUMBER_KEY = "nextGroupNumber";
	private static final String ORDER_SEPARATOR = "\n";
	private static final int DETACH_OFFSET = 16;
	private static final String RUNELITE_CONFIG_GROUP = "runelite";
	private static final String[] OVERLAY_CONFIG_SUFFIXES =
	{
		"_preferredLocation",
		"_preferredPosition",
		"_origin",
		"_originX",
		"_originY",
		"_preferredSize"
	};

	private final Client client;
	private final RingOfTimePlugin plugin;
	private final RingOfTimeConfig config;
	private final OverlayManager overlayManager;
	private final ConfigManager configManager;
	private final CustomUiAnchorsCompatibility customUiAnchorsCompatibility;

	private final Map<String, TimerGroupOverlay> groups = new LinkedHashMap<>();

	TimerGroupManager(
		Client client,
		RingOfTimePlugin plugin,
		RingOfTimeConfig config,
		OverlayManager overlayManager,
		ConfigManager configManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.overlayManager = overlayManager;
		this.configManager = configManager;
		this.customUiAnchorsCompatibility = new CustomUiAnchorsCompatibility(configManager);
	}

	void start(Collection<? extends TimerCircleOverlay> timers)
	{
		final Map<String, List<TimerCircleOverlay>> membersByGroup = new LinkedHashMap<>();
		for (TimerCircleOverlay timer : timers)
		{
			final String savedGroup = getSavedGroup(timer);
			final String groupName = savedGroup == null ? timer.getName() : savedGroup;
			membersByGroup.computeIfAbsent(groupName, ignored -> new ArrayList<>()).add(timer);
		}

		final Set<String> reservedNames = new HashSet<>(membersByGroup.keySet());
		for (Map.Entry<String, List<TimerCircleOverlay>> entry : membersByGroup.entrySet())
		{
			String groupName = entry.getKey();
			final List<TimerCircleOverlay> members = entry.getValue();
			if (isMemberNamedGroup(groupName, members) && members.size() > 1)
			{
				final String independentName = createIndependentGroupName(reservedNames);
				migrateSavedGroup(groupName, independentName, members);
				groupName = independentName;
				reservedNames.add(independentName);
			}

			final TimerGroupOverlay group = createGroup(groupName);
			for (TimerCircleOverlay member : members)
			{
				group.addMember(member);
			}
		}

		for (TimerGroupOverlay group : groups.values())
		{
			group.restoreMemberOrder(loadOrder(group.getName()));
		}
		customUiAnchorsCompatibility.reconcile(groups.values());
	}

	void shutDown()
	{
		for (TimerGroupOverlay group : groups.values())
		{
			rememberSize(group);
			overlayManager.saveOverlay(group);
			overlayManager.remove(group);
		}
		groups.clear();
	}

	boolean merge(TimerGroupOverlay source, TimerGroupOverlay destination)
	{
		if (source == destination
			|| !groups.containsValue(source)
			|| !groups.containsValue(destination))
		{
			return false;
		}

		if (isMemberNamedGroup(destination.getName(), destination.getMembers()))
		{
			destination = promoteToIndependentGroup(destination);
		}

		destination.markManaged();
		for (TimerCircleOverlay timer : source.getMembers())
		{
			source.removeMember(timer);
			destination.addMember(timer);
			saveGroup(timer, destination.getName());
		}

		groups.remove(source.getName());
		overlayManager.remove(source);
		saveOrder(destination);
		customUiAnchorsCompatibility.merge(source, destination);
		clearSavedGroupMetadata(source.getName());
		clearOverlayConfiguration(source.getName());
		return true;
	}

	/**
	 * Moves the selected visible ring through its group and persists the result.
	 */
	boolean reorder(
		TimerGroupOverlay group,
		TimerCircleOverlay dragged,
		TimerCircleOverlay target,
		boolean insertAfter)
	{
		if (!groups.containsValue(group)
			|| !group.moveMember(dragged, target, insertAfter))
		{
			return false;
		}

		saveOrder(group);
		return true;
	}

	/**
	 * Finds a visible ring at the supplied canvas coordinate.
	 */
	TimerGroupOverlay.MemberHit findMemberAt(Point canvasPoint)
	{
		for (TimerGroupOverlay group : groups.values())
		{
			final TimerGroupOverlay.MemberHit hit = group.findMemberAt(canvasPoint);
			if (hit != null)
			{
				return hit;
			}
		}

		return null;
	}

	void handleMenu(TimerGroupOverlay group, String option)
	{
		if (TimerGroupOverlay.DETACH_OPTION.equals(option))
		{
			detach(group, group.getHoveredTimer());
		}
		else if (TimerGroupOverlay.FLIP_OPTION.equals(option))
		{
			final ComponentOrientation orientation = group.flip();
			configManager.setConfiguration(
				LAYOUT_CONFIG_GROUP,
				ORIENTATION_PREFIX + group.getName(),
				orientation
			);
		}
	}

	List<TimerGroupOverlay> getGroups()
	{
		return new ArrayList<>(groups.values());
	}

	void handleOverlayConfigChanged(String key, String newValue)
	{
		if (newValue == null || newValue.trim().isEmpty())
		{
			return;
		}

		for (TimerGroupOverlay group : groups.values())
		{
			if ((group.getName() + "_preferredSize").equals(key))
			{
				rememberSize(group);
				return;
			}
		}
	}

	private void detach(TimerGroupOverlay source, TimerCircleOverlay timer)
	{
		if (timer == null || source.getMembers().size() <= 1)
		{
			return;
		}

		source.markManaged();
		final String groupName = groups.containsKey(timer.getName())
			? TimerCircleOverlay.persistentName("Group " + UUID.randomUUID())
			: timer.getName();
		final TimerGroupOverlay detached = createGroup(groupName);
		customUiAnchorsCompatibility.detach(groupName);

		source.removeMember(timer);
		detached.addMember(timer);
		saveGroup(timer, groupName);
		saveOrder(source);
		saveOrder(detached);
		configManager.setConfiguration(
			LAYOUT_CONFIG_GROUP,
			ORIENTATION_PREFIX + groupName,
			source.getOrientation()
		);
		if (detached.getOrientation() != source.getOrientation())
		{
			detached.flip();
		}

		final Point sourceLocation = source.getPreferredLocation() != null
			? new Point(source.getPreferredLocation())
			: source.getBounds().getLocation();
		sourceLocation.translate(DETACH_OFFSET, DETACH_OFFSET);
		detached.placeDetachedAt(sourceLocation);
		overlayManager.saveOverlay(detached);
	}

	private TimerGroupOverlay createGroup(String groupName)
	{
		ComponentOrientation orientation = configManager.getConfiguration(
			LAYOUT_CONFIG_GROUP,
			ORIENTATION_PREFIX + groupName,
			ComponentOrientation.class
		);
		if (orientation == null)
		{
			orientation = ComponentOrientation.VERTICAL;
		}

		final TimerGroupOverlay group = new TimerGroupOverlay(
			client,
			plugin,
			config,
			this,
			groupName,
			orientation
		);
		groups.put(groupName, group);
		overlayManager.add(group);

		/*
		 * External overlay managers can temporarily clear RuneLite's native size
		 * key while repositioning an overlay. Keep a plugin-owned copy so a group
		 * retains its rows and columns even if the client closes at that moment.
		 */
		final Dimension savedSize = configManager.getConfiguration(
			LAYOUT_CONFIG_GROUP,
			SIZE_PREFIX + groupName,
			Dimension.class
		);
		if (savedSize == null)
		{
			rememberSize(group);
		}
		else
		{
			group.setPreferredSize(new Dimension(savedSize));
		}

		group.initializeAutomaticLayout();
		return group;
	}

	private TimerGroupOverlay promoteToIndependentGroup(TimerGroupOverlay source)
	{
		final String groupName = createIndependentGroupName(groups.keySet());
		overlayManager.saveOverlay(source);
		moveConfiguration(SIZE_PREFIX + source.getName(), SIZE_PREFIX + groupName);
		moveOverlayConfiguration(source.getName(), groupName);
		final TimerGroupOverlay promoted = createGroup(groupName);
		promoted.markManaged();

		if (promoted.getOrientation() != source.getOrientation())
		{
			promoted.flip();
		}
		configManager.setConfiguration(
			LAYOUT_CONFIG_GROUP,
			ORIENTATION_PREFIX + groupName,
			source.getOrientation()
		);

		for (TimerCircleOverlay timer : source.getMembers())
		{
			source.removeMember(timer);
			promoted.addMember(timer);
			saveGroup(timer, groupName);
		}
		saveOrder(promoted);
		customUiAnchorsCompatibility.merge(source, promoted);

		groups.remove(source.getName());
		overlayManager.remove(source);
		clearSavedGroupMetadata(source.getName());
		clearOverlayConfiguration(source.getName());
		return promoted;
	}

	private void migrateSavedGroup(
		String oldName,
		String newName,
		Collection<TimerCircleOverlay> members)
	{
		for (TimerCircleOverlay member : members)
		{
			saveGroup(member, newName);
		}

		moveConfiguration(ORIENTATION_PREFIX + oldName, ORIENTATION_PREFIX + newName);
		moveConfiguration(ORDER_PREFIX + oldName, ORDER_PREFIX + newName);
		moveConfiguration(SIZE_PREFIX + oldName, SIZE_PREFIX + newName);
		moveOverlayConfiguration(oldName, newName);
	}

	private String createIndependentGroupName(Collection<String> reservedNames)
	{
		Integer nextNumber = configManager.getConfiguration(
			LAYOUT_CONFIG_GROUP,
			NEXT_GROUP_NUMBER_KEY,
			Integer.class
		);
		int number = nextNumber == null ? 1 : Math.max(1, nextNumber);

		String name;
		do
		{
			name = TimerCircleOverlay.persistentName("Group " + number++);
		}
		while (reservedNames.contains(name) || hasSavedGroupData(name));

		configManager.setConfiguration(LAYOUT_CONFIG_GROUP, NEXT_GROUP_NUMBER_KEY, number);
		return name;
	}

	private boolean hasSavedGroupData(String groupName)
	{
		if (configManager.getConfiguration(
			LAYOUT_CONFIG_GROUP,
			ORIENTATION_PREFIX + groupName) != null
			|| configManager.getConfiguration(
				LAYOUT_CONFIG_GROUP,
				ORDER_PREFIX + groupName) != null
			|| configManager.getConfiguration(
				LAYOUT_CONFIG_GROUP,
				SIZE_PREFIX + groupName) != null)
		{
			return true;
		}

		for (String suffix : OVERLAY_CONFIG_SUFFIXES)
		{
			if (configManager.getConfiguration(RUNELITE_CONFIG_GROUP, groupName + suffix) != null)
			{
				return true;
			}
		}
		return false;
	}

	private void moveConfiguration(String oldKey, String newKey)
	{
		final String value = configManager.getConfiguration(LAYOUT_CONFIG_GROUP, oldKey);
		if (value != null)
		{
			configManager.setConfiguration(LAYOUT_CONFIG_GROUP, newKey, value);
			configManager.unsetConfiguration(LAYOUT_CONFIG_GROUP, oldKey);
		}
	}

	private void moveOverlayConfiguration(String oldName, String newName)
	{
		for (String suffix : OVERLAY_CONFIG_SUFFIXES)
		{
			final String oldKey = oldName + suffix;
			final String value = configManager.getConfiguration(RUNELITE_CONFIG_GROUP, oldKey);
			if (value != null)
			{
				configManager.setConfiguration(RUNELITE_CONFIG_GROUP, newName + suffix, value);
				configManager.unsetConfiguration(RUNELITE_CONFIG_GROUP, oldKey);
			}
		}
	}

	private void clearOverlayConfiguration(String groupName)
	{
		for (String suffix : OVERLAY_CONFIG_SUFFIXES)
		{
			configManager.unsetConfiguration(RUNELITE_CONFIG_GROUP, groupName + suffix);
		}
	}

	private void clearSavedGroupMetadata(String groupName)
	{
		configManager.unsetConfiguration(LAYOUT_CONFIG_GROUP, ORDER_PREFIX + groupName);
		configManager.unsetConfiguration(LAYOUT_CONFIG_GROUP, ORIENTATION_PREFIX + groupName);
		configManager.unsetConfiguration(LAYOUT_CONFIG_GROUP, SIZE_PREFIX + groupName);
	}

	private void rememberSize(TimerGroupOverlay group)
	{
		final Dimension size = group.getPreferredSize();
		if (size == null)
		{
			return;
		}

		final String key = SIZE_PREFIX + group.getName();
		final Dimension savedSize = configManager.getConfiguration(
			LAYOUT_CONFIG_GROUP,
			key,
			Dimension.class
		);
		if (!size.equals(savedSize))
		{
			configManager.setConfiguration(LAYOUT_CONFIG_GROUP, key, new Dimension(size));
		}
	}

	static boolean isMemberNamedGroup(
		String groupName,
		Collection<? extends TimerCircleOverlay> members)
	{
		for (TimerCircleOverlay member : members)
		{
			if (groupName.equals(member.getName()))
			{
				return true;
			}
		}
		return false;
	}

	private String getSavedGroup(TimerCircleOverlay timer)
	{
		final String value = configManager.getConfiguration(
			LAYOUT_CONFIG_GROUP,
			MEMBER_PREFIX + timer.getName()
		);
		return value == null || value.trim().isEmpty() ? null : value;
	}

	private void saveGroup(TimerCircleOverlay timer, String groupName)
	{
		final String key = MEMBER_PREFIX + timer.getName();
		if (timer.getName().equals(groupName))
		{
			configManager.unsetConfiguration(LAYOUT_CONFIG_GROUP, key);
		}
		else
		{
			configManager.setConfiguration(LAYOUT_CONFIG_GROUP, key, groupName);
		}
	}

	private List<String> loadOrder(String groupName)
	{
		final String value = configManager.getConfiguration(
			LAYOUT_CONFIG_GROUP,
			ORDER_PREFIX + groupName
		);
		if (value == null || value.isEmpty())
		{
			return Collections.emptyList();
		}

		final List<String> order = new ArrayList<>();
		Collections.addAll(order, value.split(ORDER_SEPARATOR));
		return order;
	}

	private void saveOrder(TimerGroupOverlay group)
	{
		final List<TimerCircleOverlay> members = group.getMembers();
		final List<String> names = new ArrayList<>(members.size());
		for (TimerCircleOverlay member : members)
		{
			names.add(member.getName());
		}

		configManager.setConfiguration(
			LAYOUT_CONFIG_GROUP,
			ORDER_PREFIX + group.getName(),
			String.join(ORDER_SEPARATOR, names)
		);
	}
}
