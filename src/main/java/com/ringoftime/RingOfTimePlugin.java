package com.ringoftime;

import com.google.inject.Provides;
import com.ringoftime.EffectTimerTracker.Effect;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameState;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Connects RuneLite events and client state to the timer models and overlays.
 *
	 * <p>Each timer remains an independent renderable entity. Resizable group
	 * overlays provide optional wrapping while retaining stable names for saved
	 * positions and detached rings.</p>
 */
@PluginDescriptor(
	name = "Ring of Time",
	description = "Displays shrinking annular timers for skill changes and timed effects",
	tags = {
		"timer", "visual", "boost", "buff", "debuff", "poison",
		"venom", "antipoison", "stamina", "antifire", "divine", "overlay"
	}
)
public class RingOfTimePlugin extends Plugin
{
	static final String CURRENT_VERSION = "1.3.0";
	static final String UPDATE_MESSAGE = "<col=ff981f>Ring of Time v1.3.0:</col> "
		+ "Ring order within groups can now be customized with Ctrl+Alt-drag.";
	static final long UPDATE_MESSAGE_DELAY_SECONDS = 3L;
	private static final String LAST_SEEN_UPDATE_VERSION_KEY = "lastSeenUpdateVersion";
	private static final String RESURRECT_THRALL_MESSAGE_START = ">You resurrect a ";
	private static final String RESURRECT_THRALL_MESSAGE_END = " thrall.</col>";
	/*
	 * Hitpoints and Prayer use different mechanics, so this plugin follows the
	 * same boostable-skill categories as RuneLite's core Boosts Information
	 * plugin. Sailing is included for current RuneLite releases.
	 */
	static final Set<Skill> COMBAT_SKILLS = Collections.unmodifiableSet(EnumSet.of(
		Skill.ATTACK,
		Skill.STRENGTH,
		Skill.DEFENCE,
		Skill.RANGED,
		Skill.MAGIC
	));

	static final Set<Skill> NON_COMBAT_SKILLS = Collections.unmodifiableSet(EnumSet.of(
		Skill.MINING,
		Skill.AGILITY,
		Skill.SMITHING,
		Skill.HERBLORE,
		Skill.FISHING,
		Skill.THIEVING,
		Skill.COOKING,
		Skill.CRAFTING,
		Skill.FIREMAKING,
		Skill.FLETCHING,
		Skill.WOODCUTTING,
		Skill.RUNECRAFT,
		Skill.SLAYER,
		Skill.FARMING,
		Skill.CONSTRUCTION,
		Skill.HUNTER,
		Skill.SAILING
	));

	private static final Set<Skill> TRACKABLE_SKILLS;

	static
	{
		final EnumSet<Skill> skills = EnumSet.copyOf(COMBAT_SKILLS);
		skills.addAll(NON_COMBAT_SKILLS);
		TRACKABLE_SKILLS = Collections.unmodifiableSet(skills);
	}

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private RingOfTimeConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private SkillIconManager skillIconManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private ScheduledExecutorService executor;

	private final StatChangeTracker tracker = new StatChangeTracker();
	private final DivineTimerTracker divineTracker = new DivineTimerTracker();
	private final EffectTimerTracker effectTracker = new EffectTimerTracker();
	private final Map<Skill, SkillTimerOverlay> skillOverlays = new EnumMap<>(Skill.class);
	private final Map<Effect, EffectTimerOverlay> effectOverlays = new EnumMap<>(Effect.class);
	private TimerGroupManager groupManager;
	private TimerGroupReorderInput groupReorderInput;
	private ScheduledFuture<?> updateMessageFuture;
	private long lastGameTickMillis;

