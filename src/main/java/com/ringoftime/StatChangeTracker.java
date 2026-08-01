package com.ringoftime;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.Constants;
import net.runelite.api.Skill;

/**
 * Tracks temporary skill changes independently from RuneLite's rendering code.
 *
 * <p>Old School RuneScape restores boosted stats downward and drained stats
 * upward on separate shared clocks. A natural one-level change reveals the
 * exact phase of its clock. Until that happens, this tracker starts a local
 * estimate so the user still gets an immediately useful visual.</p>
 *
 * <p>The overall progress for a skill combines its current level difference
 * with the fractional progress of the current shared clock cycle. This makes
 * the annulus shrink continuously across each one-level transition.</p>
 */
final class StatChangeTracker
{
	static final int NORMAL_CYCLE_TICKS = 100;
	static final int PRESERVE_CYCLE_TICKS = 150;

	private static final int UNKNOWN_DELTA = Integer.MIN_VALUE;

	private final int[] lastDeltas = new int[Skill.values().length];
	private final int[] peakMagnitudes = new int[Skill.values().length];
	private final EnumSet<Skill> activeSkills = EnumSet.noneOf(Skill.class);
	private final Set<Skill> readOnlyActiveSkills = Collections.unmodifiableSet(activeSkills);
	private final CycleTimer buffCycle = new CycleTimer(true);
	private final CycleTimer debuffCycle = new CycleTimer(false);

	StatChangeTracker()
	{
		reset();
	}

	/**
	 * Clears all learned levels and timer phases, such as after a logout or hop.
	 */
	void reset()
	{
		Arrays.fill(lastDeltas, UNKNOWN_DELTA);
		Arrays.fill(peakMagnitudes, 0);
		activeSkills.clear();
		buffCycle.stop();
		debuffCycle.stop();
	}

	/**
	 * Observes the latest levels for one skill and updates its timer state.
	 *
	 * @param skill skill whose level changed or was sampled
	 * @param boostedLevel current effective level reported by the client
	 * @param realLevel base level reported by the client
	 * @param currentTick RuneLite game-tick counter at the observation time
	 */
	void observe(Skill skill, int boostedLevel, int realLevel, int currentTick)
	{
		final int index = skill.ordinal();
		final int previousDelta = lastDeltas[index];
		final int delta = boostedLevel - realLevel;

		/*
		 * A one-level move toward zero is the normal restoration event. It
		 * provides an exact anchor for the appropriate shared stat clock.
		 */
		if (previousDelta > 0 && delta == previousDelta - 1)
		{
			buffCycle.synchronize(currentTick);
		}
		else if (previousDelta < 0 && delta == previousDelta + 1)
		{
			debuffCycle.synchronize(currentTick);
		}

		if (delta == 0)
		{
			// Returning to the real level removes the skill's visual entirely.
			activeSkills.remove(skill);
			peakMagnitudes[index] = 0;
		}
		else
		{
			final int magnitude = Math.abs(delta);

			/*
			 * A new effect or a direction change starts a fresh visual. A
			 * stronger re-boost/re-drain expands the visual's reference size.
			 */
			if (previousDelta == UNKNOWN_DELTA
				|| previousDelta == 0
				|| Integer.signum(previousDelta) != Integer.signum(delta))
			{
				peakMagnitudes[index] = magnitude;
			}
			else
			{
				peakMagnitudes[index] = Math.max(peakMagnitudes[index], magnitude);
			}

			activeSkills.add(skill);

			/*
			 * The first cycle is estimated because the server does not expose
			 * its current stat-clock phase. A natural restoration later syncs it.
			 */
			cycleFor(delta).startEstimatedIfStopped(currentTick);
		}

		lastDeltas[index] = delta;

		// Do not carry an obsolete clock into a future, unrelated effect.
		if (!hasDirection(true))
		{
			buffCycle.stop();
		}
		if (!hasDirection(false))
		{
			debuffCycle.stop();
		}
	}

