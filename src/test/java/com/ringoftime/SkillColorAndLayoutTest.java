package com.ringoftime;

import com.ringoftime.RingOfTimeConfig.PlusMinusPosition;
import com.ringoftime.RingOfTimeConfig.DebuffDisplay;
import com.ringoftime.RingOfTimeConfig.IconPosition;
import com.ringoftime.RingOfTimeConfig.TimerPosition;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.plugins.PluginDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the pure visual defaults without starting a RuneLite client.
 */
public class SkillColorAndLayoutTest
{
	@Test
	public void everySupportedSkillHasADistinctThemedColor()
	{
		final Set<Skill> supportedSkills = EnumSet.copyOf(RingOfTimePlugin.COMBAT_SKILLS);
		supportedSkills.addAll(RingOfTimePlugin.NON_COMBAT_SKILLS);
		final Set<Color> colors = new HashSet<>();

		for (Skill skill : supportedSkills)
		{
			final Color color = SkillColorPalette.getDefaultColor(skill);
			assertNotEquals("Supported skills must not use the fallback", Color.WHITE, color);
			colors.add(color);
		}

		assertEquals(22, supportedSkills.size());
		assertEquals("Every supported skill should have its own color", 22, colors.size());
	}

	@Test
	public void verticalLayoutChangesOnlyTheYCoordinate()
	{
		assertEquals(
			new Point(10, 76),
			TimerCircleLayout.getLocation(1, 62)
		);
	}