	/**
	 * Registers every independently draggable circle and samples active state.
	 */
	@Override
	protected void startUp()
	{
		tracker.reset();
		divineTracker.reset();
		effectTracker.reset();

		for (Skill skill : TRACKABLE_SKILLS)
		{
			final SkillTimerOverlay overlay = new SkillTimerOverlay(
				client,
				this,
				config,
				skillIconManager,
				skill
			);
			skillOverlays.put(skill, overlay);
		}

		for (Effect effect : Effect.values())
		{
			final EffectTimerOverlay overlay = new EffectTimerOverlay(
				client,
				this,
				config,
				itemManager,
				spriteManager,
				effect
			);
			effectOverlays.put(effect, overlay);
		}

		final List<TimerCircleOverlay> timers = new ArrayList<>();
		timers.addAll(skillOverlays.values());
		timers.addAll(effectOverlays.values());
		groupManager = new TimerGroupManager(
			client,
			this,
			config,
			overlayManager,
			configManager
		);
		groupManager.start(timers);
		groupReorderInput = new TimerGroupReorderInput(client, groupManager);
		mouseManager.registerMouseListener(0, groupReorderInput);

		/*
		 * The settings-panel toggle starts plugins on Swing's event thread.
		 * RuneLite requires varbit and skill reads on its client thread, so the
		 * initial sample is scheduled there instead of running inline.
		 */
		clientThread.invokeLater(() ->
		{
			if (!skillOverlays.isEmpty() && client.getGameState() == GameState.LOGGED_IN)
			{
				scheduleUpdateMessage();
				observeDivineState();
				observeAllSkills();
				observeEffectState(false);
				layoutActiveOverlays();
			}
		});
	}

	/**
	 * Removes every independent overlay and forgets timers on disable.
	 */
	@Override
	protected void shutDown()
	{
		cancelUpdateMessage();

		if (groupReorderInput != null)
		{
			mouseManager.unregisterMouseListener(groupReorderInput);
			groupReorderInput = null;
		}

		if (groupManager != null)
		{
			groupManager.shutDown();
			groupManager = null;
		}

		skillOverlays.clear();
		effectOverlays.clear();

		tracker.reset();
		divineTracker.reset();
		effectTracker.reset();
		lastGameTickMillis = 0L;
	}

	/**
	 * Learns exact clock phases as RuneLite reports individual stat changes.
	 */
	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		final Skill skill = event.getSkill();
		if (!TRACKABLE_SKILLS.contains(skill))
		{
			return;
		}

