package com.ringoftime;

import com.ringoftime.RingOfTimeConfig.PlusMinusPosition;
import com.ringoftime.RingOfTimeConfig.IconPosition;
import com.ringoftime.RingOfTimeConfig.TimerPosition;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.Rectangle;

/**
 * Measures the positioned timer, +/- badge, and movable icon as one overlay.
 *
 * <p>The countdown uses its selected vertical anchor. Icon and +/- bounds move
 * away from the center if necessary and stack instead of overlapping. Above
 * and under icon positions extend the component only in that direction.</p>
 */
final class TimerLabelLayout
{
	private static final int LABEL_GAP = 3;
	private static final int STACK_GAP = 2;
	private static final int HORIZONTAL_PADDING = 3;
	private static final int VERTICAL_PADDING = 1;

	private final Dimension dimension;
	private final int ringX;
	private final int ringY;
	private final Rectangle timerBounds;
	private final Rectangle plusMinusBounds;
	private final Rectangle iconBounds;

	private TimerLabelLayout(
		Dimension dimension,
		int ringX,
		int ringY,
		Rectangle timerBounds,
		Rectangle plusMinusBounds,
		Rectangle iconBounds)
	{
		this.dimension = dimension;
		this.ringX = ringX;
		this.ringY = ringY;
		this.timerBounds = timerBounds;
		this.plusMinusBounds = plusMinusBounds;
		this.iconBounds = iconBounds;
	}

	/**
	 * Measures the current strings and returns positions relative to the ring.
	 */
	static TimerLabelLayout create(
		int ringSize,
		FontMetrics timerMetrics,
		String timerText,
		TimerPosition timerPosition,
		FontMetrics plusMinusMetrics,
		String plusMinusText,
		PlusMinusPosition plusMinusPosition,
		IconPosition iconPosition)
	{
		final int timerWidth = timerText == null
			? 0
			: timerMetrics.stringWidth(timerText) + HORIZONTAL_PADDING * 2;
		final int plusMinusWidth = plusMinusText == null
			? 0
			: plusMinusMetrics.stringWidth(plusMinusText) + HORIZONTAL_PADDING * 2;
		final int timerHeight = timerMetrics.getHeight() + VERTICAL_PADDING * 2;
		final int plusMinusHeight = plusMinusMetrics.getHeight() + VERTICAL_PADDING * 2;

		return createWithSizes(
			ringSize,
			timerWidth,
			timerText == null || !timerPosition.isShown() ? 0 : timerHeight,
			timerPosition,
			plusMinusWidth,
			plusMinusText == null ? 0 : plusMinusHeight,
			plusMinusPosition,
			iconPosition
		);
	}

	/**
	 * Returns a conservative component size for automatic vertical placement
	 * before a particular countdown string has been rendered.
	 */
	static Dimension estimateMaximumDimension(RingOfTimeConfig config)
	{
		final int timerFontSize = config.timerFontSize();
		final int plusMinusFontSize = config.plusMinusFontSize();
		final int timerWidth = timerFontSize * 5 + HORIZONTAL_PADDING * 2;
		final int plusMinusWidth = plusMinusFontSize * 3 + HORIZONTAL_PADDING * 2;
		final int timerHeight = config.timerPosition().isShown() ? timerFontSize + 8 : 0;
		final int plusMinusHeight = plusMinusFontSize + 8;

		final TimerLabelLayout buffLayout = createWithSizes(
			config.ringSize(),
			timerWidth,
			timerHeight,
			config.timerPosition(),
			plusMinusWidth,
			config.buffPlusMinusPosition() == PlusMinusPosition.OFF ? 0 : plusMinusHeight,
			config.buffPlusMinusPosition(),
			config.iconPosition()
		);
		final TimerLabelLayout debuffLayout = createWithSizes(
			config.ringSize(),
			timerWidth,
			timerHeight,
			config.timerPosition(),
			plusMinusWidth,
			config.debuffPlusMinusPosition() == PlusMinusPosition.OFF ? 0 : plusMinusHeight,
			config.debuffPlusMinusPosition(),
			config.iconPosition()
		);

		final TimerLabelLayout effectLayout = createWithSizes(
			config.ringSize(),
			timerWidth,
			timerHeight,
			config.timerPosition(),
			0,
			0,
			PlusMinusPosition.OFF,
			config.effectIconPosition()
		);

		return new Dimension(
			Math.max(
				Math.max(buffLayout.dimension.width, debuffLayout.dimension.width),
				effectLayout.dimension.width
			),
			Math.max(
				Math.max(buffLayout.dimension.height, debuffLayout.dimension.height),
				effectLayout.dimension.height
			)
		);
	}

