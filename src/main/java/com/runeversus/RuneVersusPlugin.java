package com.runeversus;

import com.google.inject.Provides;
import com.runeversus.model.DuelResult;
import com.runeversus.party.RuneVersusDuelPartyMessage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "RuneVersus",
	description = "Generate OSRS duel cards comparing players, party members, and clanmates.",
	tags = {"hiscore", "compare", "versus", "duel", "party", "clan", "boss", "kc", "xp", "card"},
	enabledByDefault = false
)
public class RuneVersusPlugin extends Plugin
{
	private static final String VS_COMMAND = "!vs";
	private static final String MENU_COMPARE = "VS Compare";
	private static final String MENU_SET_A = "VS Set A";
	private static final String MENU_SET_B = "VS Set B";

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
	private PartyService partyService;

	@Inject
	private WSClient wsClient;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ChatCommandManager chatCommandManager;

	@Inject
	private Provider<MenuManager> menuManager;

	private final Map<Integer, String> playerIndexName = new HashMap<>();
	private RuneVersusPanel panel;
	private NavigationButton navButton;
	private volatile String localPlayerName = "";
	private volatile DuelResult lastResult;

	@Provides
	RuneVersusConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RuneVersusConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new RuneVersusPanel();
		panel.setCompareCallback(this::compare);
		panel.setRosterCallback(this::loadRoster);
		panel.setSocialActionCallback(this::handleSocialAction);
		panel.setLocalPlayerSupplier(() -> localPlayerName);
		panel.setExportAgainCallback(this::exportLastCard);

		BufferedImage icon = cardRenderer.renderIcon();
		navButton = NavigationButton.builder()
			.tooltip("RuneVersus")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		wsClient.registerMessage(RuneVersusDuelPartyMessage.class);
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
		wsClient.unregisterMessage(RuneVersusDuelPartyMessage.class);
		clientToolbar.removeNavigation(navButton);
		removePlayerMenuItems();
		playerIndexName.clear();
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
		for (MenuEntry entry : event.getMenuEntries())
		{
			if (entry.getType() != MenuAction.RUNELITE_PLAYER || !isVersusPlayerMenu(entry.getOption()))
			{
				continue;
			}

			Player player = entry.getPlayer();
			if (player != null)
			{
				playerIndexName.put(entry.getIdentifier(), clean(player.getName()));
			}
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

		String option = event.getMenuOption();
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
			openPanel();
			panel.setStatus("Log in before comparing a player from the right-click menu.");
			return;
		}

		openPanel();
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

	@Subscribe
	public void onPartyChanged(PartyChanged event)
	{
		loadRoster(RuneVersusPanel.RosterKind.PARTY);
	}

