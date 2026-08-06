package com.ringoftime;

import com.ringoftime.EffectTimerTracker.Effect;
import net.runelite.api.ChatMessageType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies effect-specific timing without starting or automating the client.
 */
public class EffectTimerTrackerTest
{
	private static final double TOLERANCE = 0.0001d;

	private EffectTimerTracker tracker;

	@Before
	public void setUp()
	{
		tracker = new EffectTimerTracker();
		tracker.reset();
	}

	@Test
	public void poisonProjectsNaturalCureAndNextDamage()
	{
		tracker.observePoison(10, 100, true);

		assertTrue(tracker.isActive(Effect.TOXIN));
		assertFalse(tracker.isVenom());
		assertFalse(tracker.isEstimated(Effect.TOXIN));
		assertEquals(1d, tracker.getOverallProgress(Effect.TOXIN, 100, 0d), TOLERANCE);
		assertEquals(182, tracker.getRemainingSeconds(Effect.TOXIN, 100, 0d));
		assertEquals(2, tracker.getNextToxinDamage());
		assertTrue(tracker.hasMultiplePoisonCycles());

		// A natural one-step poison decay preserves the original total scale.
		tracker.observePoison(9, 131, true);
		assertEquals(0.9d, tracker.getOverallProgress(Effect.TOXIN, 131, 0d), TOLERANCE);
		assertEquals(164, tracker.getRemainingSeconds(Effect.TOXIN, 131, 0d));
	}

	@Test
	public void venomUsesARepeatingHitCycleThenCanBecomePoison()
	{
		tracker.observePoison(EffectTimerTracker.VENOM_THRESHOLD, 20, true);

		assertTrue(tracker.isVenom());
		assertEquals(6, tracker.getNextToxinDamage());
		assertEquals(1d, tracker.getOverallProgress(Effect.TOXIN, 20, 0d), TOLERANCE);
		assertEquals(19, tracker.getRemainingSeconds(Effect.TOXIN, 20, 0d));
		assertFalse(tracker.hasMultiplePoisonCycles());

		// An actual game-state transition reveals a new full green poison timer.
		tracker.observePoison(25, 30, true);
		assertFalse(tracker.isVenom());
		assertEquals(5, tracker.getNextToxinDamage());
		assertEquals(1d, tracker.getOverallProgress(Effect.TOXIN, 30, 0d), TOLERANCE);
		assertEquals(455, tracker.getRemainingSeconds(Effect.TOXIN, 30, 0d));
	}

	@Test
	public void antipoisonAndAntivenomProtectionUseNegativePoisonValues()
	{
		tracker.observeAntipoison(-10, 100, true);

		assertTrue(tracker.isActive(Effect.ANTIPOISON));
		assertFalse(tracker.isAntivenomProtection());
		assertFalse(tracker.isEstimated(Effect.ANTIPOISON));
		assertEquals(1d, tracker.getOverallProgress(Effect.ANTIPOISON, 100, 0d), TOLERANCE);
		assertEquals(180, tracker.getRemainingSeconds(Effect.ANTIPOISON, 100, 0d));

		// A normal 30-tick block decrease preserves the original denominator.
		tracker.observeAntipoison(-9, 130, true);
		assertEquals(0.9d, tracker.getOverallProgress(Effect.ANTIPOISON, 130, 0d), TOLERANCE);
		assertEquals(162, tracker.getRemainingSeconds(Effect.ANTIPOISON, 130, 0d));

		// Values below RuneLite's cutoff identify anti-venom protection.
		tracker.observeAntipoison(-40, 200, true);
		assertTrue(tracker.isAntivenomProtection());
		assertEquals(36, tracker.getRemainingSeconds(Effect.ANTIPOISON, 200, 0d));
		assertEquals(1d, tracker.getOverallProgress(Effect.ANTIPOISON, 200, 0d), TOLERANCE);

		tracker.observeAntipoison(0, 201, true);
		assertFalse(tracker.isActive(Effect.ANTIPOISON));
		assertFalse(tracker.isAntivenomProtection());
	}

	@Test
	public void staminaVarbitIsSmoothedAndRefreshes()
	{
		tracker.observeStamina(12, true, 0, true);
		assertEquals(72, tracker.getRemainingSeconds(Effect.STAMINA, 0, 0d));
		assertEquals(5d / 6d, tracker.getOverallProgress(Effect.STAMINA, 20, 0d), TOLERANCE);

		// Increasing the duration starts a new full countdown denominator.
		tracker.observeStamina(15, true, 20, true);
		assertEquals(1d, tracker.getOverallProgress(Effect.STAMINA, 20, 0d), TOLERANCE);
		assertEquals(90, tracker.getRemainingSeconds(Effect.STAMINA, 20, 0d));

		tracker.observeStamina(15, false, 21, true);
		assertFalse(tracker.isActive(Effect.STAMINA));
	}