	/**
	 * Returns the clear center diameter inside the active outer or inner ring.
	 */
	static int getClearCenterSize(
		int ringSize,
		int outerThickness,
		int outlineThickness,
		boolean innerRingVisible)
	{
		final double edgeMargin = Math.max(1d, outlineThickness);
		if (!innerRingVisible)
		{
			final double outerDiameter = ringSize - outerThickness - edgeMargin * 2d;
			return Math.max(
				8,
				(int) Math.floor(outerDiameter - outerThickness - outlineThickness * 2d)
			);
		}

		final int innerThickness = Math.max(2, Math.min(4, (outerThickness + 1) / 2));
		final double gap = Math.max(2d, outerThickness * 0.35d);
		final double inset = outerThickness
			+ edgeMargin
			+ outlineThickness
			+ gap
			+ innerThickness / 2d;
		final double innerDiameter = ringSize - inset * 2d;
		return Math.max(
			8,
			(int) Math.floor(innerDiameter - innerThickness - outlineThickness * 2d)
		);
	}

	/**
	 * Treats the configured font size as a maximum and shrinks countdown text
	 * until both its width and height fit inside the clear center diameter.
	 */
	static Font fitTimerFont(
		Graphics2D graphics,
		Font preferredFont,
		String text,
		int clearCenterSize)
	{
		if (text == null)
		{
			return preferredFont;
		}

		final int available = Math.max(6, clearCenterSize - 2);
		for (int size = Math.round(preferredFont.getSize2D()); size >= 5; size--)
		{
			final Font candidate = preferredFont.deriveFont((float) size);
			final FontMetrics metrics = graphics.getFontMetrics(candidate);
			if (metrics.stringWidth(text) + 1 <= available
				&& metrics.getHeight() + 1 <= available)
			{
				return candidate;
			}
		}

		return preferredFont.deriveFont(5f);
	}
	private static TimerLabelLayout createWithSizes(
		int ringSize,
		int timerWidth,
		int timerHeight,
		TimerPosition timerPosition,
		int plusMinusWidth,
		int plusMinusHeight,
		PlusMinusPosition plusMinusPosition,
		IconPosition iconPosition)
	{
		final Rectangle ringBounds = new Rectangle(0, 0, ringSize, ringSize);
		final Rectangle timerBounds = timerHeight == 0
			? null
			: placeTimer(ringSize, timerWidth, timerHeight, timerPosition);
		Rectangle iconBounds = placeIcon(ringSize, iconPosition);
		Rectangle plusMinusBounds = plusMinusHeight == 0
			? null
			: placePlusMinus(ringSize, plusMinusWidth, plusMinusHeight, plusMinusPosition);

		plusMinusBounds = moveAwayFromTimer(plusMinusBounds, timerBounds, isTop(plusMinusPosition));
		iconBounds = moveAwayFromTimer(iconBounds, timerBounds, isTop(iconPosition));
		if (plusMinusBounds != null && plusMinusBounds.intersects(iconBounds))
		{
			iconBounds = stackIconOutside(iconBounds, plusMinusBounds, isTop(iconPosition));
		}

		Rectangle componentBounds = new Rectangle(ringBounds);
		componentBounds = union(componentBounds, timerBounds);
		componentBounds = union(componentBounds, plusMinusBounds);
		componentBounds = union(componentBounds, iconBounds);

		return new TimerLabelLayout(
			new Dimension(componentBounds.width, componentBounds.height),
			-componentBounds.x,
			-componentBounds.y,
			timerBounds,
			plusMinusBounds,
			iconBounds
		);
	}

