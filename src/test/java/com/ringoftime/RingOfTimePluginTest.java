package com.ringoftime;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Developer-only entry point that launches RuneLite with Ring of Time loaded.
 */
public final class RingOfTimePluginTest
{
	private RingOfTimePluginTest()
	{
		// This utility class only exposes its static main method.
	}

	/**
	 * Loads the plugin as a built-in development plugin and starts RuneLite.
	 */
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RingOfTimePlugin.class);
		RuneLite.main(args);
	}
}
