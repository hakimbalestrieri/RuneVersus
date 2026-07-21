package com.runeversus;

import com.google.inject.Provides;
import com.runeversus.model.DuelResult;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "RuneVersus",
	description = "Compare OSRS players and run fair monthly clan leagues with shareable podium cards.",
	tags = {"hiscore", "compare", "versus", "duel", "clan", "league", "competition", "boss", "kc", "xp", "card"},
	enabledByDefault = false
)
public class RuneVersusPlugin extends Plugin
{
	private static final String VS_COMMAND = "!vs";
	private static final String MENU_COMPARE = "VS Compare";
	private static final String MENU_SET_A = "VS Set A";
	private static final String MENU_SET_B = "VS Set B";
	private static final long INCOMING_VS_GLOBAL_COOLDOWN_MILLIS = 8_000L;
	private static final long INCOMING_VS_SENDER_COOLDOWN_MILLIS = 30_000L;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private RuneVersusConfig config;

	@Inject
	private RuneVersusService versusService;

	@Inject
	private RosterService rosterService;

	@Inject
	private RuneVersusCardRenderer cardRenderer;

	@Inject
	private CardExporter cardExporter;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ChatCommandManager chatCommandManager;

	@Inject
	private Provider<MenuManager> menuManager;

	@Inject
	private SkillIconManager skillIconManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private SpriteManager spriteManager;

	private final Map<Integer, String> playerIndexName = new HashMap<>();
	private final ConcurrentMap<String, Long> incomingVsBySender = new ConcurrentHashMap<>();
	private RuneVersusPanel panel;
	private NavigationButton navButton;
	private volatile String localPlayerName = "";
	private volatile DuelResult lastResult;
	private volatile YearMonth selectedLeagueMonth = YearMonth.now(ZoneOffset.UTC);
	private long nextIncomingVsAt;

	@Provides
	RuneVersusConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneVersusConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new RuneVersusPanel(new RuneVersusIcons(skillIconManager, itemManager, spriteManager));
		panel.setCompareCallback(this::compare);
		panel.setRosterCallback(this::loadRoster);
		panel.setSocialActionCallback(this::handleSocialAction);
		panel.setLocalPlayerSupplier(() -> localPlayerName);
		panel.setExportAgainCallback(this::exportLastCard);
		panel.setClanProgressRefreshCallback(() -> analyzeClanProgress(false, true));
		panel.setClanProgressExportCallback(this::exportClanProgress);
		panel.setMonthlyLeagueLoadCallback(month -> analyzeMonthlyLeague(month, false));
		panel.setMonthlyLeagueRefreshCallback(() -> analyzeMonthlyLeague(selectedLeagueMonth, true));
		panel.setMonthlyLeagueExportCallback(this::exportMonthlyLeague);

