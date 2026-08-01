/*
 * Copyright (c) 2026, Ring of Time contributors
 * All rights reserved.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.ringoftime;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Constants;

/**
 * Tracks non-skill effect durations exposed by RuneLite game variables.
 *
 * <p>Stamina and the two antifire strengths use coarse game varbits: their
 * values decrease in blocks of several game ticks. {@link Countdown} anchors
 * each block to the tick on which its value changes so the overlay can animate
 * smoothly between those changes.</p>
 *
 * <p>Poison and venom share one varplayer. Ordinary poison decreases toward
 * zero and therefore has a total natural-cure projection. Venom instead grows
 * in severity and never naturally becomes poison, so its visual represents the
 * repeating time until the next venom hit.</p>
 */
final class EffectTimerTracker
{
	/**
	 * Identifies each independently draggable effect-circle overlay.
	 */
	enum Effect
	{
		TOXIN,
		ANTIPOISON,
		STAMINA,
		ANTIFIRE,
		SUPER_ANTIFIRE
	}

	static final int POISON_CYCLE_MILLIS = 18_200;
	static final int ANTIPOISON_CYCLE_TICKS = 30;
	static final int ANTIVENOM_CUTOFF = -38;
	static final int VENOM_THRESHOLD = 1_000_000;
	static final int VENOM_MAX_DAMAGE = 20;
	static final int STAMINA_UNIT_TICKS = 10;
	static final int ANTIFIRE_UNIT_TICKS = 30;
	static final int SUPER_ANTIFIRE_UNIT_TICKS = 20;

	private final Map<Effect, Countdown> countdowns = new EnumMap<>(Effect.class);
	private int poisonValue;
	private int poisonPeakValue;
	private int poisonCycleStartTick = -1;
	private boolean poisonEstimated = true;
	private boolean antivenomProtection;

	EffectTimerTracker()
	{
		countdowns.put(Effect.ANTIPOISON, new Countdown(1));
		countdowns.put(Effect.STAMINA, new Countdown(STAMINA_UNIT_TICKS));
		countdowns.put(Effect.ANTIFIRE, new Countdown(ANTIFIRE_UNIT_TICKS));
		countdowns.put(Effect.SUPER_ANTIFIRE, new Countdown(SUPER_ANTIFIRE_UNIT_TICKS));
	}

	/**
	 * Removes all effect state, such as when the player logs out or hops.
	 */
	void reset()
	{
		poisonValue = 0;
		poisonPeakValue = 0;
		poisonCycleStartTick = -1;
		poisonEstimated = true;
		antivenomProtection = false;

		for (Countdown countdown : countdowns.values())
		{
			countdown.reset();
		}
	}

	void observePoison(int value, int currentTick, boolean exactChange)
	{
		if (value <= 0)
		{
			poisonValue = 0;
			poisonPeakValue = 0;
			poisonCycleStartTick = -1;
			poisonEstimated = true;
			return;
		}

		final boolean wasActive = poisonValue > 0;
		final boolean wasVenom = isVenomValue(poisonValue);
		final boolean isVenom = isVenomValue(value);
		final boolean changed = value != poisonValue;
		final boolean naturalPoisonTick = wasActive
			&& !wasVenom
			&& !isVenom
			&& value == poisonValue - 1;

		if (!wasActive || changed)
		{
			poisonCycleStartTick = currentTick;
			poisonEstimated = !exactChange;
		}

		if (!isVenom)
		{
			/*
			 * Keep the initial poison severity as the denominator across
			 * ordinary one-step decay. A cure, re-poison, or venom-to-poison
			 * conversion starts a new full total-cure visual.
			 */
			if (!wasActive || wasVenom || (changed && !naturalPoisonTick))
			{
				poisonPeakValue = value;
			}
			else
			{
				poisonPeakValue = Math.max(poisonPeakValue, value);
			}
		}
		else
		{
			// Venom has no finite peak or natural-cure duration.
			poisonPeakValue = 0;
		}

		poisonValue = value;
	}