	/**
	 * Advances shared clocks and applies Preserve's current extension state.
	 *
	 * @param currentTick current RuneLite game tick
	 * @param preserveActive whether the Preserve prayer is currently active
	 */
	void onGameTick(int currentTick, boolean preserveActive)
	{
		if (hasDirection(true))
		{
			buffCycle.onGameTick(currentTick, preserveActive);
		}
		if (hasDirection(false))
		{
			debuffCycle.onGameTick(currentTick, false);
		}
	}

	/**
	 * Returns a read-only live view of skills that differ from their base level.
	 */
	Set<Skill> getActiveSkills()
	{
		return readOnlyActiveSkills;
	}

	/**
	 * Returns the signed effective-level difference for a skill.
	 */
	int getDelta(Skill skill)
	{
		final int delta = lastDeltas[skill.ordinal()];
		return delta == UNKNOWN_DELTA ? 0 : delta;
	}

	/**
	 * Calculates the fraction of the complete effect that remains.
	 *
	 * @param skill active skill to inspect
	 * @param currentTick current RuneLite game tick
	 * @param subTickProgress fraction from the latest game tick to the next
	 * @return value clamped to the inclusive range {@code [0, 1]}
	 */
	double getOverallProgress(Skill skill, int currentTick, double subTickProgress)
	{
		final int delta = getDelta(skill);
		if (delta == 0)
		{
			return 0d;
		}

		final int magnitude = Math.abs(delta);
		final int peak = Math.max(magnitude, peakMagnitudes[skill.ordinal()]);
		final double cycleProgress = cycleFor(delta).getProgress(currentTick, subTickProgress);

		/*
		 * Completed level cycles count as whole units; the current cycle is the
		 * fractional unit. This keeps the arc continuous at restoration ticks.
		 */
		final double overall = (magnitude - 1d + cycleProgress) / peak;
		return clamp(overall);
	}

	/**
	 * Calculates the fraction remaining until the next one-level change.
	 *
	 * <p>The overlay uses this separate progress value for its thin inner
	 * annulus. Unlike {@link #getOverallProgress(Skill, int, double)}, it
	 * returns to a full circle after each natural level restoration.</p>
	 *
	 * @param skill active skill to inspect
	 * @param currentTick current RuneLite game tick
	 * @param subTickProgress fraction from the latest game tick to the next
	 * @return current shared-cycle progress in the range {@code [0, 1]}
	 */
	double getNextChangeProgress(Skill skill, int currentTick, double subTickProgress)
	{
		final int delta = getDelta(skill);
		return delta == 0
			? 0d
			: cycleFor(delta).getProgress(currentTick, subTickProgress);
	}

	/**
	 * Projects seconds until the skill reaches its real level.
	 *
	 * <p>Future Preserve use cannot be known, so future buff cycles assume the
	 * prayer remains in its current state.</p>
	 */
	int getRemainingSeconds(
		Skill skill,
		int currentTick,
		double subTickProgress,
		boolean preserveActive)
	{
		final int delta = getDelta(skill);
		if (delta == 0)
		{
			return 0;
		}

		final CycleTimer cycle = cycleFor(delta);
		final int magnitude = Math.abs(delta);
		final double currentCycleTicks = cycle.getRemainingTicks(currentTick, subTickProgress);
		final int projectedCycleTicks = delta > 0 && preserveActive
			? PRESERVE_CYCLE_TICKS
			: NORMAL_CYCLE_TICKS;
		final double totalTicks = currentCycleTicks + (magnitude - 1d) * projectedCycleTicks;

		return (int) Math.ceil(totalTicks * Constants.GAME_TICK_LENGTH / 1000d);
	}

	/**
	 * Reports whether the selected skill still uses an estimated clock phase.
	 */
	boolean isEstimated(Skill skill)
	{
		final int delta = getDelta(skill);
		return delta != 0 && cycleFor(delta).isEstimated();
	}

	/**
	 * Selects the independent buff or debuff restoration clock.
	 */
	private CycleTimer cycleFor(int delta)
	{
		return delta > 0 ? buffCycle : debuffCycle;
	}

