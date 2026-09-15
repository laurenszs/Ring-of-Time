package com.ringoftime;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.input.MouseListener;

/**
 * Handles Ctrl+Alt-drag insertion inside a ring group.
 */
final class TimerGroupReorderInput implements MouseListener
{
	private static final int REORDER_MODIFIERS =
		InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK;

	private final Client client;
	private final TimerGroupManager manager;
	private TimerGroupOverlay activeGroup;
	private TimerCircleOverlay draggedMember;
	private TimerGroupOverlay.DragLayout dragLayout;
	private TimerCircleOverlay lastTarget;
	private boolean lastInsertAfter;
	private double eventScaleX = 1.0;
	private double eventScaleY = 1.0;

	TimerGroupReorderInput(Client client, TimerGroupManager manager)
	{
		this.client = client;
		this.manager = manager;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		if (!hasReorderModifiers(event) || !SwingUtilities.isLeftMouseButton(event))
		{
			return event;
		}

		final Point canvasPoint = getCanvasMousePoint(event);
		calibrateEventScale(event.getPoint(), canvasPoint);
		final TimerGroupOverlay.MemberHit hit = manager.findMemberAt(canvasPoint);
		if (hit == null)
		{
			return event;
		}

		activeGroup = hit.getGroup();
		draggedMember = hit.getMember();
		dragLayout = activeGroup.captureDragLayout();
		event.consume();
		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		if (draggedMember == null)
		{
			return event;
		}

		final Point canvasPoint = scaleEventPoint(event.getPoint());
		final TimerGroupOverlay.InsertionTarget target = dragLayout == null
			? null
			: dragLayout.findInsertionAt(canvasPoint, draggedMember);
		if (target != null && (target.getMember() != lastTarget
			|| target.isInsertAfter() != lastInsertAfter)
		)
		{
			manager.reorder(
				activeGroup,
				draggedMember,
				target.getMember(),
				target.isInsertAfter()
			);
			lastTarget = target.getMember();
			lastInsertAfter = target.isInsertAfter();
		}

		event.consume();
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		if (draggedMember != null)
		{
			clearDrag();
			event.consume();
		}
		return event;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent event)
	{
		clearDrag();
		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		return event;
	}

	static boolean hasReorderModifiers(MouseEvent event)
	{
		return (event.getModifiersEx() & REORDER_MODIFIERS) == REORDER_MODIFIERS;
	}

	private Point getCanvasMousePoint(MouseEvent event)
	{
		if (client == null)
		{
			return event.getPoint();
		}

		final net.runelite.api.Point canvasPoint = client.getMouseCanvasPosition();
		return new Point(canvasPoint.getX(), canvasPoint.getY());
	}

	private void calibrateEventScale(Point eventPoint, Point canvasPoint)
	{
		eventScaleX = calculateScale(eventPoint.x, canvasPoint.x);
		eventScaleY = calculateScale(eventPoint.y, canvasPoint.y);
	}

	private static double calculateScale(int eventCoordinate, int canvasCoordinate)
	{
		return eventCoordinate > 0 && canvasCoordinate > 0
			? (double) eventCoordinate / canvasCoordinate
			: 1.0;
	}

	private Point scaleEventPoint(Point eventPoint)
	{
		return new Point(
			(int) Math.round(eventPoint.x / eventScaleX),
			(int) Math.round(eventPoint.y / eventScaleY)
		);
	}

	private void clearDrag()
	{
		activeGroup = null;
		draggedMember = null;
		dragLayout = null;
		lastTarget = null;
		lastInsertAfter = false;
		eventScaleX = 1.0;
		eventScaleY = 1.0;
	}
}