		BufferedImage icon = cardRenderer.renderIcon();
		navButton = NavigationButton.builder()
			.tooltip("RuneVersus")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		chatCommandManager.registerCommandAsync(VS_COMMAND, this::vsLookup);
		if (config.playerMenuOptions())
		{
			addPlayerMenuItems();
		}
		clientThread.invokeLater(() -> localPlayerName = rosterService.getLocalPlayerName());
	}

	@Override
	protected void shutDown()
	{
		chatCommandManager.unregisterCommand(VS_COMMAND);
		clientToolbar.removeNavigation(navButton);
		removePlayerMenuItems();
		if (panel != null)
		{
			panel.disposeWindows();
		}
		playerIndexName.clear();
		incomingVsBySender.clear();
		nextIncomingVsAt = 0L;
		lastResult = null;
		localPlayerName = "";
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"runeversus".equals(event.getGroup()))
		{
			return;
		}
		if ("openInterfaceOnComparison".equals(event.getKey()) && !config.openInterfaceOnComparison())
		{
			panel.hideVersusWindow();
		}

		removePlayerMenuItems();
		if (config.playerMenuOptions())
		{
			addPlayerMenuItems();
		}
	}

	@Subscribe
	private void onMenuOpened(MenuOpened event)
	{
		playerIndexName.clear();
		String clanMemberName = "";
		String clanMemberTarget = "";
		boolean hasClanCompare = false;
		for (MenuEntry entry : event.getMenuEntries())
		{
			if (entry.getType() == MenuAction.RUNELITE_PLAYER && isVersusPlayerMenu(entry.getOption()))
			{
				Player player = entry.getPlayer();
				if (player != null)
				{
					playerIndexName.put(entry.getIdentifier(), clean(player.getName()));
				}
			}

			if (MENU_COMPARE.equals(entry.getOption()) && entry.getType() == MenuAction.RUNELITE)
			{
				hasClanCompare = true;
			}
			if (clanMemberName.isEmpty() && isClanMemberMenuEntry(entry))
			{
				String name = clean(entry.getTarget());
				if (!name.isEmpty())
				{
					clanMemberName = name;
					clanMemberTarget = entry.getTarget();
				}
			}
		}

		if (config.playerMenuOptions() && !hasClanCompare && !clanMemberName.isEmpty())
		{
			addClanMemberMenuEntries(clanMemberName, clanMemberTarget);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() != MenuAction.RUNELITE_PLAYER || !isVersusPlayerMenu(event.getMenuOption()))
		{
			return;
		}

		Player player = event.getMenuEntry().getPlayer();
		String target = player == null ? playerIndexName.get(event.getId()) : player.getName();
		playerIndexName.clear();
		target = clean(target);
		if (target.isEmpty())
		{
			return;
		}

		handleVersusMenuOption(event.getMenuOption(), target);
	}

	private void handleVersusMenuOption(String option, String target)
	{
		if (MENU_SET_A.equals(option))
		{
			openPanel();
			panel.setPlayerA(target);
			panel.setStatus("Player A set: " + target);
			return;
		}
		if (MENU_SET_B.equals(option))
		{
			openPanel();
			panel.setPlayerB(target);
			panel.setStatus("Player B set: " + target);
			return;
		}

		String local = getLocalPlayerName();
		if (local.isEmpty())
		{
			String message = "Log in before comparing a player from the right-click menu.";
			panel.setStatus(message);
			sendChatMessage("[RuneVersus] " + message);
			return;
		}

		compare(local, target);
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		localPlayerName = rosterService.getLocalPlayerName();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			localPlayerName = "";
		}
	}

	private void vsLookup(ChatMessage chatMessage, String message)
	{
		boolean localCommand = isLocalCommand(chatMessage);
		// Incoming commands are intentionally handled from any chat type supported by ChatCommandManager.
		// !vs player compares the sender against player; !vs player1 player2 compares the supplied pair.
		String defaultLeft = localCommand ? getLocalPlayerName() : clean(chatMessage.getName());
		VsArguments arguments = parseVsArguments(message, defaultLeft);
		if (arguments == null)
		{
			if (localCommand)
			{
				sendChatMessage("[RuneVersus] Usage: !vs player or !vs player1 player2. Use quotes or comma for names with spaces.");
			}
			return;
		}

		if (arguments.left.equalsIgnoreCase(arguments.right))
		{
			if (localCommand)
			{
				sendChatMessage("[RuneVersus] Pick two different players.");
			}
			else
			{
				setChatCommandResponse(chatMessage, "[RuneVersus] Pick two different players.");
			}
			return;
		}

		if (localCommand)
		{
			sendChatMessage("[RuneVersus] Generating " + arguments.left + " vs " + arguments.right + "...");
			compare(arguments.left, arguments.right);
		}
		else
		{
			long retryAfter = reserveIncomingVs(clean(chatMessage.getName()));
			if (retryAfter > 0L)
			{
				setChatCommandResponse(chatMessage,
					"[RuneVersus] !vs cooling down. Retry in " + retryAfter + "s.");
				return;
			}
			setChatCommandResponse(chatMessage, "[RuneVersus] Fetching " + arguments.left + " vs " + arguments.right + "...");
			compareForIncomingChat(chatMessage, arguments.left, arguments.right);
		}
	}

	private synchronized long reserveIncomingVs(String sender)
	{
		long now = System.currentTimeMillis();
		String key = normalizedChatName(sender);
		long senderAllowedAt = incomingVsBySender.getOrDefault(key, 0L);
		long allowedAt = Math.max(nextIncomingVsAt, senderAllowedAt);
		if (now < allowedAt)
		{
			return Math.max(1L, (allowedAt - now + 999L) / 1_000L);
		}

		nextIncomingVsAt = now + INCOMING_VS_GLOBAL_COOLDOWN_MILLIS;
		incomingVsBySender.put(key, now + INCOMING_VS_SENDER_COOLDOWN_MILLIS);
		if (incomingVsBySender.size() > 256)
		{
			incomingVsBySender.entrySet().removeIf(entry -> entry.getValue() <= now);
		}
		return 0L;
	}

	private static String normalizedChatName(String name)
	{
		String cleaned = clean(name).toLowerCase(java.util.Locale.ROOT);
		return cleaned.isEmpty() ? "unknown" : cleaned;
	}

	private void compare(String left, String right)
	{
		String leftName = clean(left);
		String rightName = clean(right);
		if (leftName.isEmpty() || rightName.isEmpty())
		{
			panel.setStatus("Enter two RSNs first.");
			return;
		}
		if (leftName.equalsIgnoreCase(rightName))
		{
			panel.setStatus("Pick two different players.");
			return;
		}

		panel.setStatus("Fetching " + leftName + " vs " + rightName + "...");
		boolean separateWindow = config.openInterfaceOnComparison();
		panel.showVersusLoading(leftName, rightName, separateWindow);
		if (!separateWindow)
		{
			openPanel();
		}
		versusService.compare(leftName, rightName)
			.thenAccept(result -> handleDuelResult(result, separateWindow))
			.exceptionally(ex ->
			{
				String message = "Comparison failed: " + readableError(ex);
				panel.setStatus(message);
				panel.showVersusError(message, separateWindow);
				sendChatMessage("[RuneVersus] " + message);
				return null;
			});
	}

	private void compareForIncomingChat(ChatMessage chatMessage, String leftName, String rightName)
	{
		versusService.compare(leftName, rightName)
			.thenAccept(result -> setChatCommandResponse(chatMessage, RuneVersusChatFormatter.format(result)))
			.exceptionally(ex ->
			{
				setChatCommandResponse(chatMessage, "[RuneVersus] Comparison failed: " + readableError(ex));
				return null;
			});
	}

	private void handleDuelResult(DuelResult result, boolean separateWindow)
	{
		lastResult = result;
		String verdict = verdict(result);
		File exported = null;
		if (config.autoExportCard())
		{
			try
			{
				exported = cardExporter.export(result, config.copyPathToClipboard(), config.cardTheme(), verdict);
			}
			catch (IOException ex)
			{
				panel.setStatus("Compared, but PNG export failed: " + ex.getMessage());
			}
		}

		panel.showResult(result, exported, verdict);
		panel.showVersusResult(result, exported, verdict, separateWindow);
		sendResultMessage(result);
	}

	private void exportLastCard()
	{
		DuelResult result = lastResult;
		if (result == null)
		{
			panel.setStatus("No comparison to export yet.");
			return;
		}
		try
		{
			String verdict = verdict(result);
			File exported = cardExporter.export(result, config.copyPathToClipboard(), config.cardTheme(), verdict);
			panel.showResult(result, exported, verdict);
			panel.setStatus("Card exported: " + exported.getName());
			sendChatMessage("[RuneVersus] Card exported: " + exported.getAbsolutePath());
		}
		catch (IOException ex)
		{
			panel.setStatus("PNG export failed: " + ex.getMessage());
		}
	}

	private void loadRoster(RuneVersusPanel.RosterKind kind)
	{
		if (panel == null)
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			List<String> names;
			String label;
			switch (kind)
			{
				case CLAN_ONLINE:
					names = rosterService.getOnlineClanMembers();
					label = "Online clan";
					break;
				case CLAN_ALL:
					names = rosterService.getAllClanMembers();
					label = "All clan";
					break;
				default:
					names = Collections.emptyList();
					label = "Roster";
			}
			panel.setRoster(label, names);
		});
	}

	private void handleSocialAction(RuneVersusPanel.SocialAction action)
	{
		if (panel == null)
		{
			return;
		}

		panel.setStatus("Preparing " + socialActionLabel(action) + "...");
		clientThread.invokeLater(() ->
		{
			switch (action)
			{
				case MONTHLY_LEAGUE:
					analyzeMonthlyLeague(YearMonth.now(ZoneOffset.UTC), false);
					break;
				case CLAN_PROGRESS:
					analyzeClanProgress(false, false);
					break;
				case CLAN_PROGRESS_CARD:
					analyzeClanProgress(true, false);
					break;
				default:
					panel.setStatus("Unknown social action.");
			}
		});
	}

	private void sendResultMessage(DuelResult result)
	{
		sendChatMessage(RuneVersusChatFormatter.format(result));
	}

	private void analyzeClanProgress(boolean exportCard, boolean forceRefresh)
	{
		int groupId = wiseOldManGroupId();
		if (groupId <= 0)
		{
			String message = "Set your WOM group ID in RuneVersus settings > Clan first.";
			panel.setStatus(message);
			if (!exportCard)
			{
				panel.showClanProgressLoading(message);
				panel.showClanProgressError(message);
			}
			return;
		}

		panel.setStatus("Fetching clan gains and all-time totals...");
		if (!exportCard)
		{
			panel.showClanProgressLoading("Loading five periods for every WOM clan member...");
		}
		versusService.analyzeClanProgress("Clan progress", groupId, forceRefresh)
			.thenAccept(leaderboard ->
			{
				if (leaderboard.getPlayers().isEmpty())
				{
					String message = "No tracked players found in WOM group #" + groupId + ".";
					if (!exportCard)
					{
						panel.showClanProgressError(message);
					}
					panel.setStatus(message);
					sendChatMessage("[RuneVersus] " + message);
					return;
				}

				if (exportCard)
				{
					exportClanProgress(leaderboard, false);
				}
				else
				{
					panel.showClanProgress(leaderboard);
					panel.setStatus("Clan comparison ready.");
				}
			})
			.exceptionally(ex ->
			{
				String message = "Clan progress failed: " + readableError(ex);
				panel.setStatus(message);
				if (!exportCard)
				{
					panel.showClanProgressError(message);
				}
				sendChatMessage("[RuneVersus] " + message);
				return null;
			});
	}

	private void exportClanProgress(ClanProgressLeaderboard leaderboard)
	{
		exportClanProgress(leaderboard, true);
	}

	private void analyzeMonthlyLeague(YearMonth month, boolean forceRefresh)
	{
		int groupId = wiseOldManGroupId();
		selectedLeagueMonth = month == null ? YearMonth.now(ZoneOffset.UTC) : month;
		if (groupId <= 0)
		{
			String message = "Set your WOM group ID in RuneVersus settings > Clan first.";
			panel.setStatus(message);
			panel.showMonthlyLeagueLoading(selectedLeagueMonth, message);
			panel.showMonthlyLeagueError(message);
			return;
		}

		panel.setStatus("Loading the " + selectedLeagueMonth + " monthly league...");
		panel.showMonthlyLeagueLoading(selectedLeagueMonth,
			"Calculating fair EHP and EHB scores for WOM group #" + groupId + "...");
		versusService.analyzeMonthlyLeague(groupId, selectedLeagueMonth, forceRefresh)
			.thenAccept(season ->
			{
				if (season.getStandings().isEmpty())
				{
					String message = "No WOM gains were found for " + season.getLabel() + ".";
					panel.showMonthlyLeagueError(message);
					panel.setStatus(message);
					return;
				}
				panel.showMonthlyLeague(season);
				panel.setStatus("Monthly league ready: " + season.getEligibleCount()
					+ " eligible, " + season.getProvisionalCount() + " provisional.");
			})
			.exceptionally(ex ->
			{
				String message = "Monthly league failed: " + readableError(ex);
				panel.showMonthlyLeagueError(message);
				panel.setStatus(message);
				return null;
			});
	}

	private void exportMonthlyLeague(MonthlyLeagueSeason season)
	{
		try
		{
			File exported = cardExporter.exportMonthlyLeague(
				season, config.copyPathToClipboard(), config.cardTheme());
			panel.showSavedCard(exported);
			panel.setStatus("Monthly podium card saved: " + exported.getName());
		}
		catch (IOException ex)
		{
			String message = "Monthly podium export failed: " + ex.getMessage();
			panel.setStatus(message);
			panel.showMonthlyLeagueError(message);
		}
	}

	private void exportClanProgress(ClanProgressLeaderboard leaderboard, boolean showWindowOnError)
	{
		try
		{
			File exported = cardExporter.exportClanProgress(
				leaderboard, config.copyPathToClipboard(), config.cardTheme());
			panel.showSavedCard(exported);
			panel.setStatus("Progress card saved: " + exported.getName());
		}
		catch (IOException ex)
		{
			String message = "PNG export failed: " + ex.getMessage();
			panel.setStatus(message);
			if (showWindowOnError)
			{
				panel.showClanProgressError(message);
			}
		}
	}

	private String verdict(DuelResult result)
	{
		return RuneVersusFlavor.verdict(result, config.verdictStyle());
	}

	private static String socialActionLabel(RuneVersusPanel.SocialAction action)
	{
		switch (action)
		{
			case MONTHLY_LEAGUE:
				return "monthly league";
			case CLAN_PROGRESS:
				return "Clan member comparison";
			case CLAN_PROGRESS_CARD:
				return "progress card";
			default:
				return "social action";
		}
	}

	private int wiseOldManGroupId()
	{
		String configured = config.wiseOldManGroupId();
		if (configured == null)
		{
			return 0;
		}
		try
		{
			int groupId = Integer.parseInt(configured.trim());
			return Math.max(0, groupId);
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
	}

	private void addPlayerMenuItems()
	{
		menuManager.get().addPlayerMenuItem(MENU_COMPARE);
	}

	private void removePlayerMenuItems()
	{
		menuManager.get().removePlayerMenuItem(MENU_COMPARE);
		// Remove legacy world-player entries too, in case this version replaces an older build at runtime.
		menuManager.get().removePlayerMenuItem(MENU_SET_A);
		menuManager.get().removePlayerMenuItem(MENU_SET_B);
	}

	private void addClanMemberMenuEntries(String playerName, String displayTarget)
	{
		addClanMemberMenuEntry(MENU_COMPARE, playerName, displayTarget);
		if (config.clanSetMenuOptions())
		{
			// RuneLite inserts index -1 entries at the top, so create B first to display A above it.
			addClanMemberMenuEntry(MENU_SET_B, playerName, displayTarget);
			addClanMemberMenuEntry(MENU_SET_A, playerName, displayTarget);
		}
	}

	private void addClanMemberMenuEntry(String option, String playerName, String displayTarget)
	{
		client.createMenuEntry(-1)
			.setOption(option)
			.setTarget(displayTarget == null || displayTarget.isEmpty() ? playerName : displayTarget)
			.setType(MenuAction.RUNELITE)
			.onClick(entry -> handleVersusMenuOption(option, playerName));
	}

	private static boolean isClanMemberMenuEntry(MenuEntry entry)
	{
		return isClanMemberComponent(entry.getParam1(), entry.getType());
	}

	static boolean isClanMemberComponent(int componentId, MenuAction action)
	{
		if (action != MenuAction.CC_OP && action != MenuAction.CC_OP_LOW_PRIORITY)
		{
			return false;
		}
		return componentId == InterfaceID.ClansSidepanel.PLAYERLIST
			|| componentId == InterfaceID.ClansGuestSidepanel.PLAYERLIST
			|| WidgetUtil.componentToInterface(componentId) == InterfaceID.CLANS_MEMBERS;
	}

	private void openPanel()
	{
		SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navButton));
	}

	private static boolean isVersusPlayerMenu(String option)
	{
		return MENU_COMPARE.equals(option) || MENU_SET_A.equals(option) || MENU_SET_B.equals(option);
	}

	private void sendChatMessage(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	private boolean isLocalCommand(ChatMessage chatMessage)
	{
		if (chatMessage.getType() == ChatMessageType.PRIVATECHATOUT)
		{
			return true;
		}

		String local = localPlayerName;
		if (local == null || local.isEmpty())
		{
			local = rosterService.getLocalPlayerName();
		}

		String sender = clean(chatMessage.getName());
		return !local.isEmpty() && sender.equalsIgnoreCase(local);
	}

	private String getLocalPlayerName()
	{
		String local = clean(localPlayerName);
		if (local.isEmpty())
		{
			local = clean(rosterService.getLocalPlayerName());
		}
		return local;
	}

	private void setChatCommandResponse(ChatMessage chatMessage, String response)
	{
		clientThread.invokeLater(() ->
		{
			chatMessage.getMessageNode().setRuneLiteFormatMessage(response);
			client.refreshChat();
		});
	}

	private VsArguments parseVsArguments(String message, String defaultLeft)
	{
		String args = message == null ? "" : message.trim();
		if (args.toLowerCase().startsWith(VS_COMMAND))
		{
			args = args.substring(VS_COMMAND.length()).trim();
		}
		if (args.isEmpty())
		{
			return null;
		}

		int comma = args.indexOf(',');
		if (comma >= 0)
		{
			String left = clean(unquote(args.substring(0, comma)));
			String right = clean(unquote(args.substring(comma + 1)));
			return left.isEmpty() || right.isEmpty() ? null : new VsArguments(left, right);
		}

		List<String> tokens = tokenizeArguments(args);
		if (tokens.size() == 1)
		{
			String left = clean(defaultLeft);
			return left.isEmpty() ? null : new VsArguments(left, tokens.get(0));
		}
		if (tokens.size() == 2)
		{
			return new VsArguments(tokens.get(0), tokens.get(1));
		}
		return null;
	}

	private static List<String> tokenizeArguments(String args)
	{
		List<String> tokens = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;

		for (int i = 0; i < args.length(); i++)
		{
			char c = args.charAt(i);
			if (c == '"')
			{
				quoted = !quoted;
				continue;
			}

			if (Character.isWhitespace(c) && !quoted)
			{
				if (current.length() > 0)
				{
					tokens.add(clean(current.toString()));
					current.setLength(0);
				}
				continue;
			}
			current.append(c);
		}

		if (current.length() > 0)
		{
			tokens.add(clean(current.toString()));
		}
		return tokens;
	}

	private static String unquote(String value)
	{
		String cleaned = clean(value);
		if (cleaned.length() >= 2 && cleaned.startsWith("\"") && cleaned.endsWith("\""))
		{
			return cleaned.substring(1, cleaned.length() - 1);
		}
		return cleaned;
	}

	static String clean(String value)
	{
		return value == null ? "" : Text.sanitize(Text.removeTags(value)).trim();
	}

	private static String readableError(Throwable throwable)
	{
		Throwable cursor = throwable;
		while (cursor.getCause() != null)
		{
			cursor = cursor.getCause();
		}
		return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
	}

	private static class VsArguments
	{
		private final String left;
		private final String right;

		private VsArguments(String left, String right)
		{
			this.left = left;
			this.right = right;
		}
	}
}
