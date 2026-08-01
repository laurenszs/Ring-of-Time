package com.ringoftime;

import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DivineTimerTrackerTest
{
	private static final double TOLERANCE = 0.0001d;

	private DivineTimerTracker tracker;

	@Before
	public void setUp()
	{
		tracker = new DivineTimerTracker();
	}

	@Test
	public void tracksExactPerSkillDuration()
	{
		tracker.observe(500, 400, 300, 200, 100, 0, 0, 0, 0, 1_000L);

		assertTrue(tracker.isActive(Skill.ATTACK));
		assertEquals(1d, tracker.getProgress(Skill.ATTACK, 1_000L), TOLERANCE);
		assertEquals(300, tracker.getRemainingSeconds(Skill.ATTACK, 1_000L));
		assertEquals(0.6d, tracker.getProgress(Skill.DEFENCE, 1_000L), TOLERANCE);
		assertEquals(60, tracker.getRemainingSeconds(Skill.MAGIC, 1_000L));
	}

	@Test
	public void moonlightOnlyDefenceTimerIsIgnored()
	{
		tracker.observe(0, 0, 301, 0, 0, 0, 0, 0, 300, 1_000L);

		assertFalse(tracker.isActive(Skill.DEFENCE));
	}

	@Test
	public void individualDefenceRemainsActiveOnItsFinalTick()
	{
		tracker.observe(0, 0, 1, 0, 0, 0, 0, 0, 0, 1_000L);

		assertTrue(tracker.isActive(Skill.DEFENCE));
		assertEquals(1, tracker.getRemainingSeconds(Skill.DEFENCE, 1_000L));
	}

	@Test
	public void combinationPotionKeepsDefenceDivine()
	{
		tracker.observe(0, 0, 300, 300, 0, 0, 300, 0, 300, 1_000L);

		assertTrue(tracker.isActive(Skill.DEFENCE));
		assertEquals(180, tracker.getRemainingSeconds(Skill.DEFENCE, 1_000L));
	}

	@Test
	public void changedVarbitReanchorsWithoutJumpingBackward()
	{
		tracker.observe(500, 0, 0, 0, 0, 0, 0, 0, 0, 1_000L);
		final double immediatelyBeforeChange = tracker.getProgress(Skill.ATTACK, 1_599L);

		tracker.observe(499, 0, 0, 0, 0, 0, 0, 0, 0, 1_600L);
		final double immediatelyAfterChange = tracker.getProgress(Skill.ATTACK, 1_600L);

		assertTrue(immediatelyAfterChange <= immediatelyBeforeChange);
		assertEquals(immediatelyBeforeChange, immediatelyAfterChange, 0.00001d);
	}

	@Test
	public void unchangedSamplesDoNotRestartInterpolation()
	{
		tracker.observe(500, 0, 0, 0, 0, 0, 0, 0, 0, 1_000L);
		tracker.observe(500, 0, 0, 0, 0, 0, 0, 0, 0, 1_300L);

		assertEquals(499.5d / 500d, tracker.getProgress(Skill.ATTACK, 1_300L), TOLERANCE);
	}

	@Test
	public void resetClearsEverySkill()
	{
		tracker.observe(500, 500, 500, 500, 500, 500, 0, 0, 0, 1_000L);
		tracker.reset();

		for (Skill skill : new Skill[]{
			Skill.ATTACK,
			Skill.STRENGTH,
			Skill.DEFENCE,
			Skill.RANGED,
			Skill.MAGIC})
		{
			assertFalse(tracker.isActive(skill));
		}
	}
}