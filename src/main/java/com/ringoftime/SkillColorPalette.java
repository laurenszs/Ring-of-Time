/*
 * Copyright (c) 2026, Ring of Time contributors
 * All rights reserved.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.ringoftime;

import java.awt.Color;
import net.runelite.api.Skill;

final class SkillColorPalette
{
	private SkillColorPalette()
	{
		// Utility class.
	}

	/**
	 * Returns the themed color associated with one skill.
	 *
	 * @param skill skill whose buff ring is being rendered
	 * @return a high-contrast color inspired by that skill's icon or activity
	 */
	static Color getDefaultColor(Skill skill)
	{
		switch (skill)
		{
			case ATTACK:
				return new Color(220, 60, 55);
			case STRENGTH:
				return new Color(178, 42, 72);
			case DEFENCE:
				return new Color(105, 145, 190);
			case RANGED:
				return new Color(83, 155, 75);
			case MAGIC:
				return new Color(80, 120, 235);
			case MINING:
				return new Color(117, 129, 145);
			case AGILITY:
				return new Color(70, 190, 170);
			case SMITHING:
				return new Color(200, 110, 50);
			case HERBLORE:
				return new Color(74, 170, 90);
			case FISHING:
				return new Color(50, 160, 210);
			case THIEVING:
				return new Color(145, 85, 180);
			case COOKING:
				return new Color(215, 80, 70);
			case CRAFTING:
				return new Color(190, 145, 80);
			case FIREMAKING:
				return new Color(245, 125, 35);
			case FLETCHING:
				return new Color(125, 190, 70);
			case WOODCUTTING:
				return new Color(120, 155, 60);
			case RUNECRAFT:
				return new Color(145, 90, 210);
			case SLAYER:
				return new Color(110, 80, 125);
			case FARMING:
				return new Color(95, 180, 65);
			case CONSTRUCTION:
				return new Color(190, 145, 55);
			case HUNTER:
				return new Color(160, 130, 75);
			case SAILING:
				return new Color(45, 145, 190);
			default:
				/*
				 * This fallback is only defensive: callers render the 22 skills
				 * explicitly supported by RingOfTimePlugin.
				 */
				return Color.WHITE;
		}	}

	/**
	 * Returns the user-configurable color for one positive skill buff.
	 *
	 * @param skill skill whose buff ring is being rendered
	 * @param config active Ring of Time configuration
	 * @return the configured color for the supplied skill
	 */
	static Color getColor(Skill skill, RingOfTimeConfig config)
	{
		switch (skill)
		{
			case ATTACK: return config.attackColor();
			case STRENGTH: return config.strengthColor();
			case DEFENCE: return config.defenceColor();
			case RANGED: return config.rangedColor();
			case MAGIC: return config.magicColor();
			case MINING: return config.miningColor();
			case AGILITY: return config.agilityColor();
			case SMITHING: return config.smithingColor();
			case HERBLORE: return config.herbloreColor();
			case FISHING: return config.fishingColor();
			case THIEVING: return config.thievingColor();
			case COOKING: return config.cookingColor();
			case CRAFTING: return config.craftingColor();
			case FIREMAKING: return config.firemakingColor();
			case FLETCHING: return config.fletchingColor();
			case WOODCUTTING: return config.woodcuttingColor();
			case RUNECRAFT: return config.runecraftColor();
			case SLAYER: return config.slayerColor();
			case FARMING: return config.farmingColor();
			case CONSTRUCTION: return config.constructionColor();
			case HUNTER: return config.hunterColor();
			case SAILING: return config.sailingColor();
			default: return Color.WHITE;
		}
	}
}