	/**
	 * Checks whether any active skill currently uses the requested direction.
	 */
	private boolean hasDirection(boolean buff)
	{
		for (Skill skill : activeSkills)
		{
			final int delta = getDelta(skill);
			if ((buff && delta > 0) || (!buff && delta < 0))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Clamps floating-point rounding at the start and end of an animation.
	 */
	private static double clamp(double value)
	{
		return Math.max(0d, Math.min(1d, value));
	}

	/**
	 * Models one of RuneScape's two shared stat-restoration clocks.
	 */
	private static final class CycleTimer
	{
		private final boolean buff;
		private int startTick = -1;
		private int cycleLengthTicks = NORMAL_CYCLE_TICKS;
		private boolean estimated = true;
		private boolean preserveWasActiveInExtensionWindow;

		private CycleTimer(boolean buff)
		{
			this.buff = buff;
		}

		/**
		 * Starts a best-effort clock only when no clock is currently running.
		 */
		private void startEstimatedIfStopped(int currentTick)
		{
			if (startTick == -1)
			{
				restart(currentTick, true);
			}
		}

		/**
		 * Anchors the next cycle to an observed natural restoration tick.
		 */
		private void synchronize(int currentTick)
		{
			restart(currentTick, false);
		}

		/**
		 * Updates Preserve timing and rolls over a cycle if its event was missed.
		 */
		private void onGameTick(int currentTick, boolean preserveActive)
		{
			if (startTick == -1)
			{
				return;
			}

			final int elapsedTicks = Math.max(0, currentTick - startTick);

			if (buff)
			{
				/*
				 * This mirrors RuneLite's boost-clock model. Preserve can extend
				 * the normal 100-tick cycle to 125 or 150 ticks depending on the
				 * portion of the cycle during which it remains active.
				 */
				final boolean useFullExtension =
					(preserveActive && (elapsedTicks < 75 || preserveWasActiveInExtensionWindow))
						|| elapsedTicks > 125;

				if (useFullExtension)
				{
					preserveWasActiveInExtensionWindow = true;
					cycleLengthTicks = PRESERVE_CYCLE_TICKS;
				}
				else
				{
					preserveWasActiveInExtensionWindow = false;
					cycleLengthTicks = elapsedTicks > 100 ? 125 : NORMAL_CYCLE_TICKS;
				}
			}
			else
			{
				// Drained stats always restore on the normal 100-tick cycle.
				cycleLengthTicks = NORMAL_CYCLE_TICKS;
			}

			if (elapsedTicks >= cycleLengthTicks)
			{
				/*
				 * Missing the server event means the new phase is uncertain.
				 * Restart visibly, but mark it estimated until an event syncs it.
				 */
				restart(currentTick, true);
			}
		}

		/**
		 * Returns the remaining fraction of the current level-change cycle.
		 */
		private double getProgress(int currentTick, double subTickProgress)
		{
			if (startTick == -1)
			{
				return 0d;
			}
			return clamp(getRemainingTicks(currentTick, subTickProgress) / cycleLengthTicks);
		}

		/**
		 * Returns smooth, fractional ticks left in the current cycle.
		 */
		private double getRemainingTicks(int currentTick, double subTickProgress)
		{
			if (startTick == -1)
			{
				return 0d;
			}

			final double elapsed = Math.max(0d, currentTick - startTick + clamp(subTickProgress));
			return Math.max(0d, cycleLengthTicks - elapsed);
		}

		private boolean isEstimated()
		{
			return estimated;
		}

		/**
		 * Stops the clock and removes all Preserve history.
		 */
		private void stop()
		{
			startTick = -1;
			cycleLengthTicks = NORMAL_CYCLE_TICKS;
			estimated = true;
			preserveWasActiveInExtensionWindow = false;
		}

		/**
		 * Initializes a new cycle with a known or estimated phase.
		 */
		private void restart(int currentTick, boolean estimated)
		{
			startTick = currentTick;
			cycleLengthTicks = NORMAL_CYCLE_TICKS;
			this.estimated = estimated;
			preserveWasActiveInExtensionWindow = false;
		}
	}
}
