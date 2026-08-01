package com.ringoftime;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

/**
 * Adapts one timer ring to RuneLite's wrapping panel-component API.
 */
final class TimerCircleComponent implements LayoutableRenderableEntity
{
	private final TimerCircleOverlay timer;
	private final Rectangle bounds = new Rectangle();
	private Point preferredLocation = new Point();
	private Dimension preferredSize = new Dimension();

	TimerCircleComponent(TimerCircleOverlay timer)
	{
		this.timer = timer;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final Graphics2D canvas = (Graphics2D) graphics.create();
		try
		{
			canvas.translate(preferredLocation.x, preferredLocation.y);
			final Dimension timerSize = timer.render(canvas);
			if (timerSize == null)
			{
				bounds.setBounds(preferredLocation.x, preferredLocation.y, 0, 0);
				return new Dimension();
			}

			bounds.setBounds(
				preferredLocation.x,
				preferredLocation.y,
				timerSize.width,
				timerSize.height
			);
			return new Dimension(bounds.width, bounds.height);
		}
		finally
		{
			canvas.dispose();
		}
	}

	TimerCircleOverlay getTimer()
	{
		return timer;
	}

	@Override
	public Rectangle getBounds()
	{
		return bounds;
	}

	@Override
	public void setPreferredLocation(Point position)
	{
		preferredLocation = position == null ? new Point() : new Point(position);
	}

	@Override
	public void setPreferredSize(Dimension dimension)
	{
		// Wrapping is based on the timer's rendered bounds, not padded cells.
	}
}