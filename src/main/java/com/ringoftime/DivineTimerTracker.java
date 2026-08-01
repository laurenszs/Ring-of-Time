package com.ringoftime;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Constants;
import net.runelite.api.Skill;

/**
 * Tracks the exact per-skill durations supplied by divine-potion varbits.
 */
final class DivineTimerTracker
{
	static final int FULL_DURATION_TICKS = 500;

	private final Map<Skill, TimerSample> samples = new EnumMap<>(Skill.class);

	void reset()
	{
		samples.clear();
	}

	/**
	 * Updates all five skills affected by divine potions from one client sample.
	 * Combination-potion variables identify Defence as divine even when a
	 * Moonlight potion happens to expose the same underlying Defence timer.
	 */
	void observe(
		int attackTicks,
		int strengthTicks,
		int defenceTicks,
		int rangedTicks,
		int magicTicks,
		int combatTicks,
		int bastionTicks,
		int battlemageTicks,
		int moonlightTicks,
		long sampleMillis)
	{
		setRemainingTicks(Skill.ATTACK, attackTicks, sampleMillis);
		setRemainingTicks(Skill.STRENGTH, strengthTicks, sampleMillis);
		setRemainingTicks(Skill.RANGED, rangedTicks, sampleMillis);
		setRemainingTicks(Skill.MAGIC, magicTicks, sampleMillis);

		final boolean combinationProtectsDefence = combatTicks >= defenceTicks
			|| bastionTicks >= defenceTicks
			|| battlemageTicks >= defenceTicks;
		final boolean moonlightOnly = defenceTicks > 0
			&& moonlightTicks > 0
			&& !combinationProtectsDefence
			&& moonlightTicks + 1 >= defenceTicks;
		setRemainingTicks(Skill.DEFENCE, moonlightOnly ? 0 : defenceTicks, sampleMillis);
	}

	boolean isActive(Skill skill)
	{
		return samples.containsKey(skill);
	}

	double getProgress(Skill skill, long nowMillis)
	{
		final double ticks = getRemainingTicks(skill, nowMillis);
		return clamp(ticks / FULL_DURATION_TICKS);
	}

	int getRemainingSeconds(Skill skill, long nowMillis)
	{
		final double ticks = getRemainingTicks(skill, nowMillis);
		return (int) Math.ceil(ticks * Constants.GAME_TICK_LENGTH / 1000d);
	}

	private double getRemainingTicks(Skill skill, long nowMillis)
	{
		final TimerSample sample = samples.get(skill);
		if (sample == null)
		{
			return 0d;
		}

		final long elapsedMillis = Math.max(0L, nowMillis - sample.sampleMillis);
		return Math.max(
			0d,
			sample.remainingTicks - elapsedMillis / (double) Constants.GAME_TICK_LENGTH
		);
	}

	private void setRemainingTicks(Skill skill, int ticks, long sampleMillis)
	{
		if (ticks <= 0)
		{
			samples.remove(skill);
		}
		else
		{
			final int clampedTicks = Math.min(FULL_DURATION_TICKS, ticks);
			final TimerSample previous = samples.get(skill);
			if (previous == null || previous.remainingTicks != clampedTicks)
			{
				samples.put(skill, new TimerSample(clampedTicks, sampleMillis));
			}
		}
	}

	private static double clamp(double value)
	{
		return Math.max(0d, Math.min(1d, value));
	}

	private static final class TimerSample
	{
		private final int remainingTicks;
		private final long sampleMillis;

		private TimerSample(int remainingTicks, long sampleMillis)
		{
			this.remainingTicks = remainingTicks;
			this.sampleMillis = sampleMillis;
		}
	}
}