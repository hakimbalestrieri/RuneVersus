package com.runeversus;

import com.runeversus.model.DuelResult;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.time.YearMonth;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

public class RuneVersusPanel extends PluginPanel
{
	private static final String MAIN_PAGE = "main";
	private static final String VERSUS_PAGE = "versus";
	private static final int CONTENT_WIDTH = PANEL_WIDTH - 16;
	private static final int INNER_WIDTH = CONTENT_WIDTH - 16;
	private static final Color TOOL_BUTTON_YELLOW = new Color(255, 193, 7);

	enum RosterKind
	{
		CLAN_ONLINE("Online clan"),
		CLAN_ALL("All clan members");

		private final String label;

		RosterKind(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	enum SocialAction
	{
		MONTHLY_LEAGUE,
		CLAN_PROGRESS,
		CLAN_PROGRESS_CARD
	}

	private final JComboBox<String> leftPlayer = new JComboBox<>();
	private final JComboBox<String> rightPlayer = new JComboBox<>();
	private final JComboBox<RosterKind> rosterKind = new JComboBox<>(RosterKind.values());
	private final JLabel rosterStatus = new JLabel();
	private final JLabel status = new JLabel();
	private final JTextArea toolsOutput = new JTextArea();
	private final JScrollPane toolsOutputScroll;
	private final JButton openCardButton = new JButton("Open saved card");
	private final JButton exportAgainButton = new JButton("Export card again");
	private final JPanel toolsPanel;
	private final CardLayout pageLayout = new CardLayout();
	private final JPanel mainPage = new JPanel();
	private final ClanProgressPanel clanProgressPanel;
	private final MonthlyLeaguePanel monthlyLeaguePanel;
	private final VersusComparisonPanel sidebarVersusPanel;
	private final VersusComparisonPanel windowVersusPanel;
	private JFrame clanWindow;
	private JFrame leagueWindow;
	private JFrame versusWindow;

	private BiConsumer<String, String> compareCallback;
	private Consumer<RosterKind> rosterCallback;
	private Consumer<SocialAction> socialActionCallback;
	private Supplier<String> localPlayerSupplier;
	private Runnable exportAgainCallback;
	private Runnable clanProgressRefreshCallback;
	private Consumer<ClanProgressLeaderboard> clanProgressExportCallback;
	private Consumer<YearMonth> monthlyLeagueLoadCallback;
	private Runnable monthlyLeagueRefreshCallback;
	private Consumer<MonthlyLeagueSeason> monthlyLeagueExportCallback;
	private volatile File exportedCard;

	RuneVersusPanel()
	{
		this(RuneVersusIcons.empty());
	}

	RuneVersusPanel(RuneVersusIcons icons)
	{
		setLayout(pageLayout);
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		mainPage.setLayout(new BoxLayout(mainPage, BoxLayout.Y_AXIS));
		mainPage.setBackground(ColorScheme.DARK_GRAY_COLOR);

		configurePlayerBox(leftPlayer, "First Old School RuneScape name");
		configurePlayerBox(rightPlayer, "Second Old School RuneScape name");
		configureStatusLabels();

		mainPage.add(title("RuneVersus"));
		mainPage.add(subtitle("Compare OSRS players"));
		mainPage.add(Box.createVerticalStrut(12));
		mainPage.add(buildPlayersPanel());
		mainPage.add(Box.createVerticalStrut(12));

		toolsOutputScroll = buildToolsOutput();
		toolsOutputScroll.setVisible(false);
		toolsPanel = buildToolsPanel();
		finishPanelWidth(toolsPanel);
		mainPage.add(toolsPanel);

		mainPage.add(Box.createVerticalGlue());
		mainPage.add(Box.createVerticalStrut(10));
		configureCardButtons();
		mainPage.add(status);
		mainPage.add(Box.createVerticalStrut(4));
		mainPage.add(openCardButton);
		mainPage.add(Box.createVerticalStrut(4));
		mainPage.add(exportAgainButton);
		clanProgressPanel = new ClanProgressPanel(
			this::hideClanWindow,
			() ->
			{
				if (clanProgressRefreshCallback != null)
				{
					clanProgressRefreshCallback.run();
				}
			},
			leaderboard ->
			{
				if (clanProgressExportCallback != null)
				{
					clanProgressExportCallback.accept(leaderboard);
				}
			},
			this::openSavedCard,
			icons);
		monthlyLeaguePanel = new MonthlyLeaguePanel(
			this::hideLeagueWindow,
			month ->
			{
				if (monthlyLeagueLoadCallback != null)
				{
					monthlyLeagueLoadCallback.accept(month);
				}
			},
			() ->
			{
				if (monthlyLeagueRefreshCallback != null)
				{
					monthlyLeagueRefreshCallback.run();
				}
			},
			league ->
			{
				if (monthlyLeagueExportCallback != null)
				{
					monthlyLeagueExportCallback.accept(league);
				}
			},
			this::openSavedCard);
		sidebarVersusPanel = new VersusComparisonPanel(
			"Back",
			this::showMainPage,
			() ->
			{
				if (exportAgainCallback != null)
				{
					exportAgainCallback.run();
				}
			},
			this::openSavedCard,
			icons,
			false);
		windowVersusPanel = new VersusComparisonPanel(
			"Close",
			this::hideVersusWindow,
			() ->
			{
				if (exportAgainCallback != null)
				{
					exportAgainCallback.run();
				}
			},
			this::openSavedCard,
			icons);
		add(mainPage, MAIN_PAGE);
		add(sidebarVersusPanel, VERSUS_PAGE);
		pageLayout.show(this, MAIN_PAGE);
	}

	void setCompareCallback(BiConsumer<String, String> compareCallback)
	{
		this.compareCallback = compareCallback;
	}

	void setRosterCallback(Consumer<RosterKind> rosterCallback)
	{
		this.rosterCallback = rosterCallback;
	}

	void setSocialActionCallback(Consumer<SocialAction> socialActionCallback)
	{
		this.socialActionCallback = socialActionCallback;
	}

	void setLocalPlayerSupplier(Supplier<String> localPlayerSupplier)
	{
		this.localPlayerSupplier = localPlayerSupplier;
	}

	void setExportAgainCallback(Runnable exportAgainCallback)
	{
		this.exportAgainCallback = exportAgainCallback;
	}

	void setClanProgressRefreshCallback(Runnable callback)
	{
		this.clanProgressRefreshCallback = callback;
	}

	void setClanProgressExportCallback(Consumer<ClanProgressLeaderboard> callback)
	{
		this.clanProgressExportCallback = callback;
	}

	void setMonthlyLeagueLoadCallback(Consumer<YearMonth> callback)
	{
		this.monthlyLeagueLoadCallback = callback;
	}

	void setMonthlyLeagueRefreshCallback(Runnable callback)
	{
		this.monthlyLeagueRefreshCallback = callback;
	}

	void setMonthlyLeagueExportCallback(Consumer<MonthlyLeagueSeason> callback)
	{
		this.monthlyLeagueExportCallback = callback;
	}

	void setStatus(String message)
	{
		SwingUtilities.invokeLater(() -> status.setText(statusHtml(message)));
	}

	void setPlayerA(String name)
	{
		SwingUtilities.invokeLater(() -> setPlayer(leftPlayer, name));
	}

	void setPlayerB(String name)
	{
		SwingUtilities.invokeLater(() -> setPlayer(rightPlayer, name));
	}

	void showText(String text)
	{
		SwingUtilities.invokeLater(() ->
		{
			toolsOutput.setText(text);
			toolsOutput.setCaretPosition(0);
			toolsOutputScroll.setVisible(true);
			revalidate();
			repaint();
		});
	}

	void setRoster(String label, List<String> names)
	{
		SwingUtilities.invokeLater(() ->
		{
			String currentLeft = playerName(leftPlayer);
			String currentRight = playerName(rightPlayer);
			leftPlayer.setModel(playerModel(names));
			rightPlayer.setModel(playerModel(names));
			leftPlayer.setEditable(true);
			rightPlayer.setEditable(true);

			if (!currentLeft.isEmpty())
			{
				setPlayer(leftPlayer, currentLeft);
			}
			else if (!names.isEmpty())
			{
				leftPlayer.setSelectedIndex(0);
			}

			if (!currentRight.isEmpty())
			{
				setPlayer(rightPlayer, currentRight);
			}
			else if (names.size() > 1)
			{
				rightPlayer.setSelectedIndex(1);
			}
			else if (!names.isEmpty())
			{
				rightPlayer.setSelectedIndex(0);
			}

			rosterStatus.setText(infoHtml(label + ": " + names.size() + " player(s) loaded"));
		});
	}

	void showResult(DuelResult result, File exported)
	{
		showResult(result, exported, result.getVerdict());
	}

	void showResult(DuelResult result, File exported, String verdict)
	{
		SwingUtilities.invokeLater(() ->
		{
			exportedCard = exported;
			sidebarVersusPanel.setExportedCard(exported);
			windowVersusPanel.setExportedCard(exported);
			openCardButton.setVisible(exported != null);
			exportAgainButton.setVisible(true);
			exportAgainButton.setEnabled(true);
			status.setText(statusHtml(exported == null
				? "Comparison complete. Result posted in chat."
				: "Comparison complete. Card saved."));
			revalidate();
			repaint();
		});
	}

	void showVersusLoading(String leftName, String rightName, boolean separateWindow)
	{
		SwingUtilities.invokeLater(() ->
		{
			activeVersusPanel(separateWindow).setLoading(leftName, rightName);
			showVersusDestination(separateWindow);
			revalidate();
			repaint();
		});
	}

	void showVersusResult(DuelResult result, File exported, String verdict, boolean separateWindow)
	{
		SwingUtilities.invokeLater(() ->
		{
			exportedCard = exported;
			activeVersusPanel(separateWindow).setResult(result, exported, verdict);
			showVersusDestination(separateWindow);
			revalidate();
			repaint();
		});
	}

	void showVersusError(String message, boolean separateWindow)
	{
		SwingUtilities.invokeLater(() ->
		{
			activeVersusPanel(separateWindow).setError(message);
			showVersusDestination(separateWindow);
			revalidate();
			repaint();
		});
	}

	void hideVersusWindow()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(this::hideVersusWindow);
			return;
		}
		if (versusWindow != null)
		{
			versusWindow.setVisible(false);
		}
	}

