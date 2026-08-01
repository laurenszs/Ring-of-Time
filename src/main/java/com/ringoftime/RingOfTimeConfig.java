package com.ringoftime;

import java.awt.Color;
import net.runelite.api.Skill;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * Declares every user-facing setting shown in RuneLite's configuration panel.
 *
 * <p>Buffs, debuffs, and non-skill effects have separate sections so users can
 * tune each timer family without changing the others.</p>
 */
@ConfigGroup(RingOfTimeConfig.GROUP)
public interface RingOfTimeConfig extends Config
{
	String GROUP = "ring-of-time";

	/**
	 * Selects which category of restorable skill changes appears in the overlay.
	 */
	enum TrackedSkills
	{
		COMBAT("Combat"),
		NON_COMBAT("Non-combat"),
		ALL("All");

		private final String displayName;

		TrackedSkills(String displayName)
		{
			this.displayName = displayName;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	/**
	 * Selects which kinds of harmful timers appear.
	 */
	enum DebuffDisplay
	{
		SKILLS("Skills"),
		POISON_VENOM("Poison/venom"),
		BOTH("Both"),
		OFF("Off");

		private final String displayName;

		DebuffDisplay(String displayName)
		{
			this.displayName = displayName;
		}

		boolean showsSkills()
		{
			return this == SKILLS || this == BOTH;
		}

		boolean showsPoisonVenom()
		{
			return this == POISON_VENOM || this == BOTH;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}
	/**
	 * Controls where the countdown appears inside the ring.
	 */
	enum TimerPosition
	{
		TOP("Top"),
		MIDDLE("Middle"),
		BOTTOM("Bottom"),
		OFF("Off");

		private final String displayName;

		TimerPosition(String displayName)
		{
			this.displayName = displayName;
		}

		boolean isShown()
		{
			return this != OFF;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	/**
	 * Controls where the skill or effect icon appears around its ring.
	 */
	enum IconPosition
	{
		TOP_LEFT("Top left"),
		TOP_RIGHT("Top right"),
		BOTTOM_RIGHT("Bottom right"),
		BOTTOM_LEFT("Bottom left"),
		CENTER("Center of circle"),
		ABOVE_RING("Above ring"),
		UNDER_RING("Under ring");

		private final String displayName;

		IconPosition(String displayName)
		{
			this.displayName = displayName;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	/**
	 * Provides an explicit on/off dropdown for one inner-ring category.
	 */
	enum InnerRingDisplay
	{
		SHOW("On"),
		HIDE("Off");

		private final String displayName;

		InnerRingDisplay(String displayName)
		{
			this.displayName = displayName;
		}

		boolean isShown()
		{
			return this == SHOW;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	/**
	 * Controls the number shown in a buff or debuff +/- badge.
	 */
	enum LevelDisplay
	{
		CHANGE("Change (+/-)"),
		TOTAL_LEVEL("Total level");

		private final String displayName;

		LevelDisplay(String displayName)
		{
			this.displayName = displayName;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	/**
	 * Controls where the signed level-change badge appears.
	 */
	enum PlusMinusPosition
	{
		TOP_LEFT("Top left"),
		TOP_RIGHT("Top right"),
		BOTTOM_RIGHT("Bottom right"),
		BOTTOM_LEFT("Bottom left"),
		CENTER("Center of circle"),
		OFF("Off");

		private final String displayName;

		PlusMinusPosition(String displayName)
		{
			this.displayName = displayName;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	@ConfigSection(
		name = "Duplicate timers",
		description = "RuneLite indicators that can overlap Ring of Time.",
		position = 0,
		closedByDefault = false
	)
	String DUPLICATE_TIMERS_SECTION = "duplicateTimers";

	@ConfigSection(
		name = "Labels & icons",
		description = "Countdown, +/- text, and icon placement settings.",
		position = 1,
		closedByDefault = false
	)
	String LABELS_SECTION = "labels";

	@ConfigSection(
		name = "Ring style",
		description = "Shared ring dimensions, background, and outline settings.",
		position = 2,
		closedByDefault = false
	)
	String RING_STYLE_SECTION = "ringStyle";

	@ConfigSection(
		name = "Buffs",
		description = "Settings for levels above their base value.",
		position = 3,
		closedByDefault = false
	)
	String BUFFS_SECTION = "buffs";

	@ConfigSection(
		name = "Individual buff colors",
		description = "Individual colors used by positive skill buffs.",
		position = 4,
		closedByDefault = true
	)
	String BUFF_COLORS_SECTION = "buffColors";

	@ConfigSection(
		name = "Debuffs",
		description = "Skill drains plus poison and venom timers.",
		position = 5,
		closedByDefault = false
	)
	String DEBUFFS_SECTION = "debuffs";

	@ConfigSection(
		name = "Protection & effect timers",
		description = "Antipoison, anti-venom, stamina, and antifire settings.",
		position = 6,
		closedByDefault = false
	)
	String EFFECTS_SECTION = "effects";

	@ConfigItem(
		keyName = "duplicateSkillIndicatorNotice",
		name = "<html>Boosts Information:<br>"
			+ "- Set Next buff/debuff change to Never<br>"
			+ "- Disable its infoboxes or panel if desired</html>",
		description = "Informational only; Ring of Time does not change other plugin settings.",
		position = 0,
		section = DUPLICATE_TIMERS_SECTION
	)
	default void duplicateSkillIndicatorNotice()
	{
	}

	@ConfigItem(
		keyName = "duplicateEffectIndicatorNotice",
		name = "<html>Timers & Buffs:<br>"
			+ "- Disable Antipoison, Stamina, and Antifire<br>"
			+ "- Poison: disable Show infoboxes</html>",
		description = "Informational only; Ring of Time does not change other plugin settings.",
		position = 1,
		section = DUPLICATE_TIMERS_SECTION
	)
	default void duplicateEffectIndicatorNotice()
	{
	}

	@ConfigItem(
		keyName = "trackedSkills",
		name = "Shown buffs",
		description = "Choose which positive temporary skill changes to display.",
		position = 0,
		section = BUFFS_SECTION
	)
	default TrackedSkills trackedSkills()
	{
		return TrackedSkills.ALL;
	}

	@ConfigItem(
		keyName = "timerPosition",
		name = "Timer position",
		description = "Place the countdown at the top, middle, or bottom of the ring, or hide it.",
		position = 0,
		section = LABELS_SECTION
	)
	default TimerPosition timerPosition()
	{
		return TimerPosition.MIDDLE;
	}

	@ConfigItem(
		keyName = "iconPosition",
		name = "Icon position",
		description = "Place the skill or effect icon on a corner, in the center, above, or under the ring.",
		position = 1,
		section = LABELS_SECTION
	)
	default IconPosition iconPosition()
	{
		return IconPosition.TOP_LEFT;
	}

	@Range(min = 10, max = 24)
	@ConfigItem(
		keyName = "timerFontSize",
		name = "Timer font size",
		description = "Maximum countdown-text size in pixels.",
		position = 2,
		section = LABELS_SECTION
	)
	default int timerFontSize()
	{
		return 10;
	}

	@Range(min = 10, max = 24)
	@ConfigItem(
		keyName = "plusMinusFontSize",
		name = "+/- font size",
		description = "Size of the signed level-change text in pixels.",
		position = 3,
		section = LABELS_SECTION
	)
	default int plusMinusFontSize()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "buffInnerRing",
		name = "Next-change ring",
		description = "Show or hide the next one-level decay ring for buffs.",
		position = 1,
		section = BUFFS_SECTION
	)
	default InnerRingDisplay buffInnerRing()
	{
		return InnerRingDisplay.SHOW;
	}

	@Alpha
	@ConfigItem(
		keyName = "buffInnerRingColor",
		name = "Next-change color",
		description = "Yellow color of the next one-level buff-decay ring.",
		position = 2,
		section = BUFFS_SECTION
	)
	default Color buffInnerRingColor()
	{
		return new Color(255, 214, 64, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "buffOutlineColor",
		name = "Outline color",
		description = "Outline color used by positive skill buffs.",
		position = 3,
		section = BUFFS_SECTION
	)
	default Color buffOutlineColor()
	{
		return new Color(0, 0, 0, 220);
	}

	@ConfigItem(
		keyName = "buffLevelDisplay",
		name = "+/-",
		description = "Show the positive change or total effective level in the +/- badge.",
		position = 4,
		section = BUFFS_SECTION
	)
	default LevelDisplay buffLevelDisplay()
	{
		return LevelDisplay.CHANGE;
	}

	@ConfigItem(
		keyName = "buffPlusMinusPosition",
		name = "+/- position",
		description = "Choose the position for buff +/- text, or hide the +/- badge.",
		position = 5,
		section = BUFFS_SECTION
	)
	default PlusMinusPosition buffPlusMinusPosition()
	{
		return PlusMinusPosition.TOP_RIGHT;
	}

	@Alpha
	@ConfigItem(
		keyName = "buffPlusMinusColor",
		name = "+/- color",
		description = "Green text color used for buff +/- values.",
		position = 6,
		section = BUFFS_SECTION
	)
	default Color buffPlusMinusColor()
	{
		return new Color(92, 235, 112, 255);
	}

	@ConfigItem(
		keyName = "useBaseBuffColor",
		name = "Use one buff color",
		description = "Override the individual skill-themed buff colors with the shared color below.",
		position = 7,
		section = BUFFS_SECTION
	)
	default boolean useBaseBuffColor()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "buffColor",
		name = "Shared buff color",
		description = "Shared buff color used when Use one buff color is enabled.",
		position = 8,
		section = BUFFS_SECTION
	)
	default Color buffColor()
	{
		return new Color(82, 214, 110, 255);
	}

	@ConfigItem(
		keyName = "flashExpiringBuffs",
		name = "Flash near expiry",
		description = "Flash beneficial skill and protection circles in their configured color near expiry.",
		position = 9,
		section = BUFFS_SECTION
	)
	default boolean flashExpiringBuffs()
	{
		return true;
	}

	@Range(min = 1, max = 30)
	@ConfigItem(
		keyName = "flashThresholdSeconds",
		name = "Flash threshold (seconds)",
		description = "Seconds remaining when beneficial circles begin flashing.",
		position = 10,
		section = BUFFS_SECTION
	)
	default int flashThresholdSeconds()
	{
		return 6;
	}

	@Alpha
	@ConfigItem(
		keyName = "attackColor",
		name = "Attack",
		description = "Color used by Attack buffs.",
		position = 0,
		section = BUFF_COLORS_SECTION
	)
	default Color attackColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.ATTACK);
	}

	@Alpha
	@ConfigItem(
		keyName = "strengthColor",
		name = "Strength",
		description = "Color used by Strength buffs.",
		position = 1,
		section = BUFF_COLORS_SECTION
	)
	default Color strengthColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.STRENGTH);
	}

	@Alpha
	@ConfigItem(
		keyName = "defenceColor",
		name = "Defence",
		description = "Color used by Defence buffs.",
		position = 2,
		section = BUFF_COLORS_SECTION
	)
	default Color defenceColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.DEFENCE);
	}

	@Alpha
	@ConfigItem(
		keyName = "rangedColor",
		name = "Ranged",
		description = "Color used by Ranged buffs.",
		position = 3,
		section = BUFF_COLORS_SECTION
	)
	default Color rangedColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.RANGED);
	}

	@Alpha
	@ConfigItem(
		keyName = "magicColor",
		name = "Magic",
		description = "Color used by Magic buffs.",
		position = 4,
		section = BUFF_COLORS_SECTION
	)
	default Color magicColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.MAGIC);
	}

	@Alpha
	@ConfigItem(
		keyName = "miningColor",
		name = "Mining",
		description = "Color used by Mining buffs.",
		position = 5,
		section = BUFF_COLORS_SECTION
	)
	default Color miningColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.MINING);
	}