		observeDivineState();
		tracker.observe(
			skill,
			client.getBoostedSkillLevel(skill),
			client.getRealSkillLevel(skill),
			client.getTickCount(),
			divineTracker.isActive(skill)
		);
		layoutActiveOverlays();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!isThrallSummonMessage(event.getType(), event.getMessage()))
		{
			return;
		}

		effectTracker.startThrall(
			client.getBoostedSkillLevel(Skill.MAGIC),
			client.getVarbitValue(VarbitID.CA_TIER_STATUS_MASTER) == 2,
			client.getTickCount()
		);
		layoutActiveOverlays();
	}

	static boolean isThrallSummonMessage(ChatMessageType type, String message)
	{
		return (type == ChatMessageType.SPAM || type == ChatMessageType.GAMEMESSAGE)
			&& message.contains(RESURRECT_THRALL_MESSAGE_START)
			&& message.endsWith(RESURRECT_THRALL_MESSAGE_END);
	}

	/**
	 * Anchors coarse effect variables when their authoritative values change.
	 */
	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		final int currentTick = client.getTickCount();
		boolean relevantEffectChanged = false;
		if (event.getVarpId() == VarPlayerID.POISON)
		{
			/*
			 * Positive values are active poison/venom; negative values are
			 * antipoison/anti-venom protection. Both models share this event.
			 */
			effectTracker.observePoison(event.getValue(), currentTick, true);
			effectTracker.observeAntipoison(event.getValue(), currentTick, true);
			relevantEffectChanged = true;
		}

		if (event.getVarbitId() == VarbitID.ARCEUUS_RESURRECTION_ACTIVE)
		{
			effectTracker.observeThrallActive(event.getValue() != 0);
			relevantEffectChanged = true;
		}

		if (event.getVarbitId() == VarbitID.ARCEUUS_RESURRECTION_COOLDOWN)
		{
			effectTracker.observeThrallCooldown(event.getValue() != 0, currentTick);
			relevantEffectChanged = true;
		}

		if (event.getVarbitId() == VarbitID.PRAYER_REGENERATION_POTION_TIMER)
		{
			effectTracker.observePrayerRegeneration(event.getValue(), currentTick, true);
			relevantEffectChanged = true;
		}

		if (event.getVarbitId() == VarbitID.STAMINA_ACTIVE
			|| event.getVarbitId() == VarbitID.STAMINA_DURATION)
		{
			observeStamina(currentTick, true);
			relevantEffectChanged = true;
		}

		if (event.getVarbitId() == VarbitID.ANTIFIRE_POTION
			|| event.getVarbitId() == VarbitID.SUPER_ANTIFIRE_POTION)
		{
			observeAntifire(currentTick, true);
			relevantEffectChanged = true;
		}

		if (isDivineVarbit(event.getVarbitId()))
		{
			observeDivineState();
			relevantEffectChanged = true;
		}
		if (relevantEffectChanged)
		{
			layoutActiveOverlays();
		}
	}

	/**
	 * Refreshes every model once per game tick for resilient smooth animation.
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		/*
		 * Sampling state is inexpensive and makes the model resilient if another
		 * plugin or a login transition hides an individual change event.
		 */
		observeDivineState();
		observeAllSkills();
		observeEffectState(false);
		tracker.onGameTick(client.getTickCount(), client.getVarbitValue(Prayer.PRESERVE.getVarbit()) == 1);
		lastGameTickMillis = System.currentTimeMillis();
		layoutActiveOverlays();
	}

	/**
	 * Prevents clock phases from leaking across logout and world-hop boundaries.
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGIN_SCREEN:
			case HOPPING:
				cancelUpdateMessage();
				tracker.reset();
				effectTracker.reset();
				lastGameTickMillis = 0L;
				divineTracker.reset();
				break;
			case LOGGED_IN:
				scheduleUpdateMessage();
				observeDivineState();
				observeAllSkills();
				observeEffectState(false);
				lastGameTickMillis = System.currentTimeMillis();
				layoutActiveOverlays();
				break;
			default:
				// Other transient states do not invalidate the current clocks.
				break;
		}
	}

	/**
	 * Reflows untouched circles immediately when a Ring of Time setting
	 * changes. Manually positioned entities ignore this automatic layout.
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (groupManager != null && "runelite".equals(event.getGroup()))
		{
			groupManager.handleOverlayConfigChanged(event.getKey(), event.getNewValue());
		}

		if (RingOfTimeConfig.GROUP.equals(event.getGroup()))
		{
			layoutActiveOverlays();
		}
	}

	@Subscribe
	public void onOverlayMenuClicked(OverlayMenuClicked event)
	{
		if (groupManager != null && event.getOverlay() instanceof TimerGroupOverlay)
		{
			groupManager.handleMenu(
				(TimerGroupOverlay) event.getOverlay(),
				event.getEntry().getOption()
			);
		}
	}


	@Provides
	RingOfTimeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RingOfTimeConfig.class);
	}

	static boolean shouldShowUpdateMessage(String lastSeenVersion)
	{
		return !CURRENT_VERSION.equals(lastSeenVersion);
	}

	private void showUpdateMessageIfNeeded()
	{
		final String lastSeenVersion = configManager.getConfiguration(
			RingOfTimeConfig.GROUP,
			LAST_SEEN_UPDATE_VERSION_KEY
		);
		if (!shouldShowUpdateMessage(lastSeenVersion))
		{
			return;
		}

		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", UPDATE_MESSAGE, null);
		configManager.setConfiguration(
			RingOfTimeConfig.GROUP,
			LAST_SEEN_UPDATE_VERSION_KEY,
			CURRENT_VERSION
		);
	}

	private void scheduleUpdateMessage()
	{
		cancelUpdateMessage();
		if (!shouldShowUpdateMessage(configManager.getConfiguration(
			RingOfTimeConfig.GROUP,
			LAST_SEEN_UPDATE_VERSION_KEY)))
		{
			return;
		}

		updateMessageFuture = executor.schedule(
			() -> clientThread.invokeLater(() ->
			{
				updateMessageFuture = null;
				if (groupManager != null && client.getGameState() == GameState.LOGGED_IN)
				{
					showUpdateMessageIfNeeded();
				}
			}),
			UPDATE_MESSAGE_DELAY_SECONDS,
			TimeUnit.SECONDS
		);
	}

	private void cancelUpdateMessage()
	{
		if (updateMessageFuture != null)
		{
			updateMessageFuture.cancel(false);
			updateMessageFuture = null;
		}
	}

	/**
	 * Samples every skill whose normal temporary levels share these clocks.
	 */
	private void observeAllSkills()
	{
		final int currentTick = client.getTickCount();
		for (Skill skill : TRACKABLE_SKILLS)
		{
			tracker.observe(
				skill,
				client.getBoostedSkillLevel(skill),
				client.getRealSkillLevel(skill),
				currentTick,
				divineTracker.isActive(skill)
			);
		}
	}

	/**
	 * Samples the exact remaining ticks for individual and combination divine
	 * potions. Moonlight is included only to avoid treating its shared Defence
	 * variable as a divine effect.
	 */
	private void observeDivineState()
	{
		divineTracker.observe(
			client.getVarbitValue(VarbitID.DIVINEATTACK_POTION_TIME),
			client.getVarbitValue(VarbitID.DIVINESTRENGTH_POTION_TIME),
			client.getVarbitValue(VarbitID.DIVINEDEFENCE_POTION_TIME),
			client.getVarbitValue(VarbitID.DIVINERANGE_POTION_TIME),
			client.getVarbitValue(VarbitID.DIVINEMAGIC_POTION_TIME),
			client.getVarbitValue(VarbitID.DIVINECOMBAT_POTION_TIME),
			client.getVarbitValue(VarbitID.DIVINEBASTION_POTION_TIME),
			client.getVarbitValue(VarbitID.DIVINEBATTLEMAGE_POTION_TIME),
			client.getVarbitValue(VarbitID.MOONLIGHT_POTION_TIME),
			System.currentTimeMillis()
		);
	}

	private static boolean isDivineVarbit(int varbitId)
	{
		return varbitId == VarbitID.DIVINEATTACK_POTION_TIME
			|| varbitId == VarbitID.DIVINESTRENGTH_POTION_TIME
			|| varbitId == VarbitID.DIVINEDEFENCE_POTION_TIME
			|| varbitId == VarbitID.DIVINERANGE_POTION_TIME
			|| varbitId == VarbitID.DIVINEMAGIC_POTION_TIME
			|| varbitId == VarbitID.DIVINECOMBAT_POTION_TIME
			|| varbitId == VarbitID.DIVINEBASTION_POTION_TIME
			|| varbitId == VarbitID.DIVINEBATTLEMAGE_POTION_TIME
			|| varbitId == VarbitID.MOONLIGHT_POTION_TIME;
	}

	/**
	 * Samples all requested non-skill effect variables.
	 */
	private void observeEffectState(boolean exactChange)
	{
		final int currentTick = client.getTickCount();
		final int poisonState = client.getVarpValue(VarPlayerID.POISON);
		effectTracker.observePoison(poisonState, currentTick, exactChange);
		effectTracker.observeAntipoison(poisonState, currentTick, exactChange);
		observeStamina(currentTick, exactChange);
		observePrayerRegeneration(currentTick, exactChange);
		observeAntifire(currentTick, exactChange);
	}

	/**
	 * Samples the paired stamina active/duration variables together.
	 */
	private void observeStamina(int currentTick, boolean exactChange)
	{
		effectTracker.observeStamina(
			client.getVarbitValue(VarbitID.STAMINA_DURATION),
			client.getVarbitValue(VarbitID.STAMINA_ACTIVE) == 1,
			currentTick,
			exactChange
		);
	}

	private void observePrayerRegeneration(int currentTick, boolean exactChange)
	{
		effectTracker.observePrayerRegeneration(
			client.getVarbitValue(VarbitID.PRAYER_REGENERATION_POTION_TIMER),
			currentTick,
			exactChange
		);
	}

	/**
	 * Samples regular and super-antifire because they use different unit sizes.
	 */
	private void observeAntifire(int currentTick, boolean exactChange)
	{
		effectTracker.observeAntifire(
			client.getVarbitValue(VarbitID.ANTIFIRE_POTION),
			client.getVarbitValue(VarbitID.SUPER_ANTIFIRE_POTION),
			currentTick,
			exactChange
		);
	}

	/**
	 * Packs only active, untouched circles so inactive timers leave no gaps.
	 * Skill circles precede effect circles in a stable order.
	 */
	private void layoutActiveOverlays()
	{
		if (groupManager == null)
		{
			return;
		}

		final Dimension maximumCircleSize = TimerLabelLayout.estimateMaximumDimension(config);
		int maximumGroupHeight = maximumCircleSize.height;
		for (TimerGroupOverlay group : groupManager.getGroups())
		{
			if (group.isTimerActive())
			{
				maximumGroupHeight = Math.max(
					maximumGroupHeight,
					group.estimateAutomaticHeight(maximumCircleSize)
				);
			}
		}

		int activeIndex = 0;
		for (TimerGroupOverlay group : groupManager.getGroups())
		{
			if (!group.isTimerActive())
			{
				continue;
			}

			final Point location = TimerCircleLayout.getLocation(activeIndex, maximumGroupHeight);
			if (group.applyAutomaticLocation(location))
			{
				activeIndex++;
			}
		}
	}

	/**
	 * Returns the skill tracker used by skill-circle overlays.
	 */
	StatChangeTracker getTracker()
	{
		return tracker;
	}

	/**
	 * Returns the fixed-duration tracker used by divine skill rings.
	 */
	DivineTimerTracker getDivineTracker()
	{
		return divineTracker;
	}

	/**
	 * Returns the non-skill tracker used by effect-circle overlays.
	 */
	EffectTimerTracker getEffectTracker()
	{
		return effectTracker;
	}

	/**
	 * Converts wall-clock time since the last game tick into smooth animation.
	 */
	double getSubTickProgress()
	{
		if (lastGameTickMillis == 0L)
		{
			return 0d;
		}

		final long elapsedMillis = Math.max(0L, System.currentTimeMillis() - lastGameTickMillis);
		return Math.min(1d, elapsedMillis / (double) Constants.GAME_TICK_LENGTH);
	}

	/**
	 * Reports Preserve state for remaining-time projections.
	 */
	boolean isPreserveActive()
	{
		return client.getGameState() == GameState.LOGGED_IN
			&& client.getVarbitValue(Prayer.PRESERVE.getVarbit()) == 1;
	}

	/**
	 * Applies the user's combat/non-combat filter without changing tracker state.
	 */
	boolean isSkillVisible(Skill skill)
	{
		switch (config.trackedSkills())
		{
			case COMBAT:
				return COMBAT_SKILLS.contains(skill);
			case NON_COMBAT:
				return NON_COMBAT_SKILLS.contains(skill);
			case ALL:
				return TRACKABLE_SKILLS.contains(skill);
			default:
				return false;
		}
	}
}
