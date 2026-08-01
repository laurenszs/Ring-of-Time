package com.ringoftime;

import com.ringoftime.RingOfTimeConfig.PlusMinusPosition;
import com.ringoftime.RingOfTimeConfig.InnerRingDisplay;
import com.ringoftime.RingOfTimeConfig.LevelDisplay;
import com.ringoftime.RingOfTimeConfig.TimerPosition;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.FontManager;

/**
 * Renders exactly one independently draggable skill timer.
 *
 * <p>The thick outer ring shows total time until this skill returns to its base
 * level. For a multi-level buff or debuff, the optional thin inner ring shows
 * the time until its next one-level change.</p>
 */
final class SkillTimerOverlay extends TimerCircleOverlay
{
	private static final int ARC_START_DEGREES = 90;
	private static final int TEXT_SHADOW_OFFSET = 1;
	private static final Color DIVINE_INDICATOR_COLOR = new Color(130, 235, 255, 245);
	private static final Color DIVINE_INDICATOR_OUTLINE_COLOR = new Color(0, 0, 0, 210);

	private final Client client;
	private final RingOfTimePlugin plugin;
	private final RingOfTimeConfig config;
	private final SkillIconManager skillIconManager;
	private final Skill skill;

	/**
	 * Creates one overlay entity for one skill. The plugin constructs all
	 * instances explicitly so each can have a distinct persistent overlay name.
	 */
	SkillTimerOverlay(
		Client client,
		RingOfTimePlugin plugin,
		RingOfTimeConfig config,
		SkillIconManager skillIconManager,
		Skill skill)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.skillIconManager = skillIconManager;
		this.skill = skill;

	}

	@Override
	public String getName()
	{
		return "Ring of Time: " + skill.getName();
	}

	/**
	 * Draws this skill only, or remains absent while its timer is inactive.
	 *
	 * @return this circle's bounds, or {@code null} when it should not appear
	 */
	@Override
	public Dimension render(Graphics2D graphics)
	{
		final StatChangeTracker tracker = plugin.getTracker();
		final DivineTimerTracker divineTracker = plugin.getDivineTracker();
		final int delta = tracker.getDelta(skill);
		final boolean divine = divineTracker.isActive(skill);
		if (!isTimerActive())
		{
			return null;
		}

		final int ringSize = config.ringSize();
		final int currentTick = client.getTickCount();
		final double subTickProgress = plugin.getSubTickProgress();
		final long nowMillis = System.currentTimeMillis();
		final int remainingSeconds = divine
			? divineTracker.getRemainingSeconds(skill, nowMillis)
			: tracker.getRemainingSeconds(
				skill,
				currentTick,
				subTickProgress,
				plugin.isPreserveActive()
			);
		final TimerPosition timerPosition = config.timerPosition();
		final String timeText = !timerPosition.isShown()
			? null
			: formatDuration(remainingSeconds);
		final LevelDisplay levelDisplay = divine || delta > 0
			? config.buffLevelDisplay()
			: config.debuffLevelDisplay();
		final PlusMinusPosition plusMinusPosition = divine || delta > 0
			? config.buffPlusMinusPosition()
			: config.debuffPlusMinusPosition();
		final String plusMinusText = plusMinusPosition == PlusMinusPosition.OFF
			? null
			: levelDisplay == LevelDisplay.TOTAL_LEVEL
				? Integer.toString(client.getBoostedSkillLevel(skill))
				: formatDelta(delta);
		final Font plusMinusFont = FontManager.getRunescapeBoldFont().deriveFont((float) config.plusMinusFontSize());
		final InnerRingDisplay innerRingDisplay = divine || delta > 0
			? config.buffInnerRing()
			: config.debuffInnerRing();
		final boolean innerRingVisible = !divine
			&& Math.abs(delta) > 1
			&& innerRingDisplay.isShown();
		final int thickness = Math.min(config.ringThickness(), Math.max(2, ringSize / 3));
		final int outlineThickness = config.showRingOutline() ? config.outlineThickness() : 0;
		final int clearCenterSize = TimerLabelLayout.getClearCenterSize(
			ringSize,
			thickness,
			outlineThickness,
			innerRingVisible
		);
		final Font preferredTimerFont = FontManager.getRunescapeBoldFont()
			.deriveFont((float) config.timerFontSize());
		final Font timerFont = TimerLabelLayout.fitTimerFont(
			graphics,
			preferredTimerFont,
			timeText,
			clearCenterSize
		);
		final TimerLabelLayout labelLayout = TimerLabelLayout.create(
			ringSize,
			graphics.getFontMetrics(timerFont),
			timeText,
			timerPosition,
			graphics.getFontMetrics(plusMinusFont),
			plusMinusText,
			plusMinusPosition,
			config.iconPosition()
		);

		/*
		 * A child graphics context contains our translation, antialiasing, stroke,
		 * and font changes so they cannot affect RuneLite or another overlay.
		 */
		final Graphics2D canvas = (Graphics2D) graphics.create();
		try
		{
			canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			canvas.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
			canvas.translate(labelLayout.getRingX(), labelLayout.getRingY());

			drawTimer(
				canvas,
				delta,
				divine,
				ringSize,
				currentTick,
				subTickProgress,
				nowMillis,
				remainingSeconds,
				timeText,
				plusMinusText,
				timerFont,
				plusMinusFont,
				labelLayout
			);
		}
		finally
		{
			canvas.dispose();
		}

		return labelLayout.getDimension();
	}

	/**
	 * Draws the total ring, optional next-change ring, positioned icon, and labels.
	 */
	private void drawTimer(
		Graphics2D graphics,
		int delta,
		boolean divine,
		int size,
		int currentTick,
		double subTickProgress,
		long nowMillis,
		int remainingSeconds,
		String timeText,
		String plusMinusText,
		Font timerFont,
		Font plusMinusFont,
		TimerLabelLayout labelLayout)
	{
		final StatChangeTracker tracker = plugin.getTracker();
		final double totalProgress = divine
			? plugin.getDivineTracker().getProgress(skill, nowMillis)
			: tracker.getOverallProgress(skill, currentTick, subTickProgress);
		final int thickness = Math.min(config.ringThickness(), Math.max(2, size / 3));
		final int outlineThickness = config.showRingOutline() ? config.outlineThickness() : 0;
		final double edgeMargin = Math.max(1d, outlineThickness);
		final double inset = thickness / 2d + edgeMargin;
		final double diameter = size - thickness - edgeMargin * 2d;
		final Arc2D.Double ring = new Arc2D.Double(
			inset,
			inset,
			diameter,
			diameter,
			ARC_START_DEGREES,
			-360d,
			Arc2D.OPEN
		);
		final Color outlineColor = divine || delta > 0
			? config.buffOutlineColor()
			: config.debuffOutlineColor();
		final Color innerRingColor = divine || delta > 0
			? config.buffInnerRingColor()
			: config.debuffInnerRingColor();

		if (config.showRingOutline())
		{
			graphics.setStroke(new BasicStroke(
				thickness + outlineThickness * 2,
				BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND
			));
			graphics.setColor(outlineColor);
			graphics.draw(ring);
		}

		graphics.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(config.emptyRingColor());
		graphics.draw(ring);

		final Color activeColor = divine || delta > 0
			? (config.useBaseBuffColor() ? config.buffColor() : SkillColorPalette.getColor(skill, config))
			: config.debuffColor();
		ring.setAngleExtent(-360d * totalProgress);
		graphics.setColor(activeColor);
		graphics.draw(ring);

		if (divine || delta > 0)
		{
			drawExpiryFlash(
				graphics,
				ring,
				thickness,
				activeColor,
				remainingSeconds,
				currentTick,
				subTickProgress
			);
		}
		if (divine)
		{
			drawDivineIndicator(graphics, size, thickness, outlineThickness);
		}

		final InnerRingDisplay innerRingDisplay = divine || delta > 0
			? config.buffInnerRing()
			: config.debuffInnerRing();
		if (!divine && Math.abs(delta) > 1 && innerRingDisplay.isShown())
		{
			drawNextLevelIndicator(
				graphics,
				size,
				thickness,
				outlineThickness,
				tracker.getNextChangeProgress(skill, currentTick, subTickProgress),
				innerRingColor,
				outlineColor
			);
		}

		drawSkillIcon(graphics, labelLayout.getIconBounds());
		final Color timerTextColor = getTimerTextColor(
			delta,
			plugin.isPreserveActive() && !divine
		);
		drawLabel(
			graphics,
			timeText,
			labelLayout.getTimerBounds(),
			timerTextColor,
			timerFont,
			false
		);
		final Color plusMinusColor = divine || delta > 0
			? config.buffPlusMinusColor()
			: config.debuffPlusMinusColor();
		drawLabel(
			graphics,
			plusMinusText,
			labelLayout.getPlusMinusBounds(),
			plusMinusColor,
			plusMinusFont,
			true
		);
	}
	/**
	 * Marks a fixed-duration divine potion with a small crystalline diamond at
	 * the top of the ring without replacing that skill's configured color.
	 */
	private static void drawDivineIndicator(
		Graphics2D graphics,
		int size,
		int ringThickness,
		int outlineThickness)
	{
		final Polygon marker = createDivineIndicator(size, ringThickness, outlineThickness);
		graphics.setColor(DIVINE_INDICATOR_COLOR);
		graphics.fillPolygon(marker);
		graphics.setStroke(new BasicStroke(1f));
		graphics.setColor(DIVINE_INDICATOR_OUTLINE_COLOR);
		graphics.drawPolygon(marker);
	}

	/**
	 * Builds a compact marker centered on the outer ring's twelve-o'clock point.
	 */
	static Polygon createDivineIndicator(int size, int ringThickness, int outlineThickness)
	{
		final int halfWidth = Math.max(2, Math.min(4, (ringThickness + 1) / 2));
		final int halfHeight = halfWidth + 1;
		final int centerX = size / 2;
		final int centerY = Math.max(
			halfHeight,
			(int) Math.round(Math.max(1d, outlineThickness) + ringThickness / 2d)
		);
		return new Polygon(
			new int[]{centerX, centerX + halfWidth, centerX, centerX - halfWidth},
			new int[]{centerY - halfHeight, centerY, centerY + halfHeight, centerY},
			4
		);
	}

	/**
	 * Flashes a complete ring in its own configured color near beneficial expiry.
	 * Game-tick time keeps the pulse deterministic and independent of frame rate.
	 */
	private void drawExpiryFlash(
		Graphics2D graphics,
		Arc2D.Double ring,
		int thickness,
		Color color,
		int remainingSeconds,
		int currentTick,
		double subTickProgress)
	{
		if (!config.flashExpiringBuffs()
			|| remainingSeconds <= 0
			|| remainingSeconds > config.flashThresholdSeconds())
		{
			return;
		}

		final long elapsedMillis = Math.round(
			(currentTick + subTickProgress) * net.runelite.api.Constants.GAME_TICK_LENGTH
		);
		final boolean brightPhase = (elapsedMillis / 300L) % 2L == 0L;
		final int pulseAlpha = Math.min(color.getAlpha(), brightPhase ? 235 : 55);
		ring.setAngleExtent(-360d);
		graphics.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), pulseAlpha));
		graphics.draw(ring);
	}

	/**
	 * Draws the thin countdown to the next one-level buff or debuff change.
	 */
	private void drawNextLevelIndicator(
		Graphics2D graphics,
		int size,
		int outerThickness,
		int outlineThickness,
		double progress,
		Color innerRingColor,
		Color outlineColor)
	{
		final int innerThickness = Math.max(2, Math.min(4, (outerThickness + 1) / 2));
		final double gap = Math.max(2d, outerThickness * 0.35d);
		final double edgeMargin = Math.max(1d, outlineThickness);
		final double inset = outerThickness
			+ edgeMargin
			+ outlineThickness
			+ gap
			+ innerThickness / 2d;
		final double diameter = size - inset * 2d;

		// Very small, unusually thick rings may not have safe room for a second arc.
		if (diameter < 10d)
		{
			return;
		}

		final Arc2D.Double nextLevelRing = new Arc2D.Double(
			inset,
			inset,
			diameter,
			diameter,
			ARC_START_DEGREES,
			-360d,
			Arc2D.OPEN
		);
		if (config.showRingOutline())
		{
			graphics.setStroke(new BasicStroke(
				innerThickness + outlineThickness * 2,
				BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND
			));
			graphics.setColor(outlineColor);
			graphics.draw(nextLevelRing);
		}

		graphics.setStroke(new BasicStroke(innerThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(withReducedAlpha(config.emptyRingColor()));
		graphics.draw(nextLevelRing);

		nextLevelRing.setAngleExtent(-360d * progress);
		graphics.setColor(innerRingColor);
		graphics.draw(nextLevelRing);
	}

	private void drawSkillIcon(Graphics2D graphics, Rectangle bounds)
	{
		final Image icon = skillIconManager.getSkillImage(skill);
		if (icon == null)
		{
			return;
		}

		final int sourceWidth = Math.max(1, icon.getWidth(null));
		final int sourceHeight = Math.max(1, icon.getHeight(null));
		final double scale = Math.min(
			1d,
			Math.min(bounds.width / (double) sourceWidth, bounds.height / (double) sourceHeight)
		);
		final int iconWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
		final int iconHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
		final int iconX = bounds.x + (bounds.width - iconWidth) / 2;
		final int iconY = bounds.y + (bounds.height - iconHeight) / 2;

		graphics.drawImage(icon, iconX, iconY, iconWidth, iconHeight, null);
	}

	/**
	 * Draws fitted timer text without a background, or a +/- value with its
	 * configurable badge background.
	 */
	private void drawLabel(
		Graphics2D graphics,
		String text,
		Rectangle bounds,
		Color textColor,
		Font font,
		boolean drawBackground)
	{
		if (text == null || bounds == null)
		{
			return;
		}

		final FontMetrics metrics = graphics.getFontMetrics(font);
		final int textX = bounds.x + (bounds.width - metrics.stringWidth(text)) / 2;
		final int baselineY = bounds.y + (bounds.height - metrics.getHeight()) / 2
			+ metrics.getAscent();

		if (drawBackground)
		{
			graphics.setColor(config.plusMinusBackgroundColor());
			graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
		}
		graphics.setFont(font);
		graphics.setColor(new Color(0, 0, 0, 235));
		graphics.drawString(
			text,
			textX + TEXT_SHADOW_OFFSET,
			baselineY + TEXT_SHADOW_OFFSET
		);
		graphics.setColor(textColor);
		graphics.drawString(text, textX, baselineY);
	}

	/**
	 * Includes this skill in automatic stacking only while it is visible and
	 * differs from its real level.
	 */
	@Override
	boolean isTimerActive()
	{
		final int delta = plugin.getTracker().getDelta(skill);
		if (!plugin.isSkillVisible(skill))
		{
			return false;
		}

		return plugin.getDivineTracker().isActive(skill)
			|| delta > 0
			|| (delta < 0 && config.debuffDisplay().showsSkills());
	}

	/**
	 * Uses cyan only while Preserve can extend an active positive skill buff.
	 * Re-evaluating this every frame restores white as soon as Preserve turns off.
	 */
	static Color getTimerTextColor(int delta, boolean preserveActive)
	{
		return delta > 0 && preserveActive ? Color.CYAN : Color.WHITE;
	}

	/**
	 * Makes the inner ring's unused track quieter than the primary outer track.
	 */
	private static Color withReducedAlpha(Color color)
	{
		return new Color(
			color.getRed(),
			color.getGreen(),
			color.getBlue(),
			Math.max(30, color.getAlpha() / 2)
		);
	}

	/**
	 * Formats long countdowns compactly enough to fit inside or below the ring.
	 */
	private static String formatDuration(int totalSeconds)
	{
		if (totalSeconds < 60)
		{
			return totalSeconds + "s";
		}

		final int minutes = totalSeconds / 60;
		final int seconds = totalSeconds % 60;
		return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
	}

	/**
	 * Makes positive and negative changes visually unambiguous.
	 */
	private static String formatDelta(int delta)
	{
		return delta > 0 ? "+" + delta : Integer.toString(delta);
	}
}