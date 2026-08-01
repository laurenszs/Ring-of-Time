package com.ringoftime;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimerGroupOverlayTest
{
	@Test
	public void circleComponentUsesRenderedSizeAndTracksItsBounds()
	{
		final TimerCircleComponent component = new TimerCircleComponent(
			new FakeTimer(true, new Dimension(40, 30))
		);
		component.setPreferredLocation(new Point(10, 20));

		final BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			assertEquals(new Dimension(40, 30), component.render(graphics));
			assertEquals(10, component.getBounds().x);
			assertEquals(20, component.getBounds().y);
			assertEquals(40, component.getBounds().width);
			assertEquals(30, component.getBounds().height);
		}
		finally
		{
			graphics.dispose();
		}
	}

	@Test
	public void orientationChangesDefaultGroupRows()
	{
		final RingOfTimeConfig config = new RingOfTimeConfig() { };
		final TimerGroupOverlay group = new TimerGroupOverlay(
			null,
			null,
			config,
			null,
			"Test group",
			ComponentOrientation.VERTICAL
		);
		for (int i = 0; i < 5; i++)
		{
			group.addMember(new FakeTimer(true, new Dimension(50, 50)));
		}

		final Dimension cell = TimerLabelLayout.estimateMaximumDimension(config);
		assertEquals(cell.height * 4 + 26, group.estimateAutomaticHeight(cell));

		group.flip();
		assertEquals(cell.height * 2 + 22, group.estimateAutomaticHeight(cell));
	}

	@Test
	public void resizingStopsAutomaticPlacement()
	{
		final RingOfTimeConfig config = new RingOfTimeConfig() { };
		final TimerGroupOverlay group = new TimerGroupOverlay(
			null,
			null,
			config,
			null,
			"Test group",
			ComponentOrientation.VERTICAL
		);
		group.initializeAutomaticLayout();

		assertTrue(group.applyAutomaticLocation(new Point(10, 10)));
		group.setPreferredSize(new Dimension(100, 100));
		assertFalse(group.applyAutomaticLocation(new Point(10, 80)));
	}

	private static final class FakeTimer extends TimerCircleOverlay
	{
		private final boolean active;
		private final Dimension dimension;

		private FakeTimer(boolean active, Dimension dimension)
		{
			super(null);
			this.active = active;
			this.dimension = dimension;
		}

		@Override
		public String getName()
		{
			return "Fake timer";
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			return new Dimension(dimension);
		}

		@Override
		boolean isTimerActive()
		{
			return active;
		}
	}
}
