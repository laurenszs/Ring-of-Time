package com.ringoftime;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.components.ComponentOrientation;

/**
 * Owns ring-group membership, overlay registration, and saved layout metadata.
 */
final class TimerGroupManager
{
	private static final String LAYOUT_CONFIG_GROUP = "ring-of-time-layout";
	private static final String MEMBER_PREFIX = "member_";
	private static final String ORIENTATION_PREFIX = "orientation_";
	private static final int DETACH_OFFSET = 16;

	private final Client client;
	private final RingOfTimePlugin plugin;
	private final RingOfTimeConfig config;
	private final OverlayManager overlayManager;
	private final ConfigManager configManager;

	private final Map<String, TimerGroupOverlay> groups = new LinkedHashMap<>();

	TimerGroupManager(
		Client client,
		RingOfTimePlugin plugin,
		RingOfTimeConfig config,
		OverlayManager overlayManager,
		ConfigManager configManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.overlayManager = overlayManager;
		this.configManager = configManager;
	}

	void start(Collection<? extends TimerCircleOverlay> timers)
	{
		for (TimerCircleOverlay timer : timers)
		{
			final String savedGroup = getSavedGroup(timer);
			final String groupName = savedGroup == null ? timer.getName() : savedGroup;
			TimerGroupOverlay group = groups.get(groupName);
			if (group == null)
			{
				group = createGroup(groupName);
			}
			group.addMember(timer);
		}
	}

	void shutDown()
	{
		for (TimerGroupOverlay group : groups.values())
		{
			overlayManager.remove(group);
		}
		groups.clear();
	}

	boolean merge(TimerGroupOverlay source, TimerGroupOverlay destination)
	{
		if (source == destination
			|| !groups.containsValue(source)
			|| !groups.containsValue(destination))
		{
			return false;
		}

		destination.markManaged();
		for (TimerCircleOverlay timer : source.getMembers())
		{
			source.removeMember(timer);
			destination.addMember(timer);
			saveGroup(timer, destination.getName());
		}

		groups.remove(source.getName());
		overlayManager.remove(source);
		return true;
	}

	void handleMenu(TimerGroupOverlay group, String option)
	{
		if (TimerGroupOverlay.DETACH_OPTION.equals(option))
		{
			detach(group, group.getHoveredTimer());
		}
		else if (TimerGroupOverlay.FLIP_OPTION.equals(option))
		{
			final ComponentOrientation orientation = group.flip();
			configManager.setConfiguration(
				LAYOUT_CONFIG_GROUP,
				ORIENTATION_PREFIX + group.getName(),
				orientation
			);
		}
	}

	List<TimerGroupOverlay> getGroups()
	{
		return new ArrayList<>(groups.values());
	}

	private void detach(TimerGroupOverlay source, TimerCircleOverlay timer)
	{
		if (timer == null || source.getMembers().size() <= 1)
		{
			return;
		}

		source.markManaged();
		final String groupName = groups.containsKey(timer.getName())
			? "Ring of Time: Group " + UUID.randomUUID()
			: timer.getName();
		final TimerGroupOverlay detached = createGroup(groupName);

		source.removeMember(timer);
		detached.addMember(timer);
		saveGroup(timer, groupName);
		configManager.setConfiguration(
			LAYOUT_CONFIG_GROUP,
			ORIENTATION_PREFIX + groupName,
			source.getOrientation()
		);
		if (detached.getOrientation() != source.getOrientation())
		{
			detached.flip();
		}

		final Point sourceLocation = source.getPreferredLocation() != null
			? new Point(source.getPreferredLocation())
			: source.getBounds().getLocation();
		sourceLocation.translate(DETACH_OFFSET, DETACH_OFFSET);
		detached.placeDetachedAt(sourceLocation);
		overlayManager.saveOverlay(detached);
	}

	private TimerGroupOverlay createGroup(String groupName)
	{
		ComponentOrientation orientation = configManager.getConfiguration(
			LAYOUT_CONFIG_GROUP,
			ORIENTATION_PREFIX + groupName,
			ComponentOrientation.class
		);
		if (orientation == null)
		{
			orientation = ComponentOrientation.VERTICAL;
		}

		final TimerGroupOverlay group = new TimerGroupOverlay(
			client,
			plugin,
			config,
			this,
			groupName,
			orientation
		);
		groups.put(groupName, group);
		overlayManager.add(group);
		group.initializeAutomaticLayout();
		return group;
	}

	private String getSavedGroup(TimerCircleOverlay timer)
	{
		final String value = configManager.getConfiguration(
			LAYOUT_CONFIG_GROUP,
			MEMBER_PREFIX + timer.getName()
		);
		return value == null || value.trim().isEmpty() ? null : value;
	}

	private void saveGroup(TimerCircleOverlay timer, String groupName)
	{
		final String key = MEMBER_PREFIX + timer.getName();
		if (timer.getName().equals(groupName))
		{
			configManager.unsetConfiguration(LAYOUT_CONFIG_GROUP, key);
		}
		else
		{
			configManager.setConfiguration(LAYOUT_CONFIG_GROUP, key, groupName);
		}
	}
}
