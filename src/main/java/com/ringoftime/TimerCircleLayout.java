/*
 * Copyright (c) 2026, Ring of Time contributors
 * All rights reserved.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.ringoftime;

import java.awt.Point;

/**
 * Pure vertical layout calculation for active circles that have not been
 * manually dragged.
 */
final class TimerCircleLayout
{
	private static final int ORIGIN_X = 10;
	private static final int ORIGIN_Y = 10;
	private static final int GAP = 4;

	private TimerCircleLayout()
	{
		// Utility class.
	}

	/**
	 * Returns the top-left point for one active circle in the vertical stack.
	 */
	static Point getLocation(int activeIndex, int circleHeight)
	{
		final int safeIndex = Math.max(0, activeIndex);
		return new Point(ORIGIN_X, ORIGIN_Y + safeIndex * (circleHeight + GAP));
	}
}