	@Subscribe
	public void onRuneVersusDuelPartyMessage(RuneVersusDuelPartyMessage message)
	{
		PartyMember sender = partyService.getMemberById(message.getMemberId());
		PartyMember local = partyService.getLocalMember();
		if (sender == null || sender == local)
		{
			return;
		}

		sendChatMessage("[RuneVersus] " + sender.getDisplayName() + " generated "
			+ message.leftName + " vs " + message.rightName + ": "
			+ message.leftScore + "-" + message.rightScore + ". " + message.verdict);
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
			setChatCommandResponse(chatMessage, "[RuneVersus] Fetching " + arguments.left + " vs " + arguments.right + "...");
			compareForIncomingChat(chatMessage, arguments.left, arguments.right);
		}
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
		versusService.compare(leftName, rightName)
			.thenAccept(this::handleDuelResult)
			.exceptionally(ex ->
			{
				panel.setStatus("Comparison failed: " + readableError(ex));
				sendChatMessage("[RuneVersus] Comparison failed: " + readableError(ex));
				return null;
			});
	}

	private void compareForIncomingChat(ChatMessage chatMessage, String leftName, String rightName)
	{
		versusService.compare(leftName, rightName)
			.thenAccept(result -> setChatCommandResponse(chatMessage, buildCompactResult(result, verdict(result))))
			.exceptionally(ex ->
			{
				setChatCommandResponse(chatMessage, "[RuneVersus] Comparison failed: " + readableError(ex));
				return null;
			});
	}

	private void handleDuelResult(DuelResult result)
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
		sendResultMessage(result, exported, verdict);
		if (config.partyAnnounceCards() && partyService.isInParty())
		{
			announceToParty(result, verdict);
		}
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

	private void announceToParty(DuelResult result, String verdict)
	{
		RuneVersusDuelPartyMessage message = new RuneVersusDuelPartyMessage();
		message.leftName = result.getLeft().getName();
		message.rightName = result.getRight().getName();
		message.winnerName = result.getWinnerName();
		message.verdict = verdict;
		message.leftScore = result.getLeftTotalWins();
		message.rightScore = result.getRightTotalWins();
		partyService.send(message);
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
				case PARTY:
					names = rosterService.getPartyMembers();
					label = "Party";
					break;
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
				case PARTY_BOARD:
					analyzeRoster("Party", rosterService.getPartyMembers(), false);
					break;
				case CLAN_BOARD:
					analyzeRoster("Online clan", rosterService.getOnlineClanMembers(), false);
					break;
				case CLAN_RECAP:
					analyzeRoster("Clan recap", rosterService.getOnlineClanMembers(), true);
					break;
				case FIGHT_NIGHT:
					startFightNight();
					break;
				case WATCHLIST:
					checkWatchlist();
					break;
				default:
					panel.setStatus("Unknown social action.");
			}
		});
	}

	private void analyzeRoster(String label, List<String> names, boolean exportRecap)
	{
		List<String> roster = limitedNames(names);
		if (roster.isEmpty())
		{
			panel.setStatus("No players found for " + label + ".");
			panel.showText("[RuneVersus] " + label + ": no players found.");
			return;
		}

		panel.setStatus("Fetching " + roster.size() + " " + label + " player(s)...");
		versusService.analyzeRoster(label, roster)
			.thenAccept(leaderboard ->
			{
				String text = leaderboard.toCompactText();
				File exported = null;
				if (exportRecap)
				{
					try
					{
						exported = cardExporter.exportRecap(leaderboard, config.copyPathToClipboard(), config.cardTheme());
					}
					catch (IOException ex)
					{
						panel.setStatus("Recap built, but PNG export failed: " + ex.getMessage());
					}
				}

				StringBuilder output = new StringBuilder(text);
				if (exported != null)
				{
					output.append("\nPNG: ").append(exported.getAbsolutePath());
				}
				panel.showText(output.toString());
				panel.setStatus(exported == null ? "Leaderboard ready." : "Recap exported: " + exported.getName());
				sendChatMessage(exported == null ? text : text + " PNG: " + exported.getAbsolutePath());
			})
			.exceptionally(ex ->
			{
				panel.setStatus("Leaderboard failed: " + readableError(ex));
				sendChatMessage("[RuneVersus] Leaderboard failed: " + readableError(ex));
				return null;
			});
	}

	private void startFightNight()
	{
		List<String> names = limitedNames(rosterService.getOnlineClanMembers());
		if (names.size() < 2)
		{
			names = limitedNames(rosterService.getPartyMembers());
		}
		if (names.size() < 2)
		{
			panel.setStatus("Fight Night needs at least two online clan or party players.");
			panel.showText("[RuneVersus] Fight Night needs at least two online clan or party players.");
			return;
		}

		Collections.shuffle(names);
		String left = names.get(0);
		String right = names.get(1);
		panel.setStatus("Fight Night matchup: " + left + " vs " + right + "...");
		versusService.compare(left, right)
			.thenAccept(result ->
			{
				lastResult = result;
				String verdict = "Fight Night: " + verdict(result);
				File exported = null;
				try
				{
					exported = cardExporter.export(result, config.copyPathToClipboard(), RuneVersusCardTheme.CLAN_WAR, verdict);
				}
				catch (IOException ex)
				{
					panel.setStatus("Fight Night compared, but PNG export failed: " + ex.getMessage());
				}

				panel.showResult(result, exported, verdict);
				sendChatMessage(buildCompactResult(result, verdict)
					+ (exported == null ? "" : " PNG: " + exported.getAbsolutePath()));
			})
			.exceptionally(ex ->
			{
				panel.setStatus("Fight Night failed: " + readableError(ex));
				sendChatMessage("[RuneVersus] Fight Night failed: " + readableError(ex));
				return null;
			});
	}

	private void checkWatchlist()
	{
		String local = getLocalPlayerName();
		if (local.isEmpty())
		{
			panel.setStatus("Log in before checking watchlist rivals.");
			return;
		}

		List<String> rivals = limitedNames(parseNameList(config.watchlistRivals()));
		if (rivals.isEmpty())
		{
			panel.setStatus("Add comma-separated rivals in RuneVersus config first.");
			panel.showText("[RuneVersus] Watchlist is empty. Add comma-separated RSNs in config.");
			return;
		}

		panel.setStatus("Checking " + rivals.size() + " watchlist rival(s)...");
		List<CompletableFuture<String>> futures = new ArrayList<>();
		for (String rival : rivals)
		{
			if (rival.equalsIgnoreCase(local))
			{
				continue;
			}
			futures.add(versusService.compare(local, rival)
				.thenApply(result -> buildWatchlistLine(result, verdict(result)))
				.exceptionally(ex -> "[RuneVersus] " + local + " vs " + rival + " failed: " + readableError(ex)));
		}

		if (futures.isEmpty())
		{
			panel.setStatus("Watchlist only contains your current player.");
			return;
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
			.thenRun(() ->
			{
				StringBuilder text = new StringBuilder();
				for (CompletableFuture<String> future : futures)
				{
					String line = future.join();
					text.append(line).append('\n');
					sendChatMessage(line);
				}
				panel.showText(text.toString().trim());
				panel.setStatus("Watchlist checked.");
			});
	}

	private void sendResultMessage(DuelResult result, File exported, String verdict)
	{
		StringBuilder message = new StringBuilder()
			.append("[RuneVersus] ")
			.append(result.getLeft().getName())
			.append(" vs ")
			.append(result.getRight().getName())
			.append(": ")
			.append(result.getLeftTotalWins())
			.append("-")
			.append(result.getRightTotalWins())
			.append(". ")
			.append(verdict)
			.append(" ")
			.append(RuneVersusFlavor.snipeCallout(result));

		if (exported != null)
		{
			message.append(" PNG: ").append(exported.getAbsolutePath());
		}
		sendChatMessage(message.toString());
	}

	private String verdict(DuelResult result)
	{
		return RuneVersusFlavor.verdict(result, config.verdictStyle());
	}

	private static String buildCompactResult(DuelResult result, String verdict)
	{
		StringBuilder message = new StringBuilder()
			.append("[RuneVersus] ")
			.append(result.getLeft().getName())
			.append(" vs ")
			.append(result.getRight().getName())
			.append(" | Score ")
			.append(result.getLeftTotalWins())
			.append("-")
			.append(result.getRightTotalWins())
			.append(" | PvM ")
			.append(result.getLeftBossWins())
			.append("-")
			.append(result.getRightBossWins())
			.append(" | Skills ")
			.append(result.getLeftSkillWins())
			.append("-")
			.append(result.getRightSkillWins());

		if (!result.getDayForm().isEmpty())
		{
			message.append(" | 24h XP ")
				.append(formatShort(result.getLeftDayXp()))
				.append("-")
				.append(formatShort(result.getRightDayXp()));
		}

		message.append(" | ").append(verdict)
			.append(" | ")
			.append(RuneVersusFlavor.snipeCallout(result));
		return message.toString();
	}

	private static String buildWatchlistLine(DuelResult result, String verdict)
	{
		return buildCompactResult(result, verdict);
	}

	private List<String> limitedNames(List<String> names)
	{
		if (names == null || names.isEmpty())
		{
			return Collections.emptyList();
		}

		int max = Math.max(2, Math.min(50, config.maxRosterPlayers()));
		Set<String> unique = new LinkedHashSet<>();
		for (String name : names)
		{
			String cleaned = clean(name);
			if (!cleaned.isEmpty())
			{
				unique.add(cleaned);
			}
			if (unique.size() >= max)
			{
				break;
			}
		}
		return new ArrayList<>(unique);
	}

	private static List<String> parseNameList(String names)
	{
		if (names == null || names.trim().isEmpty())
		{
			return Collections.emptyList();
		}

		List<String> out = new ArrayList<>();
		for (String token : names.split(","))
		{
			String cleaned = clean(token);
			if (!cleaned.isEmpty())
			{
				out.add(cleaned);
			}
		}
		return out;
	}

	private static String socialActionLabel(RuneVersusPanel.SocialAction action)
	{
		switch (action)
		{
			case PARTY_BOARD:
				return "Party leaderboard";
			case CLAN_BOARD:
				return "Clan leaderboard";
			case CLAN_RECAP:
				return "Clan recap";
			case FIGHT_NIGHT:
				return "Fight Night";
			case WATCHLIST:
				return "Watchlist";
			default:
				return "social action";
		}
	}

	private void addPlayerMenuItems()
	{
		menuManager.get().addPlayerMenuItem(MENU_COMPARE);
		menuManager.get().addPlayerMenuItem(MENU_SET_A);
		menuManager.get().addPlayerMenuItem(MENU_SET_B);
	}

	private void removePlayerMenuItems()
	{
		menuManager.get().removePlayerMenuItem(MENU_COMPARE);
		menuManager.get().removePlayerMenuItem(MENU_SET_A);
		menuManager.get().removePlayerMenuItem(MENU_SET_B);
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

	private static String clean(String value)
	{
		return value == null ? "" : Text.sanitize(value).trim();
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

	private static String formatShort(long value)
	{
		long absolute = Math.abs(value);
		if (absolute >= 1_000_000_000L)
		{
			return String.format("%.1fb", value / 1_000_000_000.0);
		}
		if (absolute >= 1_000_000L)
		{
			return String.format("%.1fm", value / 1_000_000.0);
		}
		if (absolute >= 10_000L)
		{
			return String.format("%.1fk", value / 1_000.0);
		}
		return String.format("%,d", value);
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