	void disposeWindows()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (clanWindow != null)
			{
				clanWindow.dispose();
				clanWindow = null;
			}
			if (leagueWindow != null)
			{
				leagueWindow.dispose();
				leagueWindow = null;
			}
			if (versusWindow != null)
			{
				versusWindow.dispose();
				versusWindow = null;
			}
		});
	}

	void showSavedCard(File exported)
	{
		SwingUtilities.invokeLater(() ->
		{
			exportedCard = exported;
			openCardButton.setVisible(exported != null);
			clanProgressPanel.setExportedCard(exported);
			monthlyLeaguePanel.setExportedCard(exported);
			sidebarVersusPanel.setExportedCard(exported);
			windowVersusPanel.setExportedCard(exported);
			revalidate();
			repaint();
		});
	}

	void showClanProgress(ClanProgressLeaderboard leaderboard)
	{
		SwingUtilities.invokeLater(() ->
		{
			clanProgressPanel.setLeaderboard(leaderboard);
			showClanWindow();
		});
	}

	void showClanProgressLoading(String message)
	{
		SwingUtilities.invokeLater(() ->
		{
			clanProgressPanel.setLoading(message);
			showClanWindow();
		});
	}

	void showClanProgressError(String message)
	{
		SwingUtilities.invokeLater(() ->
		{
			clanProgressPanel.setError(message);
			showClanWindow();
		});
	}

	void showMonthlyLeague(MonthlyLeagueSeason season)
	{
		SwingUtilities.invokeLater(() ->
		{
			monthlyLeaguePanel.setSeason(season);
			showLeagueWindow();
		});
	}

	void showMonthlyLeagueLoading(YearMonth month, String message)
	{
		SwingUtilities.invokeLater(() ->
		{
			monthlyLeaguePanel.setLoading(month, message);
			showLeagueWindow();
		});
	}

	void showMonthlyLeagueError(String message)
	{
		SwingUtilities.invokeLater(() ->
		{
			monthlyLeaguePanel.setError(message);
			showLeagueWindow();
		});
	}

	private void showMainPage()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(this::showMainPage);
			return;
		}
		pageLayout.show(this, MAIN_PAGE);
		revalidate();
		repaint();
	}

	private VersusComparisonPanel activeVersusPanel(boolean separateWindow)
	{
		return separateWindow ? windowVersusPanel : sidebarVersusPanel;
	}

	private void showVersusDestination(boolean separateWindow)
	{
		if (separateWindow)
		{
			showMainPage();
			showVersusWindow();
		}
		else
		{
			hideVersusWindow();
			pageLayout.show(this, VERSUS_PAGE);
		}
	}

	private void showVersusWindow()
	{
		if (versusWindow == null)
		{
			versusWindow = new JFrame("RuneVersus - Player comparison");
			versusWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			versusWindow.setContentPane(windowVersusPanel);
			versusWindow.setMinimumSize(new Dimension(820, 650));
			versusWindow.setSize(1000, 820);
			versusWindow.setLocationByPlatform(true);
		}
		versusWindow.setVisible(true);
		versusWindow.setExtendedState(versusWindow.getExtendedState() & ~JFrame.ICONIFIED);
		versusWindow.toFront();
		versusWindow.requestFocus();
	}

	private void hideClanWindow()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(this::hideClanWindow);
			return;
		}
		if (clanWindow != null)
		{
			clanWindow.setVisible(false);
		}
	}

	private void hideLeagueWindow()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(this::hideLeagueWindow);
			return;
		}
		if (leagueWindow != null)
		{
			leagueWindow.setVisible(false);
		}
	}

	private void showClanWindow()
	{
		if (clanWindow == null)
		{
			clanWindow = new JFrame("RuneVersus — Clan performance");
			clanWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			clanWindow.setContentPane(clanProgressPanel);
			clanWindow.setMinimumSize(new Dimension(900, 650));
			clanWindow.setSize(1100, 820);
			clanWindow.setLocationByPlatform(true);
		}
		clanWindow.setVisible(true);
		clanWindow.setExtendedState(clanWindow.getExtendedState() & ~JFrame.ICONIFIED);
		clanWindow.toFront();
		clanWindow.requestFocus();
	}

	private void showLeagueWindow()
	{
		if (leagueWindow == null)
		{
			leagueWindow = new JFrame("RuneVersus — Monthly league");
			leagueWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			leagueWindow.setContentPane(monthlyLeaguePanel);
			leagueWindow.setMinimumSize(new Dimension(980, 700));
			leagueWindow.setSize(1180, 860);
			leagueWindow.setLocationByPlatform(true);
		}
		leagueWindow.setVisible(true);
		leagueWindow.setExtendedState(leagueWindow.getExtendedState() & ~JFrame.ICONIFIED);
		leagueWindow.toFront();
		leagueWindow.requestFocus();
	}

	private JPanel buildPlayersPanel()
	{
		JPanel panel = cardPanel();
		panel.add(sectionTitle("Players"));
		panel.add(Box.createVerticalStrut(3));
		panel.add(infoLabel("Type two names, or load players from your clan."));
		panel.add(Box.createVerticalStrut(7));
		panel.add(fieldRow("Player 1", leftPlayer));
		panel.add(Box.createVerticalStrut(3));
		panel.add(fieldRow("Player 2", rightPlayer));
		panel.add(Box.createVerticalStrut(8));

		JButton me = fullWidthButton("Use my name");
		me.addActionListener(e ->
		{
			if (localPlayerSupplier != null)
			{
				setPlayer(leftPlayer, localPlayerSupplier.get());
			}
		});
		panel.add(me);
		panel.add(Box.createVerticalStrut(12));

		panel.add(subsectionTitle("Load clan members"));
		panel.add(Box.createVerticalStrut(3));
		panel.add(fieldRow("Player list", rosterKind));
		panel.add(Box.createVerticalStrut(4));
		JButton loadPlayers = fullWidthButton("Load player list");
		loadPlayers.setToolTipText("Makes clan member names selectable in the two fields above");
		loadPlayers.addActionListener(e -> loadSelectedRoster());
		panel.add(loadPlayers);
		panel.add(Box.createVerticalStrut(5));
		panel.add(rosterStatus);
		panel.add(Box.createVerticalStrut(12));

		JButton compare = fullWidthButton("Compare players");
		compare.setFont(FontManager.getRunescapeBoldFont());
		compare.setBackground(TOOL_BUTTON_YELLOW);
		compare.setForeground(Color.BLACK);
		compare.setToolTipText("Compare total XP, Combat Achievements, and collection logs");
		compare.addActionListener(e -> runComparison());
		panel.add(compare);
		finishPanelWidth(panel);
		return panel;
	}

	private JPanel buildToolsPanel()
	{
		JPanel panel = cardPanel();
		panel.add(sectionTitle("Clan tools"));
		panel.add(Box.createVerticalStrut(3));
		panel.add(infoLabel("Clan performance, rankings, and shareable cards."));
		panel.add(Box.createVerticalStrut(7));
		panel.add(toolsOutputScroll);
		panel.add(Box.createVerticalStrut(8));

		addTool(panel,
			"Monthly league",
			"Live monthly podium using fair EHP and EHB scores, with a separate Collection spotlight.",
			SocialAction.MONTHLY_LEAGUE);
		addTool(panel,
			"Clan member comparison",
			"Opens a large clan window with five periods, champions, totals, search and ranking filters.",
			SocialAction.CLAN_PROGRESS);
		addTool(panel,
			"Export progress card",
			"Exports all 15 clan leaders directly to a shareable PNG without opening the clan window.",
			SocialAction.CLAN_PROGRESS_CARD);
		return panel;
	}

	private void addTool(JPanel panel, String title, String description, SocialAction action)
	{
		JButton button = fullWidthButton(title);
		button.setFont(FontManager.getRunescapeBoldFont());
		button.setBackground(TOOL_BUTTON_YELLOW);
		button.setForeground(Color.BLACK);
		button.setFocusPainted(false);
		button.addActionListener(e ->
		{
			if (socialActionCallback != null)
			{
				socialActionCallback.accept(action);
			}
		});
		panel.add(button);
		panel.add(Box.createVerticalStrut(2));
		panel.add(infoLabel(description));
		panel.add(Box.createVerticalStrut(9));
	}

	private JScrollPane buildToolsOutput()
	{
		toolsOutput.setEditable(false);
		toolsOutput.setLineWrap(true);
		toolsOutput.setWrapStyleWord(true);
		toolsOutput.setFont(FontManager.getRunescapeSmallFont());
		toolsOutput.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		toolsOutput.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		toolsOutput.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
		JScrollPane scroll = new JScrollPane(toolsOutput);
		scroll.setAlignmentX(CENTER_ALIGNMENT);
		scroll.setPreferredSize(new Dimension(INNER_WIDTH, 125));
		scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
		scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		return scroll;
	}

	private void configureStatusLabels()
	{
		status.setText(statusHtml("Enter two names to compare."));
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		status.setFont(FontManager.getRunescapeSmallFont());
		status.setHorizontalAlignment(SwingConstants.CENTER);
		status.setAlignmentX(CENTER_ALIGNMENT);
		status.setPreferredSize(new Dimension(CONTENT_WIDTH, 36));
		status.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

		rosterStatus.setText(infoHtml("Type names directly; loading a list is optional."));
		rosterStatus.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		rosterStatus.setFont(FontManager.getRunescapeSmallFont());
		rosterStatus.setHorizontalAlignment(SwingConstants.CENTER);
		rosterStatus.setAlignmentX(CENTER_ALIGNMENT);
		rosterStatus.setPreferredSize(new Dimension(INNER_WIDTH, 34));
		rosterStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
	}

	private void configureCardButtons()
	{
		openCardButton.setVisible(false);
		openCardButton.setText("<html><u>Open saved card</u></html>");
		openCardButton.setAlignmentX(CENTER_ALIGNMENT);
		openCardButton.setFont(FontManager.getRunescapeSmallFont());
		openCardButton.setForeground(ColorScheme.BRAND_ORANGE);
		openCardButton.setBorderPainted(false);
		openCardButton.setContentAreaFilled(false);
		openCardButton.setFocusPainted(false);
		openCardButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		openCardButton.setToolTipText("Open the exported PNG in your image viewer");
		openCardButton.addActionListener(e -> openSavedCard());

		exportAgainButton.setVisible(false);
		exportAgainButton.setEnabled(false);
		exportAgainButton.setAlignmentX(CENTER_ALIGNMENT);
		exportAgainButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
		exportAgainButton.addActionListener(e ->
		{
			if (exportAgainCallback != null)
			{
				exportAgainCallback.run();
			}
		});
	}

	private void openSavedCard()
	{
		File card = exportedCard;
		if (card == null || !card.isFile())
		{
			setStatus("The saved card could not be found.");
			return;
		}
		// LinkBrowser.open expects a filesystem path and delegates to Desktop#open.
		// A file: URI is treated as a literal filename and cannot be opened.
		LinkBrowser.open(card.getAbsolutePath());
	}

	private void loadSelectedRoster()
	{
		RosterKind selected = (RosterKind) rosterKind.getSelectedItem();
		if (selected != null && rosterCallback != null)
		{
			rosterStatus.setText(infoHtml("Loading " + selected.toString().toLowerCase() + "..."));
			rosterCallback.accept(selected);
		}
	}

	private void runComparison()
	{
		if (compareCallback != null)
		{
			compareCallback.accept(playerName(leftPlayer), playerName(rightPlayer));
		}
	}

	private void configurePlayerBox(JComboBox<String> comboBox, String tooltip)
	{
		comboBox.setEditable(true);
		comboBox.setToolTipText(tooltip);
		Component editor = comboBox.getEditor().getEditorComponent();
		if (editor instanceof JTextField)
		{
			((JTextField) editor).addActionListener(e -> runComparison());
		}
	}

	private static DefaultComboBoxModel<String> playerModel(List<String> names)
	{
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		for (String name : names)
		{
			model.addElement(name);
		}
		return model;
	}

	private static String playerName(JComboBox<String> comboBox)
	{
		Object value = comboBox.isEditable()
			? comboBox.getEditor().getItem()
			: comboBox.getSelectedItem();
		return value == null ? "" : value.toString().trim();
	}

	private static void setPlayer(JComboBox<String> comboBox, String name)
	{
		comboBox.getEditor().setItem(name == null ? "" : name);
	}

	private static JPanel cardPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
		panel.setAlignmentX(CENTER_ALIGNMENT);
		return panel;
	}

	private static void finishPanelWidth(JPanel panel)
	{
		Dimension preferred = panel.getPreferredSize();
		preferred.width = CONTENT_WIDTH;
		panel.setPreferredSize(preferred);
		panel.setMinimumSize(new Dimension(0, preferred.height));
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
	}

	private static JButton fullWidthButton(String text)
	{
		JButton button = new JButton(text);
		Dimension size = new Dimension(INNER_WIDTH, 30);
		button.setAlignmentX(CENTER_ALIGNMENT);
		button.setPreferredSize(size);
		button.setMinimumSize(new Dimension(0, 30));
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		return button;
	}

	private static JPanel fieldRow(String labelText, JComboBox<?> comboBox)
	{
		JPanel row = new JPanel(new BorderLayout(7, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setAlignmentX(CENTER_ALIGNMENT);
		row.setPreferredSize(new Dimension(INNER_WIDTH, 29));
		row.setMinimumSize(new Dimension(0, 29));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));

		JLabel label = new JLabel(labelText);
		label.setPreferredSize(new Dimension(61, 27));
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setFont(FontManager.getRunescapeSmallFont());
		row.add(label, BorderLayout.WEST);

		comboBox.setPreferredSize(new Dimension(INNER_WIDTH - 68, 27));
		row.add(comboBox, BorderLayout.CENTER);
		return row;
	}

	private static JLabel title(String text)
	{
		JLabel label = new JLabel(text, SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeBoldFont().deriveFont(Font.BOLD, 18f));
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setAlignmentX(CENTER_ALIGNMENT);
		return label;
	}

	private static JLabel subtitle(String text)
	{
		JLabel label = new JLabel(text, SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setAlignmentX(CENTER_ALIGNMENT);
		return label;
	}

	private static JLabel sectionTitle(String text)
	{
		JLabel label = new JLabel(text, SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeBoldFont().deriveFont(Font.BOLD, 15f));
		label.setForeground(Color.WHITE);
		label.setAlignmentX(CENTER_ALIGNMENT);
		return label;
	}

	private static JLabel subsectionTitle(String text)
	{
		JLabel label = new JLabel(text, SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Color.WHITE);
		label.setAlignmentX(CENTER_ALIGNMENT);
		label.setPreferredSize(new Dimension(INNER_WIDTH, 22));
		label.setMinimumSize(new Dimension(0, 22));
		label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
		return label;
	}

	private static JTextArea infoLabel(String text)
	{
		JTextArea area = new JTextArea(text);
		area.setEditable(false);
		area.setFocusable(false);
		area.setOpaque(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setFont(FontManager.getRunescapeSmallFont());
		area.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		area.setAlignmentX(CENTER_ALIGNMENT);
		area.setBorder(BorderFactory.createEmptyBorder());
		int rows = Math.max(1, (text.length() + 31) / 32);
		int height = rows * 16 + 2;
		area.setRows(rows);
		area.setPreferredSize(new Dimension(INNER_WIDTH, height));
		area.setMinimumSize(new Dimension(0, height));
		area.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		return area;
	}

	private static String statusHtml(String text)
	{
		return "<html><div width='" + Math.max(150, CONTENT_WIDTH - 36) + "' align='center'>"
			+ escapeHtml(text) + "</div></html>";
	}

	private static String infoHtml(String text)
	{
		return "<html><div width='" + Math.max(150, INNER_WIDTH - 36) + "'>"
			+ escapeHtml(text) + "</div></html>";
	}

	private static String escapeHtml(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}
}