	@Test
	public void centerTimerAvoidsPlusMinusAndIcon()
	{
		final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			final Font font = new Font(Font.SANS_SERIF, Font.BOLD, 16);
			final TimerLabelLayout layout = TimerLabelLayout.create(
				52,
				graphics.getFontMetrics(font),
				"1:23",
				TimerPosition.MIDDLE,
				graphics.getFontMetrics(font),
				"+5",
				PlusMinusPosition.TOP_RIGHT,
				IconPosition.TOP_LEFT
			);
			assertEquals(26d, layout.getTimerBounds().getCenterX(), 1d);
			assertEquals(26d, layout.getTimerBounds().getCenterY(), 1d);
			assertFalse(layout.getTimerBounds().intersects(layout.getPlusMinusBounds()));
			assertFalse(layout.getTimerBounds().intersects(layout.getIconBounds()));
			assertFalse(layout.getPlusMinusBounds().intersects(layout.getIconBounds()));
		}
		finally
		{
			graphics.dispose();
		}
	}

	@Test
	public void timerFontFitsInsideVisibleInnerRing()
	{
		final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			final int clearSize = TimerLabelLayout.getClearCenterSize(52, 6, 2, true);
			final Font preferred = new Font(Font.SANS_SERIF, Font.BOLD, 16);
			final Font fitted = TimerLabelLayout.fitTimerFont(
				graphics,
				preferred,
				"12:34",
				clearSize
			);

			assertTrue(graphics.getFontMetrics(fitted).stringWidth("12:34") + 1 <= clearSize - 2);
			assertTrue(graphics.getFontMetrics(fitted).getHeight() + 1 <= clearSize - 2);
		}
		finally
		{
			graphics.dispose();
		}
	}

	@Test
	public void changingIconPositionDoesNotMovePlusMinusAnchor()
	{
		final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			final Font font = new Font(Font.SANS_SERIF, Font.BOLD, 16);
			final TimerLabelLayout top = TimerLabelLayout.create(
				52,
				graphics.getFontMetrics(font),
				"42s",
				TimerPosition.MIDDLE,
				graphics.getFontMetrics(font),
				"+5",
				PlusMinusPosition.TOP_RIGHT,
				IconPosition.TOP_RIGHT
			);
			final TimerLabelLayout bottom = TimerLabelLayout.create(
				52,
				graphics.getFontMetrics(font),
				"42s",
				TimerPosition.MIDDLE,
				graphics.getFontMetrics(font),
				"+5",
				PlusMinusPosition.TOP_RIGHT,
				IconPosition.BOTTOM_RIGHT
			);

			assertEquals(top.getPlusMinusBounds(), bottom.getPlusMinusBounds());
		}
		finally
		{
			graphics.dispose();
		}
	}
	@Test
	public void timerCanBeTurnedOffWithoutHidingTheIcon()
	{
		final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			final TimerLabelLayout layout = TimerLabelLayout.create(
				52,
				graphics.getFontMetrics(new Font(Font.SANS_SERIF, Font.BOLD, 16)),
				null,
				TimerPosition.OFF,
				graphics.getFontMetrics(new Font(Font.SANS_SERIF, Font.BOLD, 16)),
				null,
				PlusMinusPosition.OFF,
				IconPosition.UNDER_RING
			);

			assertNull(layout.getTimerBounds());
			assertTrue(layout.getIconBounds().y > 52);
		}
		finally
		{
			graphics.dispose();
		}
	}

	@Test
	public void debuffDropdownSeparatesSkillsFromPoisonAndVenom()
	{
		assertTrue(DebuffDisplay.SKILLS.showsSkills());
		assertFalse(DebuffDisplay.SKILLS.showsPoisonVenom());
		assertFalse(DebuffDisplay.POISON_VENOM.showsSkills());
		assertTrue(DebuffDisplay.POISON_VENOM.showsPoisonVenom());
		assertTrue(DebuffDisplay.BOTH.showsSkills());
		assertTrue(DebuffDisplay.BOTH.showsPoisonVenom());
		assertFalse(DebuffDisplay.OFF.showsSkills());
		assertFalse(DebuffDisplay.OFF.showsPoisonVenom());
	}

	@Test
	public void aboveIconReservesItsOwnRow()
	{
		final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			final Font font = new Font(Font.SANS_SERIF, Font.BOLD, 16);
			final TimerLabelLayout layout = TimerLabelLayout.create(
				52,
				graphics.getFontMetrics(font),
				"42s",
				TimerPosition.MIDDLE,
				graphics.getFontMetrics(font),
				null,
				PlusMinusPosition.OFF,
				IconPosition.ABOVE_RING
			);

			assertTrue(layout.getRingY() > 0);
			assertTrue(layout.getIconBounds().y < 0);
			assertEquals(26d, layout.getTimerBounds().getCenterY(), 1d);
			assertTrue(layout.getDimension().height > 52);
		}
		finally
		{
			graphics.dispose();
		}
	}
	@Test
	public void requestedDefaultsAreExposed()
	{
		final RingOfTimeConfig config = new RingOfTimeConfig() { };

		assertEquals(1, config.outlineThickness());
		assertEquals(50, config.ringSize());
		assertEquals(10, config.timerFontSize());
		assertEquals(10, config.plusMinusFontSize());
		assertEquals(5, config.ringThickness());
		assertFalse(config.showRingOutline());
		assertEquals(new Color(0, 0, 0, 50), config.plusMinusBackgroundColor());
		assertEquals(IconPosition.TOP_LEFT, config.iconPosition());
		assertEquals(PlusMinusPosition.TOP_RIGHT, config.buffPlusMinusPosition());
		assertEquals(PlusMinusPosition.TOP_RIGHT, config.debuffPlusMinusPosition());
		assertEquals(RingOfTimeConfig.TrackedSkills.ALL, config.trackedSkills());
	}

	@Test
	public void timerSupportsTopAndBottomAnchors()
	{
		final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			final Font font = new Font(Font.SANS_SERIF, Font.BOLD, 10);
			final TimerLabelLayout top = TimerLabelLayout.create(
				50, graphics.getFontMetrics(font), "42s", TimerPosition.TOP,
				graphics.getFontMetrics(font), null, PlusMinusPosition.OFF, IconPosition.UNDER_RING
			);
			final TimerLabelLayout bottom = TimerLabelLayout.create(
				50, graphics.getFontMetrics(font), "42s", TimerPosition.BOTTOM,
				graphics.getFontMetrics(font), null, PlusMinusPosition.OFF, IconPosition.ABOVE_RING
			);

			assertEquals(0, top.getTimerBounds().y);
			assertEquals(50, bottom.getTimerBounds().y + bottom.getTimerBounds().height);
		}
		finally
		{
			graphics.dispose();
		}
	}

	@Test
	public void iconAndPlusMinusSupportCenterOfCircle()
	{
		final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			final Font font = new Font(Font.SANS_SERIF, Font.BOLD, 10);
			final TimerLabelLayout plusMinusCenter = TimerLabelLayout.create(
				50, graphics.getFontMetrics(font), null, TimerPosition.OFF,
				graphics.getFontMetrics(font), "+5", PlusMinusPosition.CENTER, IconPosition.ABOVE_RING
			);
			final TimerLabelLayout iconCenter = TimerLabelLayout.create(
				50, graphics.getFontMetrics(font), null, TimerPosition.OFF,
				graphics.getFontMetrics(font), null, PlusMinusPosition.OFF, IconPosition.CENTER
			);

			assertEquals(25d, plusMinusCenter.getPlusMinusBounds().getCenterX(), 1d);
			assertEquals(25d, plusMinusCenter.getPlusMinusBounds().getCenterY(), 1d);
			assertEquals(25d, iconCenter.getIconBounds().getCenterX(), 1d);
			assertEquals(25d, iconCenter.getIconBounds().getCenterY(), 1d);
		}
		finally
		{
			graphics.dispose();
		}
	}

	@Test
	public void preserveOnlyColorsActiveBuffTimersCyan()
	{
		assertEquals(Color.CYAN, SkillTimerOverlay.getTimerTextColor(5, true));
		assertEquals(Color.WHITE, SkillTimerOverlay.getTimerTextColor(5, false));
		assertEquals(Color.WHITE, SkillTimerOverlay.getTimerTextColor(-5, true));
	}

	@Test
	public void pluginUsesRingOfTimeIdentity()
	{
		assertEquals(
			"Ring of Time",
			RingOfTimePlugin.class.getAnnotation(PluginDescriptor.class).name()
		);
		assertEquals(
			"ring-of-time",
			RingOfTimeConfig.class.getAnnotation(ConfigGroup.class).value()
		);
	}
	@Test
	public void configUsesApprovedSectionAndSettingNames() throws Exception
	{
		assertEquals(
			"Duplicate timers",
			RingOfTimeConfig.class.getField("DUPLICATE_TIMERS_SECTION")
				.getAnnotation(ConfigSection.class).name()
		);
		assertEquals(
			"Labels & icons",
			RingOfTimeConfig.class.getField("LABELS_SECTION")
				.getAnnotation(ConfigSection.class).name()
		);
		assertEquals(
			"Ring style",
			RingOfTimeConfig.class.getField("RING_STYLE_SECTION")
				.getAnnotation(ConfigSection.class).name()
		);
		assertEquals(
			"Individual buff colors",
			RingOfTimeConfig.class.getField("BUFF_COLORS_SECTION")
				.getAnnotation(ConfigSection.class).name()
		);
		assertEquals(
			"Protection & effect timers",
			RingOfTimeConfig.class.getField("EFFECTS_SECTION")
				.getAnnotation(ConfigSection.class).name()
		);

		assertConfigItemName("trackedSkills", "Shown buffs");
		assertConfigItemName("debuffDisplay", "Shown debuffs");
		assertConfigItemName("buffInnerRing", "Next-change ring");
		assertConfigItemName("useBaseBuffColor", "Use one buff color");
		assertConfigItemName("buffColor", "Shared buff color");
		assertConfigItemName("flashThresholdSeconds", "Flash threshold (seconds)");
		assertConfigItemName("emptyRingColor", "Ring background color");
		assertConfigItemName("outlineThickness", "Ring outline thickness");
		assertConfigItemName("poisonInnerRing", "Next poison-hit ring");
	}

	private static void assertConfigItemName(String methodName, String expected) throws Exception
	{
		assertEquals(
			expected,
			RingOfTimeConfig.class.getMethod(methodName)
				.getAnnotation(ConfigItem.class).name()
		);
	}

}