	@Alpha
	@ConfigItem(
		keyName = "agilityColor",
		name = "Agility",
		description = "Color used by Agility buffs.",
		position = 6,
		section = BUFF_COLORS_SECTION
	)
	default Color agilityColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.AGILITY);
	}

	@Alpha
	@ConfigItem(
		keyName = "smithingColor",
		name = "Smithing",
		description = "Color used by Smithing buffs.",
		position = 7,
		section = BUFF_COLORS_SECTION
	)
	default Color smithingColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.SMITHING);
	}

	@Alpha
	@ConfigItem(
		keyName = "herbloreColor",
		name = "Herblore",
		description = "Color used by Herblore buffs.",
		position = 8,
		section = BUFF_COLORS_SECTION
	)
	default Color herbloreColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.HERBLORE);
	}

	@Alpha
	@ConfigItem(
		keyName = "fishingColor",
		name = "Fishing",
		description = "Color used by Fishing buffs.",
		position = 9,
		section = BUFF_COLORS_SECTION
	)
	default Color fishingColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.FISHING);
	}

	@Alpha
	@ConfigItem(
		keyName = "thievingColor",
		name = "Thieving",
		description = "Color used by Thieving buffs.",
		position = 10,
		section = BUFF_COLORS_SECTION
	)
	default Color thievingColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.THIEVING);
	}

	@Alpha
	@ConfigItem(
		keyName = "cookingColor",
		name = "Cooking",
		description = "Color used by Cooking buffs.",
		position = 11,
		section = BUFF_COLORS_SECTION
	)
	default Color cookingColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.COOKING);
	}

	@Alpha
	@ConfigItem(
		keyName = "craftingColor",
		name = "Crafting",
		description = "Color used by Crafting buffs.",
		position = 12,
		section = BUFF_COLORS_SECTION
	)
	default Color craftingColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.CRAFTING);
	}

	@Alpha
	@ConfigItem(
		keyName = "firemakingColor",
		name = "Firemaking",
		description = "Color used by Firemaking buffs.",
		position = 13,
		section = BUFF_COLORS_SECTION
	)
	default Color firemakingColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.FIREMAKING);
	}

	@Alpha
	@ConfigItem(
		keyName = "fletchingColor",
		name = "Fletching",
		description = "Color used by Fletching buffs.",
		position = 14,
		section = BUFF_COLORS_SECTION
	)
	default Color fletchingColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.FLETCHING);
	}

	@Alpha
	@ConfigItem(
		keyName = "woodcuttingColor",
		name = "Woodcutting",
		description = "Color used by Woodcutting buffs.",
		position = 15,
		section = BUFF_COLORS_SECTION
	)
	default Color woodcuttingColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.WOODCUTTING);
	}

	@Alpha
	@ConfigItem(
		keyName = "runecraftColor",
		name = "Runecraft",
		description = "Color used by Runecraft buffs.",
		position = 16,
		section = BUFF_COLORS_SECTION
	)
	default Color runecraftColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.RUNECRAFT);
	}

	@Alpha
	@ConfigItem(
		keyName = "slayerColor",
		name = "Slayer",
		description = "Color used by Slayer buffs.",
		position = 17,
		section = BUFF_COLORS_SECTION
	)
	default Color slayerColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.SLAYER);
	}

	@Alpha
	@ConfigItem(
		keyName = "farmingColor",
		name = "Farming",
		description = "Color used by Farming buffs.",
		position = 18,
		section = BUFF_COLORS_SECTION
	)
	default Color farmingColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.FARMING);
	}

	@Alpha
	@ConfigItem(
		keyName = "constructionColor",
		name = "Construction",
		description = "Color used by Construction buffs.",
		position = 19,
		section = BUFF_COLORS_SECTION
	)
	default Color constructionColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.CONSTRUCTION);
	}

	@Alpha
	@ConfigItem(
		keyName = "hunterColor",
		name = "Hunter",
		description = "Color used by Hunter buffs.",
		position = 20,
		section = BUFF_COLORS_SECTION
	)
	default Color hunterColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.HUNTER);
	}

	@Alpha
	@ConfigItem(
		keyName = "sailingColor",
		name = "Sailing",
		description = "Color used by Sailing buffs.",
		position = 21,
		section = BUFF_COLORS_SECTION
	)
	default Color sailingColor()
	{
		return SkillColorPalette.getDefaultColor(Skill.SAILING);
	}

	@ConfigItem(
		keyName = "debuffDisplay",
		name = "Shown debuffs",
		description = "Choose skill drains, poison and venom, both groups, or no debuff timers.",
		position = 0,
		section = DEBUFFS_SECTION
	)
	default DebuffDisplay debuffDisplay()
	{
		return DebuffDisplay.BOTH;
	}
	@ConfigItem(
		keyName = "debuffInnerRing",
		name = "Next-change ring",
		description = "Show or hide the next one-level recovery ring for debuffs.",
		position = 1,
		section = DEBUFFS_SECTION
	)
	default InnerRingDisplay debuffInnerRing()
	{
		return InnerRingDisplay.SHOW;
	}

	@Alpha
	@ConfigItem(
		keyName = "debuffInnerRingColor",
		name = "Next-change color",
		description = "Red color of the next one-level debuff-recovery ring.",
		position = 2,
		section = DEBUFFS_SECTION
	)
	default Color debuffInnerRingColor()
	{
		return new Color(238, 74, 74, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "debuffOutlineColor",
		name = "Outline color",
		description = "Outline color shared by skill debuffs, poison, and venom.",
		position = 3,
		section = DEBUFFS_SECTION
	)
	default Color debuffOutlineColor()
	{
		return new Color(0, 0, 0, 220);
	}

	@ConfigItem(
		keyName = "debuffLevelDisplay",
		name = "+/-",
		description = "Show the negative change or total effective level in the +/- badge.",
		position = 4,
		section = DEBUFFS_SECTION
	)
	default LevelDisplay debuffLevelDisplay()
	{
		return LevelDisplay.CHANGE;
	}

	@ConfigItem(
		keyName = "debuffPlusMinusPosition",
		name = "+/- position",
		description = "Choose the position for debuff and toxin +/- text, or hide it.",
		position = 5,
		section = DEBUFFS_SECTION
	)
	default PlusMinusPosition debuffPlusMinusPosition()
	{
		return PlusMinusPosition.TOP_RIGHT;
	}

	@Alpha
	@ConfigItem(
		keyName = "debuffPlusMinusColor",
		name = "+/- color",
		description = "Red text color used for skill-debuff and toxin +/- values.",
		position = 6,
		section = DEBUFFS_SECTION
	)
	default Color debuffPlusMinusColor()
	{
		return new Color(255, 92, 92, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "debuffColor",
		name = "Skill debuff color",
		description = "Color used for skills below their base level.",
		position = 7,
		section = DEBUFFS_SECTION
	)
	default Color debuffColor()
	{
		return new Color(238, 74, 74, 255);
	}

	@ConfigItem(
		keyName = "showStamina",
		name = "Stamina",
		description = "Show the remaining stamina-effect circle.",
		position = 3,
		section = EFFECTS_SECTION
	)
	default boolean showStamina()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showAntifire",
		name = "Antifire",
		description = "Show regular, extended, super, and extended-super antifire circles.",
		position = 5,
		section = EFFECTS_SECTION
	)
	default boolean showAntifire()
	{
		return true;
	}

	@ConfigItem(
		keyName = "poisonInnerRing",
		name = "Next poison-hit ring",
		description = "Show the next poison-damage cycle inside its total natural-cure ring.",
		position = 9,
		section = DEBUFFS_SECTION
	)
	default InnerRingDisplay poisonInnerRing()
	{
		return InnerRingDisplay.SHOW;
	}

	@Alpha
	@ConfigItem(
		keyName = "poisonColor",
		name = "Poison color",
		description = "Color used for active poison and beneath active venom.",
		position = 10,
		section = DEBUFFS_SECTION
	)
	default Color poisonColor()
	{
		return new Color(52, 190, 78, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "venomColor",
		name = "Venom color",
		description = "Dark green color used for active venom.",
		position = 11,
		section = DEBUFFS_SECTION
	)
	default Color venomColor()
	{
		return new Color(10, 92, 40, 255);
	}

	@ConfigItem(
		keyName = "showAntipoison",
		name = "Antipoison protection",
		description = "Show antipoison and anti-venom protection in one independent circle.",
		position = 0,
		section = EFFECTS_SECTION
	)
	default boolean showAntipoison()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "antipoisonColor",
		name = "Antipoison color",
		description = "Color used while ordinary antipoison protection is active.",
		position = 1,
		section = EFFECTS_SECTION
	)
	default Color antipoisonColor()
	{
		return new Color(82, 205, 105, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "antivenomProtectionColor",
		name = "Anti-venom color",
		description = "Color used while anti-venom protection is active.",
		position = 2,
		section = EFFECTS_SECTION
	)
	default Color antivenomProtectionColor()
	{
		return new Color(25, 125, 68, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "staminaColor",
		name = "Stamina color",
		description = "Brown color used for the stamina effect.",
		position = 4,
		section = EFFECTS_SECTION
	)
	default Color staminaColor()
	{
		return new Color(148, 96, 52, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "antifireColor",
		name = "Antifire color",
		description = "Light purple color used for regular and super antifire.",
		position = 6,
		section = EFFECTS_SECTION
	)
	default Color antifireColor()
	{
		return new Color(204, 156, 255, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "effectOutlineColor",
		name = "Outline color",
		description = "Yellow outline used by protection, stamina, and antifire circles.",
		position = 7,
		section = EFFECTS_SECTION
	)
	default Color effectOutlineColor()
	{
		return new Color(255, 214, 64, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "toxinInnerRingColor",
		name = "Next poison-hit color",
		description = "Color of poison's next-damage inner ring.",
		position = 12,
		section = DEBUFFS_SECTION
	)
	default Color toxinInnerRingColor()
	{
		return new Color(255, 214, 64, 255);
	}

	@Range(min = 32, max = 96)
	@ConfigItem(
		keyName = "ringSize",
		name = "Ring size",
		description = "Diameter of each independent annular timer in pixels.",
		position = 0,
		section = RING_STYLE_SECTION
	)
	default int ringSize()
	{
		return 50;
	}

	@Range(min = 2, max = 14)
	@ConfigItem(
		keyName = "ringThickness",
		name = "Ring thickness",
		description = "Width of the outer annular timer stroke in pixels.",
		position = 1,
		section = RING_STYLE_SECTION
	)
	default int ringThickness()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "showRingOutline",
		name = "Ring outlines",
		description = "Draw category-colored outlines around outer and inner timer rings.",
		position = 2,
		section = RING_STYLE_SECTION
	)
	default boolean showRingOutline()
	{
		return false;
	}

	@Range(min = 1, max = 6)
	@ConfigItem(
		keyName = "outlineThickness",
		name = "Ring outline thickness",
		description = "Width of the colored outline extending around each timer stroke.",
		position = 3,
		section = RING_STYLE_SECTION
	)
	default int outlineThickness()
	{
		return 1;
	}

	@Alpha
	@ConfigItem(
		keyName = "emptyRingColor",
		name = "Ring background color",
		description = "Background color drawn behind the remaining portion of every timer.",
		position = 4,
		section = RING_STYLE_SECTION
	)
	default Color emptyRingColor()
	{
		return new Color(20, 20, 20, 165);
	}

	@Alpha
	@ConfigItem(
		keyName = "plusMinusBackgroundColor",
		name = "+/- background",
		description = "Adjustable translucent background behind +/- text.",
		position = 4,
		section = LABELS_SECTION
	)
	default Color plusMinusBackgroundColor()
	{
		return new Color(0, 0, 0, 50);
	}
}