	void observeAntipoison(int value, int currentTick, boolean exactChange)
	{
		final Countdown protection = countdowns.get(Effect.ANTIPOISON);
		if (value >= 0)
		{
			protection.reset();
			antivenomProtection = false;
			return;
		}

		final boolean newAntivenomProtection = value < ANTIVENOM_CUTOFF;
		if (protection.isActive() && newAntivenomProtection != antivenomProtection)
		{
			// A protection-type transition starts a fresh independent total ring.
			protection.reset();
		}

		final int remainingTicks = newAntivenomProtection
			? ANTIPOISON_CYCLE_TICKS
				+ Math.abs((value + 1 - ANTIVENOM_CUTOFF) * ANTIPOISON_CYCLE_TICKS)
			: ANTIPOISON_CYCLE_TICKS
				+ Math.abs((value + 1) * ANTIPOISON_CYCLE_TICKS);
		protection.observe(remainingTicks, true, currentTick, exactChange);
		antivenomProtection = newAntivenomProtection;
	}

	/**
	 * Reports which icon and color the shared protection circle should use.
	 */
	boolean isAntivenomProtection()
	{
		return isActive(Effect.ANTIPOISON) && antivenomProtection;
	}

	/**
	 * Observes the stamina varbits.
	 */
	void observeStamina(int durationUnits, boolean effectActive, int currentTick, boolean exactChange)
	{
		countdowns.get(Effect.STAMINA).observe(
			durationUnits,
			effectActive,
			currentTick,
			exactChange
		);
	}

	/**
	 * Observes regular and super-antifire varbits independently.
	 */
	void observeAntifire(
		int antifireUnits,
		int superAntifireUnits,
		int currentTick,
		boolean exactChange)
	{
		countdowns.get(Effect.ANTIFIRE).observe(
			antifireUnits,
			antifireUnits > 0,
			currentTick,
			exactChange
		);
		countdowns.get(Effect.SUPER_ANTIFIRE).observe(
			superAntifireUnits,
			superAntifireUnits > 0,
			currentTick,
			exactChange
		);
	}

	/**
	 * Reports whether an effect circle currently has meaningful state.
	 */
	boolean isActive(Effect effect)
	{
		return effect == Effect.TOXIN
			? poisonValue > 0
			: countdowns.get(effect).isActive();
	}

	/**
	 * Reports whether the shared toxin circle is currently venomous.
	 */
	boolean isVenom()
	{
		return isVenomValue(poisonValue);
	}

	/**
	 * Calculates the fraction of the effect represented by the outer ring.
	 */
	double getOverallProgress(Effect effect, int currentTick, double subTickProgress)
	{
		if (!isActive(effect))
		{
			return 0d;
		}

		if (effect != Effect.TOXIN)
		{
			return countdowns.get(effect).getProgress(currentTick, subTickProgress);
		}

		final double cycleProgress = getPoisonCycleProgress(currentTick, subTickProgress);
		if (isVenom())
		{
			// Venom's outer ring is its repeating next-hit cycle.
			return cycleProgress;
		}

		final int peak = Math.max(poisonValue, poisonPeakValue);
		return clamp((poisonValue - 1d + cycleProgress) / peak);
	}

	/**
	 * Avoids drawing a duplicate inner ring during poison's final cycle.
	 */
	boolean hasMultiplePoisonCycles()
	{
		return poisonValue > 1 && !isVenom();
	}
	/**
	 * Returns poison's inner-ring progress until its next damage tick.
	 */
	double getNextPoisonHitProgress(int currentTick, double subTickProgress)
	{
		return poisonValue > 0 && !isVenom()
			? getPoisonCycleProgress(currentTick, subTickProgress)
			: 0d;
	}

	/**
	 * Projects the seconds represented by an effect's timer label.
	 *
	 * <p>For poison this is time to natural cure. For venom it is time to the
	 * next hit because venom has no natural expiry.</p>
	 */
	int getRemainingSeconds(Effect effect, int currentTick, double subTickProgress)
	{
		if (!isActive(effect))
		{
			return 0;
		}

		if (effect != Effect.TOXIN)
		{
			final double ticks = countdowns.get(effect).getRemainingTicks(currentTick, subTickProgress);
			return ticksToSeconds(ticks);
		}

		final double cycleProgress = getPoisonCycleProgress(currentTick, subTickProgress);
		final double milliseconds = isVenom()
			? cycleProgress * POISON_CYCLE_MILLIS
			: (poisonValue - 1d + cycleProgress) * POISON_CYCLE_MILLIS;
		return (int) Math.ceil(milliseconds / 1000d);
	}

