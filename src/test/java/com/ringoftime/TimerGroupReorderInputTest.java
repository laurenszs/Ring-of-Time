package com.ringoftime;

import java.awt.Canvas;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimerGroupReorderInputTest
{
	@Test
	public void requiresBothControlAndAlt()
	{
		final Canvas canvas = new Canvas();
		assertTrue(TimerGroupReorderInput.hasReorderModifiers(mouseEvent(
			canvas,
			InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK
		)));
		assertFalse(TimerGroupReorderInput.hasReorderModifiers(mouseEvent(
			canvas,
			InputEvent.ALT_DOWN_MASK
		)));
		assertFalse(TimerGroupReorderInput.hasReorderModifiers(mouseEvent(
			canvas,
			InputEvent.CTRL_DOWN_MASK
		)));
	}

	private static MouseEvent mouseEvent(Canvas canvas, int modifiers)
	{
		return new MouseEvent(
			canvas,
			MouseEvent.MOUSE_PRESSED,
			0L,
			modifiers,
			0,
			0,
			1,
			false,
			MouseEvent.BUTTON1
		);
	}
}
