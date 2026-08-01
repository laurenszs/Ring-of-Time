package com.ringoftime;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.Overlay;

/**
 * Base renderable entity shared by skill and effect timer rings. Group
 * overlays own movement, resizing, wrapping, and persisted placement.
 */
abstract class TimerCircleOverlay extends Overlay
{
	TimerCircleOverlay(Plugin plugin)
	{
		super(plugin);
	}

	/**
	 * Reports whether this timer should be included in its active group layout.
	 */
	abstract boolean isTimerActive();
}