	@Test
	public void antifireStrengthsUseTheirOwnGameUnitSizes()
	{
		tracker.observeAntifire(4, 3, 0, true);

		assertTrue(tracker.isActive(Effect.ANTIFIRE));
		assertTrue(tracker.isActive(Effect.SUPER_ANTIFIRE));
		assertEquals(72, tracker.getRemainingSeconds(Effect.ANTIFIRE, 0, 0d));
		assertEquals(36, tracker.getRemainingSeconds(Effect.SUPER_ANTIFIRE, 0, 0d));

		assertEquals(66, tracker.getRemainingSeconds(Effect.ANTIFIRE, 10, 0d));
		assertEquals(30, tracker.getRemainingSeconds(Effect.SUPER_ANTIFIRE, 10, 0d));
	}

	@Test
	public void prayerRegenerationUsesTwelveTickUnitsAndShowsNextRestore()
	{
		tracker.observePrayerRegeneration(40, 100, true);

		assertTrue(tracker.isActive(Effect.PRAYER_REGENERATION));
		assertEquals(288, tracker.getRemainingSeconds(Effect.PRAYER_REGENERATION, 100, 0d));
		assertEquals(1d, tracker.getPrayerRegenerationCycleProgress(100, 0d), TOLERANCE);
		assertEquals(0.5d, tracker.getPrayerRegenerationCycleProgress(106, 0d), TOLERANCE);

		tracker.observePrayerRegeneration(39, 112, true);
		assertEquals(39d / 40d, tracker.getOverallProgress(
			Effect.PRAYER_REGENERATION,
			112,
			0d
		), TOLERANCE);
		assertEquals(1d, tracker.getPrayerRegenerationCycleProgress(112, 0d), TOLERANCE);

		tracker.observePrayerRegeneration(50, 120, true);
		assertEquals(1d, tracker.getOverallProgress(Effect.PRAYER_REGENERATION, 120, 0d), TOLERANCE);
		assertEquals(360, tracker.getRemainingSeconds(Effect.PRAYER_REGENERATION, 120, 0d));

		tracker.observePrayerRegeneration(0, 121, true);
		assertFalse(tracker.isActive(Effect.PRAYER_REGENERATION));
	}
	@Test
	public void thrallDurationUsesBoostedMagicAndMasterTier()
	{
		tracker.observeThrallCooldown(true, 96);
		tracker.startThrall(90, false, 100);
		assertTrue(tracker.isActive(Effect.THRALL));
		assertFalse(tracker.isEstimated(Effect.THRALL));
		assertEquals(1d, tracker.getOverallProgress(Effect.THRALL, 100, 0d), TOLERANCE);
		assertEquals(54, tracker.getRemainingSeconds(Effect.THRALL, 100, 0d));
		assertEquals(0.5d, tracker.getOverallProgress(Effect.THRALL, 145, 0d), TOLERANCE);
		assertTrue(tracker.isThrallCooldownActive());
		assertEquals(13d / 17d, tracker.getThrallCooldownProgress(100, 0d), TOLERANCE);

		// Repeated active observations must not move the original cooldown anchor.
		tracker.observeThrallCooldown(true, 100);
		assertEquals(13d / 17d, tracker.getThrallCooldownProgress(100, 0d), TOLERANCE);
		assertEquals(0.5d, tracker.getThrallCooldownProgress(104, 0.5d), TOLERANCE);
		tracker.observeThrallCooldown(false, 113);
		assertFalse(tracker.isThrallCooldownActive());

		tracker.startThrall(90, true, 200);
		assertEquals(108, tracker.getRemainingSeconds(Effect.THRALL, 200, 0d));

		tracker.observeThrallActive(false);
		assertFalse(tracker.isActive(Effect.THRALL));
	}

	@Test
	public void onlySuccessfulThrallGameMessagesStartTheTimer()
	{
		final String message = "<col=ef1020>You resurrect a greater ghost thrall.</col>";
		assertTrue(RingOfTimePlugin.isThrallSummonMessage(ChatMessageType.GAMEMESSAGE, message));
		assertTrue(RingOfTimePlugin.isThrallSummonMessage(ChatMessageType.SPAM, message));
		assertFalse(RingOfTimePlugin.isThrallSummonMessage(ChatMessageType.PUBLICCHAT, message));
		assertFalse(RingOfTimePlugin.isThrallSummonMessage(
			ChatMessageType.GAMEMESSAGE,
			"<col=ef1020>You do not have enough runes.</col>"
		));
	}
}
