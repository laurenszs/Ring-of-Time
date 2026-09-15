package com.ringoftime;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.client.config.ConfigManager;

/**
 * Keeps optional Custom UI Anchors data attached when a ring group changes identity.
 */
final class CustomUiAnchorsCompatibility
{
	private static final String CONFIG_GROUP = "anchorcustomizer";
	private static final String ASSIGNMENTS_KEY = "overlayAssignments";
	private static final String ORDER_KEY = "overlayOrder";
	private static final Type INTEGER_MAP_TYPE = new TypeToken<Map<String, Integer>>() { }.getType();

	private final ConfigManager configManager;
	private final Gson gson = new Gson();

	CustomUiAnchorsCompatibility(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	void reconcile(Collection<TimerGroupOverlay> groups)
	{
		final Map<String, Integer> assignments = loadMap(ASSIGNMENTS_KEY);
		final Map<String, Integer> order = loadMap(ORDER_KEY);
		if (assignments == null && order == null)
		{
			return;
		}

		boolean assignmentsChanged = false;
		boolean orderChanged = false;
		for (TimerGroupOverlay group : groups)
		{
			final List<String> aliases = memberNames(group);
			assignmentsChanged |= reconcileNames(assignments, group.getName(), aliases);
			orderChanged |= reconcileNames(order, group.getName(), aliases);
		}

		saveIfChanged(ASSIGNMENTS_KEY, assignments, assignmentsChanged);
		saveIfChanged(ORDER_KEY, order, orderChanged);
	}

	void merge(TimerGroupOverlay source, TimerGroupOverlay destination)
	{
		final List<String> aliases = memberNames(destination);
		aliases.add(source.getName());
		reconcile(destination.getName(), aliases);
	}

	void detach(String detachedName)
	{
		removeName(ASSIGNMENTS_KEY, detachedName);
		removeName(ORDER_KEY, detachedName);
	}

	private void reconcile(String groupName, Collection<String> aliases)
	{
		final Map<String, Integer> assignments = loadMap(ASSIGNMENTS_KEY);
		final Map<String, Integer> order = loadMap(ORDER_KEY);
		saveIfChanged(
			ASSIGNMENTS_KEY,
			assignments,
			reconcileNames(assignments, groupName, aliases)
		);
		saveIfChanged(ORDER_KEY, order, reconcileNames(order, groupName, aliases));
	}

	private void removeName(String key, String name)
	{
		final Map<String, Integer> values = loadMap(key);
		if (values != null && values.remove(name) != null)
		{
			configManager.setConfiguration(CONFIG_GROUP, key, gson.toJson(values));
		}
	}

	private Map<String, Integer> loadMap(String key)
	{
		final String json = configManager.getConfiguration(CONFIG_GROUP, key);
		if (json == null || json.trim().isEmpty())
		{
			return null;
		}

		try
		{
			final Map<String, Integer> values = gson.fromJson(json, INTEGER_MAP_TYPE);
			return values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
		}
		catch (RuntimeException ignored)
		{
			return null;
		}
	}

	private void saveIfChanged(String key, Map<String, Integer> values, boolean changed)
	{
		if (values != null && changed)
		{
			configManager.setConfiguration(CONFIG_GROUP, key, gson.toJson(values));
		}
	}

	private static List<String> memberNames(TimerGroupOverlay group)
	{
		final List<String> names = new ArrayList<>();
		for (TimerCircleOverlay member : group.getMembers())
		{
			names.add(member.getName());
		}
		return names;
	}

	static boolean reconcileNames(
		Map<String, Integer> values,
		String groupName,
		Collection<String> aliases)
	{
		if (values == null)
		{
			return false;
		}

		Integer groupValue = values.get(groupName);
		boolean changed = false;
		for (String alias : aliases)
		{
			if (groupName.equals(alias))
			{
				continue;
			}

			final Integer aliasValue = values.remove(alias);
			if (aliasValue != null)
			{
				changed = true;
				if (groupValue == null)
				{
					groupValue = aliasValue;
				}
			}
		}

		if (groupValue != null && !groupValue.equals(values.get(groupName)))
		{
			values.put(groupName, groupValue);
			changed = true;
		}
		return changed;
	}
}
