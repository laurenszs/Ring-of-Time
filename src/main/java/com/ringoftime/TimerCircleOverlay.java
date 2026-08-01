package com.ringoftime;

import java.awt.Point;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Shares independent drag behavior between skill and effect circles.
 *
 * <p>New circles receive compact automatic positions. As soon as RuneLite
 * changes a circle's position through an Alt-drag or snap, this class stops
 * moving that entity so its user-selected location remains independent.</p>
 */
abstract class TimerCircleOverlay extends Overlay
{
	private Point lastAutomaticLocation;
	private boolean automaticLayoutEnabled;

	TimerCircleOverlay(Plugin plugin)
	{
		super(plugin);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_MED);
		setMovable(true);
		setSnappable(true);
		setResettable(true);
	}

	/**
	 * Runs after OverlayManager has loaded any saved RuneLite position. A saved
	 * location is always treated as the user's choice and is never overwritten.
	 */
	final void initializeAutomaticLayout()
	{
		automaticLayoutEnabled = getPreferredLocation() == null
			&& getPreferredPosition() == null;
		lastAutomaticLocation = null;
	}

	/**
	 * Moves an untouched active circle into the current compact stack.
	 * Comparing with a copy of the previous point also detects RuneLite mutating
	 * the live Point while the user drags it.
	 *
	 * @return true when the circle remains in the automatic compact stack
	 */
	final boolean applyAutomaticLocation(Point location)
	{
		if (!automaticLayoutEnabled)
		{
			return false;
		}

		final Point currentLocation = getPreferredLocation();
		if (lastAutomaticLocation != null
			&& (!lastAutomaticLocation.equals(currentLocation) || getPreferredPosition() != null))
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

	/**
	 * Reports whether this entity currently participates in compact stacking.
	 */
	abstract boolean isTimerActive();
}
