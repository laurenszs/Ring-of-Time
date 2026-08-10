package com.ringoftime;

import com.ringoftime.EffectTimerTracker.Effect;
import com.ringoftime.RingOfTimeConfig.PlusMinusPosition;
import com.ringoftime.RingOfTimeConfig.TimerPosition;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import net.runelite.api.Client;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;

/**
 * Renders one independently draggable non-skill effect timer.
 *
 * <p>The toxin instance switches between poison and venom instead of creating
 * overlapping entities for mutually exclusive states. Regular and super
 * antifire remain separate entities because both game variables can exist and
 * have different countdown scales.</p>
 */
final class EffectTimerOverlay extends TimerCircleOverlay
{
	private static final int ARC_START_DEGREES = 90;
	private static final int TEXT_SHADOW_OFFSET = 1;

	private final Client client;
	private final RingOfTimePlugin plugin;
	private final RingOfTimeConfig config;
	private final Effect effect;
	private volatile Image primaryIcon;
	private final Image alternateIcon;

	/**
	 * Creates one persistent RuneLite overlay key for one effect family.
	 */
	EffectTimerOverlay(
		Client client,
		RingOfTimePlugin plugin,
		RingOfTimeConfig config,
		ItemManager itemManager,
		SpriteManager spriteManager,
		Effect effect)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.effect = effect;
		this.primaryIcon = loadPrimaryIcon(itemManager, effect);
		if (effect == Effect.THRALL && spriteManager != null)
		{
			spriteManager.getSpriteAsync(
				SpriteID.MagicNecroOn.RESURRECT_SUPERIOR_SKELETON,
				0,
				image -> primaryIcon = image
			);
		}
		this.alternateIcon = itemManager != null
			&& (effect == Effect.TOXIN || effect == Effect.ANTIPOISON)
			? itemManager.getImage(ItemID.ANTIVENOM4)
			: null;

	}

	@Override
	public String getName()
	{
		return getOverlayName(effect);
	}

	static String getOverlayName(Effect effect)
	{
		switch (effect)
		{
			case TOXIN:
				return persistentName("Poison and Venom");
			case ANTIPOISON:
				return persistentName("Antipoison Protection");
			case STAMINA:
				return persistentName("Stamina");
			case ANTIFIRE:
				return persistentName("Antifire");
			case SUPER_ANTIFIRE:
				return persistentName("Super Antifire");
			case PRAYER_REGENERATION:
				return persistentName("Prayer Regeneration");
			case THRALL:
				return persistentName("Thrall");
			default:
				return persistentName("Effect");
		}
	}

	/**
	 * Draws this effect only while both active and enabled in configuration.
	 */
	@Override
	public Dimension render(Graphics2D graphics)
	{
		final EffectTimerTracker tracker = plugin.getEffectTracker();
		if (!isTimerActive())
		{
			return null;
		}

		final int size = config.ringSize();
		final int currentTick = client.getTickCount();
		final double subTickProgress = plugin.getSubTickProgress();
		final int remainingSeconds = tracker.getRemainingSeconds(
			effect,
			currentTick,
			subTickProgress
		);
		final TimerPosition timerPosition = config.timerPosition();
		final String timeText = !timerPosition.isShown()
			? null
			: formatDuration(remainingSeconds);
		final PlusMinusPosition plusMinusPosition = effect == Effect.TOXIN
			? config.debuffPlusMinusPosition()
			: PlusMinusPosition.OFF;
		final String plusMinusText = plusMinusPosition == PlusMinusPosition.OFF
			? null
			: Integer.toString(tracker.getNextToxinDamage());
		final Font plusMinusFont = FontManager.getRunescapeBoldFont().deriveFont((float) config.plusMinusFontSize());
		final boolean poisonInnerRingVisible = effect == Effect.TOXIN
			&& tracker.hasMultiplePoisonCycles()
			&& config.poisonInnerRing().isShown();
		final boolean thrallCooldownVisible = effect == Effect.THRALL
			&& tracker.isThrallCooldownActive();
		final boolean prayerRegenerationCycleVisible = effect == Effect.PRAYER_REGENERATION;
		final boolean innerRingVisible = poisonInnerRingVisible
			|| thrallCooldownVisible
			|| prayerRegenerationCycleVisible;
		final int thickness = Math.min(config.ringThickness(), Math.max(2, size / 3));
		final int outlineThickness = config.showRingOutline() ? config.outlineThickness() : 0;
		final int clearCenterSize = TimerLabelLayout.getClearCenterSize(
			size,
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
			size,
			graphics.getFontMetrics(timerFont),
			timeText,
			timerPosition,
			graphics.getFontMetrics(plusMinusFont),
			plusMinusText,
			plusMinusPosition,
			config.effectIconPosition()
		);

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
				tracker,
				size,
				currentTick,
				subTickProgress,
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
	 * Draws the effect ring, optional poison cycle, positioned icon, and labels.
	 */
	private void drawTimer(
		Graphics2D graphics,
		EffectTimerTracker tracker,
		int size,
		int currentTick,
		double subTickProgress,
		int remainingSeconds,
		String timeText,
		String plusMinusText,
		Font timerFont,
		Font plusMinusFont,
		TimerLabelLayout labelLayout)
	{
		final Color activeColor = getActiveColor(tracker);
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

		if (config.showRingOutline())
		{
			graphics.setStroke(new BasicStroke(
				thickness + outlineThickness * 2,
				BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND
			));
			graphics.setColor(effect == Effect.TOXIN
				? config.debuffOutlineColor()
				: config.effectOutlineColor());
			graphics.draw(ring);
		}

		/*
		 * Venom uses a complete poison-green track under its dark-green active
		 * arc. An actual conversion turns this entity into the poison countdown.
		 */
		final boolean venom = effect == Effect.TOXIN && tracker.isVenom();
		final Color trackColor = venom ? config.poisonColor() : config.emptyRingColor();
		graphics.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(trackColor);
		graphics.draw(ring);

		ring.setAngleExtent(-360d * tracker.getOverallProgress(effect, currentTick, subTickProgress));
		graphics.setColor(activeColor);
		graphics.draw(ring);

		if (effect != Effect.TOXIN)
		{
			final Color flashColor = effect == Effect.THRALL
				? config.thrallFlashColor()
				: activeColor;
			drawExpiryFlash(
				graphics,
				ring,
				thickness,
				flashColor,
				remainingSeconds,
				currentTick,
				subTickProgress
			);
		}

		if (effect == Effect.TOXIN
			&& tracker.hasMultiplePoisonCycles()
			&& config.poisonInnerRing().isShown())
		{
			drawInnerRing(
				graphics,
				size,
				thickness,
				outlineThickness,
				tracker.getNextPoisonHitProgress(currentTick, subTickProgress),
				config.toxinInnerRingColor(),
				config.debuffOutlineColor()
			);
		}
		else if (effect == Effect.THRALL && tracker.isThrallCooldownActive())
		{
			drawInnerRing(
				graphics,
				size,
				thickness,
				outlineThickness,
				tracker.getThrallCooldownProgress(currentTick, subTickProgress),
				config.buffInnerRingColor(),
				config.effectOutlineColor()
			);
		}
		else if (effect == Effect.PRAYER_REGENERATION)
		{
			drawInnerRing(
				graphics,
				size,
				thickness,
				outlineThickness,
				tracker.getPrayerRegenerationCycleProgress(currentTick, subTickProgress),
				config.buffInnerRingColor(),
				config.effectOutlineColor()
			);
		}

		final boolean alternateProtectionIcon = effect == Effect.ANTIPOISON
			&& tracker.isAntivenomProtection();
		drawIcon(
			graphics,
			labelLayout.getIconBounds(),
			venom || alternateProtectionIcon ? alternateIcon : primaryIcon
		);
		drawLabel(
			graphics,
			timeText,
			labelLayout.getTimerBounds(),
			Color.WHITE,
			timerFont,
			false
		);
		drawLabel(
			graphics,
			plusMinusText,
			labelLayout.getPlusMinusBounds(),
			config.debuffPlusMinusColor(),
			plusMinusFont,
			true
		);
	}
	/**
	 * Flashes beneficial effects in their own configured color near expiry.
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
	 * Draws a secondary countdown inside an effect's total-duration ring.
	 */
	private void drawInnerRing(
		Graphics2D graphics,
		int size,
		int outerThickness,
		int outlineThickness,
		double progress,
		Color activeColor,
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
		if (diameter < 10d)
		{
			return;
		}

		final Arc2D.Double innerRing = new Arc2D.Double(
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
			graphics.draw(innerRing);
		}

		graphics.setStroke(new BasicStroke(innerThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(withReducedAlpha(config.emptyRingColor()));
		graphics.draw(innerRing);

		innerRing.setAngleExtent(-360d * progress);
		graphics.setColor(activeColor);
		graphics.draw(innerRing);
	}

	/**
	 * Applies each requested default color to its effect family.
	 */
	private Color getActiveColor(EffectTimerTracker tracker)
	{
		switch (effect)
		{
			case TOXIN:
				return tracker.isVenom() ? config.venomColor() : config.poisonColor();
			case ANTIPOISON:
				return tracker.isAntivenomProtection()
					? config.antivenomProtectionColor()
					: config.antipoisonColor();
			case STAMINA:
				return config.staminaColor();
			case ANTIFIRE:
			case SUPER_ANTIFIRE:
				return config.antifireColor();
			case PRAYER_REGENERATION:
				return config.prayerRegenerationColor();
			case THRALL:
				return config.thrallColor();
			default:
				return Color.WHITE;
		}
	}

	/**
	 * Includes this effect in automatic stacking only while its authoritative
	 * state is active and its Effects-section switch is enabled.
	 */
	@Override
	boolean isTimerActive()
	{
		return plugin.getEffectTracker().isActive(effect) && isConfiguredVisible();
	}

	/**
	 * Applies the Effects-section enable switches.
	 */
	private boolean isConfiguredVisible()
	{
		switch (effect)
		{
			case TOXIN:
				return config.debuffDisplay().showsPoisonVenom();
			case ANTIPOISON:
				return config.showAntipoison();
			case STAMINA:
				return config.showStamina();
			case ANTIFIRE:
			case SUPER_ANTIFIRE:
				return config.showAntifire();
			case PRAYER_REGENERATION:
				return config.showPrayerRegeneration();
			case THRALL:
				return config.showThrall();
			default:
				return false;
		}
	}

	private static Image loadPrimaryIcon(ItemManager itemManager, Effect effect)
	{
		if (itemManager == null || effect == Effect.THRALL)
		{
			return null;
		}

		switch (effect)
		{
			case TOXIN:
				return itemManager.getImage(ItemID._4DOSEANTIPOISON);
			case ANTIPOISON:
				return itemManager.getImage(ItemID._4DOSEANTIPOISON);
			case STAMINA:
				return itemManager.getImage(ItemID._4DOSESTAMINA);
			case ANTIFIRE:
				return itemManager.getImage(ItemID._4DOSE1ANTIDRAGON);
			case SUPER_ANTIFIRE:
				return itemManager.getImage(ItemID._4DOSE3ANTIDRAGON);
			case PRAYER_REGENERATION:
				return itemManager.getImage(ItemID._4DOSE1PRAYER_REGENERATION);
			default:
				return null;
		}
	}

	/**
	 * Scales the effect image into its configured icon slot.
	 */
	private static void drawIcon(Graphics2D graphics, Rectangle bounds, Image icon)
	{
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
		graphics.drawImage(
			icon,
			bounds.x + (bounds.width - iconWidth) / 2,
			bounds.y + (bounds.height - iconHeight) / 2,
			iconWidth,
			iconHeight,
			null
		);
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

	private static Color withReducedAlpha(Color color)
	{
		return new Color(
			color.getRed(),
			color.getGreen(),
			color.getBlue(),
			Math.max(30, color.getAlpha() / 2)
		);
	}

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
}
