package com.ringoftime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RingOfTimeUpdateMessageTest
{
	@Test
	public void updateMessageIsOnlyPendingForAnUnseenVersion()
	{
		assertTrue(RingOfTimePlugin.shouldShowUpdateMessage(null));
		assertTrue(RingOfTimePlugin.shouldShowUpdateMessage("1.2.0"));
		assertFalse(RingOfTimePlugin.shouldShowUpdateMessage(
			RingOfTimePlugin.CURRENT_VERSION
		));
	}

	@Test
	public void updateMessageWaitsThreeSecondsAfterLogin()
	{
		assertEquals(3L, RingOfTimePlugin.UPDATE_MESSAGE_DELAY_SECONDS);
	}
}
