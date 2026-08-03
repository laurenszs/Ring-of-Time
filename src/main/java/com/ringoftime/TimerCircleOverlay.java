package com.ringoftime;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.Overlay;

/**
 * Base renderable entity shared by skill and effect timer rings. Group
 * overlays own movement, resizing, wrapping, and persisted placement.
 */
abstract class TimerCircleOverlay extends Overlay
{
	private static final String PERSISTENT_NAME_PREFIX = "Ring of Time - ";

	TimerCircleOverlay(Plugin plugin)
	{
		super(plugin);
	}

	static String persistentName(String label)
	{
		return PERSISTENT_NAME_PREFIX + label.replace(':', '-');
	}

	/**
	 * Reports whether this timer should be included in its active group layout.
	 */
	abstract boolean isTimerActive();
}