	/**
	 * Returns the next poison or venom damage shown in the circle's badge.
	 */
	int getNextToxinDamage()
	{
		if (poisonValue <= 0)
		{
			return 0;
		}

		if (isVenom())
		{
			final int scaledValue = poisonValue - VENOM_THRESHOLD + 3;
			return Math.min(VENOM_MAX_DAMAGE, scaledValue * 2);
		}

		return (int) Math.ceil(poisonValue / 5d);
	}

	/**
	 * Reports whether a timer's first partial interval is only estimated.
	 */
	boolean isEstimated(Effect effect)
	{
		return effect == Effect.TOXIN
			? poisonValue > 0 && poisonEstimated
			: countdowns.get(effect).isEstimated();
	}

	/**
	 * Calculates remaining progress in the current poison/venom damage cycle.
	 */
	private double getPoisonCycleProgress(int currentTick, double subTickProgress)
	{
		if (poisonCycleStartTick < 0)
		{
			return 0d;
		}

		final double elapsedTicks = Math.max(
			0d,
			currentTick - poisonCycleStartTick + clamp(subTickProgress)
		);
		final double elapsedMillis = elapsedTicks * Constants.GAME_TICK_LENGTH;
		return clamp(1d - elapsedMillis / POISON_CYCLE_MILLIS);
	}

	private static boolean isVenomValue(int value)
	{
		return value >= VENOM_THRESHOLD;
	}

	private static int ticksToSeconds(double ticks)
	{
		return (int) Math.ceil(ticks * Constants.GAME_TICK_LENGTH / 1000d);
	}

	private static double clamp(double value)
	{
		return Math.max(0d, Math.min(1d, value));
	}

	/**
	 * Smooths one coarse remaining-duration varbit into a continuous countdown.
	 */
	private static final class Countdown
	{
		private final int ticksPerUnit;
		private int rawUnits;
		private int anchorTick = -1;
		private double remainingTicksAtAnchor;
		private double totalTicks;
		private boolean estimated = true;

		private Countdown(int ticksPerUnit)
		{
			this.ticksPerUnit = ticksPerUnit;
		}

		private void observe(int units, boolean enabled, int currentTick, boolean exactChange)
		{
			if (!enabled || units <= 0)
			{
				reset();
				return;
			}

			final boolean wasActive = isActive();
			if (!wasActive || units != rawUnits)
			{
				final double newRemainingTicks = units * (double) ticksPerUnit;

				/*
				 * A larger value is a refresh and starts a new full ring.
				 * Normal decreases preserve the original denominator.
				 */
				if (!wasActive || units > rawUnits)
				{
					totalTicks = newRemainingTicks;
				}

				rawUnits = units;
				remainingTicksAtAnchor = newRemainingTicks;
				anchorTick = currentTick;
				estimated = !exactChange;
			}
		}

		private boolean isActive()
		{
			return anchorTick >= 0 && rawUnits > 0;
		}

		private double getProgress(int currentTick, double subTickProgress)
		{
			if (!isActive() || totalTicks <= 0d)
			{
				return 0d;
			}
			return clamp(getRemainingTicks(currentTick, subTickProgress) / totalTicks);
		}

		private double getRemainingTicks(int currentTick, double subTickProgress)
		{
			if (!isActive())
			{
				return 0d;
			}

			final double elapsed = Math.max(
				0d,
				currentTick - anchorTick + clamp(subTickProgress)
			);
			return Math.max(0d, remainingTicksAtAnchor - elapsed);
		}

		private boolean isEstimated()
		{
			return isActive() && estimated;
		}

		private void reset()
		{
			rawUnits = 0;
			anchorTick = -1;
			remainingTicksAtAnchor = 0d;
			totalTicks = 0d;
			estimated = true;
		}
	}
}