/*
 * Copyright (c) 2026, Ring of Time contributors
 * All rights reserved.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.ringoftime;

import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies timing behavior without starting or automating the game client.
 */
public class StatChangeTrackerTest
{
	private static final double TOLERANCE = 0.0001d;

	private StatChangeTracker tracker;

	@Before
	public void setUp()
	{
		tracker = new StatChangeTracker();
	}

	@Test
	public void buffRingShrinksAcrossTheWholeEffect()
	{
		// A five-level boost begins with a full, estimated five-minute ring.
		tracker.observe(Skill.STRENGTH, 104, 99, 0);
		tracker.onGameTick(0, false);
		assertEquals(1d, tracker.getOverallProgress(Skill.STRENGTH, 0, 0d), TOLERANCE);
		assertEquals(300, tracker.getRemainingSeconds(Skill.STRENGTH, 0, 0d, false));
		assertTrue(tracker.isEstimated(Skill.STRENGTH));

		// Halfway through the first minute, 4.5 of five cycles remain.
		tracker.onGameTick(50, false);
		assertEquals(0.9d, tracker.getOverallProgress(Skill.STRENGTH, 50, 0d), TOLERANCE);
		assertEquals(0.5d, tracker.getNextChangeProgress(Skill.STRENGTH, 50, 0d), TOLERANCE);
		assertEquals(270, tracker.getRemainingSeconds(Skill.STRENGTH, 50, 0d, false));
	}

	@Test
	public void naturalBuffDecaySynchronizesWithoutJumping()
	{
		tracker.observe(Skill.STRENGTH, 104, 99, 0);

		// The observed 104 -> 103 restoration anchors a new exact cycle.
		tracker.observe(Skill.STRENGTH, 103, 99, 100);
		tracker.onGameTick(100, false);

		assertFalse(tracker.isEstimated(Skill.STRENGTH));
		assertEquals(0.8d, tracker.getOverallProgress(Skill.STRENGTH, 100, 0d), TOLERANCE);
		assertEquals(240, tracker.getRemainingSeconds(Skill.STRENGTH, 100, 0d, false));
	}

	@Test
	public void nextChangeRingResetsWhenABuffedLevelDecays()
	{
		tracker.observe(Skill.STRENGTH, 102, 99, 0);
		tracker.onGameTick(75, false);
		assertEquals(0.25d, tracker.getNextChangeProgress(Skill.STRENGTH, 75, 0d), TOLERANCE);

		// The natural +3 -> +2 change starts the next inner-ring cycle at full.
		tracker.observe(Skill.STRENGTH, 101, 99, 100);
		assertEquals(1d, tracker.getNextChangeProgress(Skill.STRENGTH, 100, 0d), TOLERANCE);
		assertEquals(2d / 3d, tracker.getOverallProgress(Skill.STRENGTH, 100, 0d), TOLERANCE);
	}

	@Test
	public void debuffUsesItsOwnRecoveryClock()
	{
		tracker.observe(Skill.MAGIC, 96, 99, 10);
		tracker.onGameTick(35, false);

		// Two complete levels plus three quarters of the current cycle remain.
		assertEquals(2.75d / 3d, tracker.getOverallProgress(Skill.MAGIC, 35, 0d), TOLERANCE);
		assertEquals(0.75d, tracker.getNextChangeProgress(Skill.MAGIC, 35, 0d), TOLERANCE);
		assertEquals(-3, tracker.getDelta(Skill.MAGIC));
		assertTrue(tracker.getActiveSkills().contains(Skill.MAGIC));

		// A natural -3 -> -2 recovery synchronizes the debuff clock.
		tracker.observe(Skill.MAGIC, 97, 99, 110);
		assertFalse(tracker.isEstimated(Skill.MAGIC));
		assertEquals(1d, tracker.getNextChangeProgress(Skill.MAGIC, 110, 0d), TOLERANCE);
		assertEquals(2d / 3d, tracker.getOverallProgress(Skill.MAGIC, 110, 0d), TOLERANCE);
	}

	@Test
	public void separateInnerRingDropdownValuesMapToVisibility()
	{
		// Buff and debuff sections each use this same explicit on/off value type.
		assertTrue(RingOfTimeConfig.InnerRingDisplay.SHOW.isShown());
		assertFalse(RingOfTimeConfig.InnerRingDisplay.HIDE.isShown());
	}

	@Test
	public void visualDisappearsAtTheRealLevel()
	{
		tracker.observe(Skill.MAGIC, 98, 99, 0);
		assertTrue(tracker.getActiveSkills().contains(Skill.MAGIC));

		tracker.observe(Skill.MAGIC, 99, 99, 100);
		assertFalse(tracker.getActiveSkills().contains(Skill.MAGIC));
		assertEquals(0d, tracker.getOverallProgress(Skill.MAGIC, 100, 0d), TOLERANCE);
	}

	@Test
	public void preserveExtendsTheCurrentAndProjectedBuffCycles()
	{
		tracker.observe(Skill.STRENGTH, 101, 99, 0);

		// Preserve active in its extension window changes a 100-tick cycle to 150.
		tracker.onGameTick(50, true);
		assertEquals(5d / 6d, tracker.getOverallProgress(Skill.STRENGTH, 50, 0d), TOLERANCE);
		assertEquals(150, tracker.getRemainingSeconds(Skill.STRENGTH, 50, 0d, true));
	}
}