	private static Rectangle placeTimer(
		int ringSize,
		int width,
		int height,
		TimerPosition position)
	{
		final int x = Math.floorDiv(ringSize - width, 2);
		switch (position)
		{
			case TOP:
				return new Rectangle(x, 0, width, height);
			case MIDDLE:
				return new Rectangle(x, (ringSize - height) / 2, width, height);
			case BOTTOM:
				return new Rectangle(x, ringSize - height, width, height);
			default:
				throw new IllegalArgumentException("Unsupported timer position: " + position);
		}
	}

	private static Rectangle placeIcon(int ringSize, IconPosition position)
	{
		final int size = Math.max(14, (int) (ringSize * 0.40));
		switch (position)
		{
			case TOP_LEFT:
				return new Rectangle(0, 0, size, size);
			case TOP_RIGHT:
				return new Rectangle(ringSize - size, 0, size, size);
			case BOTTOM_RIGHT:
				return new Rectangle(ringSize - size, ringSize - size, size, size);
			case BOTTOM_LEFT:
				return new Rectangle(0, ringSize - size, size, size);
			case CENTER:
				return new Rectangle((ringSize - size) / 2, (ringSize - size) / 2, size, size);
			case ABOVE_RING:
				return new Rectangle((ringSize - size) / 2, -size - LABEL_GAP, size, size);
			case UNDER_RING:
				return new Rectangle((ringSize - size) / 2, ringSize + LABEL_GAP, size, size);
			default:
				throw new IllegalArgumentException("Unsupported icon position: " + position);
		}
	}

	private static Rectangle placePlusMinus(
		int ringSize,
		int width,
		int height,
		PlusMinusPosition position)
	{
		if (position == PlusMinusPosition.CENTER)
		{
			return new Rectangle(
				Math.floorDiv(ringSize - width, 2),
				(ringSize - height) / 2,
				width,
				height
			);
		}

		final int x = isLeft(position) ? 0 : ringSize - width;
		final int y = isTop(position) ? 0 : ringSize - height;
		return new Rectangle(x, y, width, height);
	}

	/**
	 * Keeps the center countdown readable while retaining the selected edge.
	 */
	private static Rectangle moveAwayFromTimer(
		Rectangle bounds,
		Rectangle timerBounds,
		boolean towardTop)
	{
		if (bounds == null || timerBounds == null || !bounds.intersects(timerBounds))
		{
			return bounds;
		}

		final Rectangle moved = new Rectangle(bounds);
		moved.y = towardTop
			? timerBounds.y - STACK_GAP - moved.height
			: timerBounds.y + timerBounds.height + STACK_GAP;
		return moved;
	}

	/**
	 * Preserves the configured +/- anchor and moves a colliding icon outward.
	 */
	private static Rectangle stackIconOutside(
		Rectangle iconBounds,
		Rectangle plusMinusBounds,
		boolean towardTop)
	{
		final Rectangle moved = new Rectangle(iconBounds);
		moved.y = towardTop
			? plusMinusBounds.y - STACK_GAP - moved.height
			: plusMinusBounds.y + plusMinusBounds.height + STACK_GAP;
		return moved;
	}

	private static Rectangle union(Rectangle component, Rectangle addition)
	{
		if (addition == null)
		{
			return component;
		}

		return component.union(addition);
	}

	private static boolean isTop(IconPosition position)
	{
		return position == IconPosition.TOP_LEFT
			|| position == IconPosition.TOP_RIGHT
			|| position == IconPosition.ABOVE_RING;
	}

	private static boolean isLeft(PlusMinusPosition position)
	{
		return position == PlusMinusPosition.TOP_LEFT || position == PlusMinusPosition.BOTTOM_LEFT;
	}

	private static boolean isTop(PlusMinusPosition position)
	{
		return position == PlusMinusPosition.TOP_LEFT || position == PlusMinusPosition.TOP_RIGHT;
	}

	Dimension getDimension()
	{
		return new Dimension(dimension);
	}

	int getRingX()
	{
		return ringX;
	}

	int getRingY()
	{
		return ringY;
	}

	Rectangle getTimerBounds()
	{
		return timerBounds == null ? null : new Rectangle(timerBounds);
	}

	Rectangle getPlusMinusBounds()
	{
		return plusMinusBounds == null ? null : new Rectangle(plusMinusBounds);
	}

	Rectangle getIconBounds()
	{
		return new Rectangle(iconBounds);
	}
}