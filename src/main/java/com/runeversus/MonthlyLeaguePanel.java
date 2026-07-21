package com.runeversus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.File;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

class MonthlyLeaguePanel extends JPanel
{
	private static final Color GOLD = new Color(255, 193, 7);
	private static final Color SILVER = new Color(205, 211, 222);
	private static final Color BRONZE = new Color(205, 135, 82);
	private static final Color TEAL = new Color(82, 201, 181);
	private static final Color RED = new Color(224, 104, 104);
	private static final Color GREEN = new Color(108, 207, 142);
	private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
	private static final String SCORE_TOOLTIP = "<html><b>League score</b><br>50% of the member's clan-relative EHP score plus 50% of their EHB score.<br>Collection Logs never affect the overall podium.</html>";
	private static final String EHP_TOOLTIP = "<html><b>EHP gained</b><br>Efficient Hours Played normalizes XP across skills using Wise Old Man rates.</html>";
	private static final String EHB_TOOLTIP = "<html><b>EHB gained</b><br>Efficient Hours Bossed normalizes boss KC using boss-specific kill rates.</html>";
	private static final String CLOG_TOOLTIP = "<html><b>Collection podium</b><br>New Collection Log slots are ranked separately because early-account slots are easier.<br>They do not affect League score.</html>";
	private static final String PROVISIONAL_TOOLTIP = "<html><b>Provisional</b><br>The member was not frozen into the opening roster, joined or started tracking late,<br>has missing dates, or lacks a fresh final snapshot. They remain visible but cannot enter the podium.</html>";

	private final Runnable closeCallback;
	private final Consumer<YearMonth> seasonCallback;
	private final Runnable refreshCallback;
	private final Consumer<MonthlyLeagueSeason> exportCallback;
	private final Runnable openCardCallback;
	private final JComboBox<SeasonOption> seasonSelector = new JComboBox<>();
	private final JComboBox<MonthlyLeagueMetric> viewSelector = new JComboBox<>(MonthlyLeagueMetric.values());
	private final JTextField searchField = new JTextField();
	private final JLabel stateBadge = new JLabel("WAITING", SwingConstants.CENTER);
	private final JLabel countdownLabel = new JLabel("Select a season", SwingConstants.CENTER);
	private final JLabel sourceStatus = new JLabel("No league data loaded.", SwingConstants.CENTER);
	private final JLabel trackedValue = summaryValue(GOLD);
	private final JLabel eligibleValue = summaryValue(GREEN);
	private final JLabel activeValue = summaryValue(SILVER);
	private final JLabel freshValue = summaryValue(TEAL);
	private final JLabel provisionalValue = summaryValue(BRONZE);
	private final PodiumSlot first = new PodiumSlot(1, GOLD);
	private final PodiumSlot second = new PodiumSlot(2, SILVER);
	private final PodiumSlot third = new PodiumSlot(3, BRONZE);
	private final JLabel skillingChampion = championValue();
	private final JLabel pvmChampion = championValue();
	private final JLabel collectionChampion = championValue();
	private final DefaultListModel<MonthlyLeagueStanding> listModel = new DefaultListModel<>();
	private final JList<MonthlyLeagueStanding> standingsList = new JList<>(listModel);
	private final JLabel visiblePlayers = new JLabel("0 shown", SwingConstants.RIGHT);
	private final JLabel scoreHeader = columnLabel("LEAGUE", SwingConstants.CENTER, SCORE_TOOLTIP);
	private final JButton refreshButton = new JButton("Refresh data");
	private final JButton exportButton = new JButton("Export podium card");
	private final JButton closeButton = new JButton("Close");
	private final JButton openCardButton = new JButton("Open saved card");
	private MonthlyLeagueSeason season;
	private boolean changingSeason;

	MonthlyLeaguePanel(
		Runnable closeCallback,
		Consumer<YearMonth> seasonCallback,
		Runnable refreshCallback,
		Consumer<MonthlyLeagueSeason> exportCallback,
		Runnable openCardCallback)
	{
		this.closeCallback = closeCallback;
		this.seasonCallback = seasonCallback;
		this.refreshCallback = refreshCallback;
		this.exportCallback = exportCallback;
		this.openCardCallback = openCardCallback;

		setLayout(new BorderLayout(0, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));
		populateSeasons(YearMonth.now(ZoneOffset.UTC));
		add(buildTop(), BorderLayout.NORTH);
		add(buildLeaderboard(), BorderLayout.CENTER);
		add(buildFooter(), BorderLayout.SOUTH);
		wireControls();
	}

	void setSeason(MonthlyLeagueSeason season)
	{
		this.season = season;
		selectMonth(season == null ? YearMonth.now(ZoneOffset.UTC) : season.getMonth());
		refreshButton.setEnabled(true);
		exportButton.setEnabled(season != null && season.getEligibleCount() > 0);
		refreshView();
	}

	void setLoading(YearMonth month, String message)
	{
		selectMonth(month);
		stateBadge.setText("LOADING");
		stateBadge.setForeground(GOLD);
		countdownLabel.setText("Building the live standings...");
		sourceStatus.setText(message);
		refreshButton.setEnabled(false);
		exportButton.setEnabled(false);
	}

	void setError(String message)
	{
		stateBadge.setText("UNAVAILABLE");
		stateBadge.setForeground(RED);
		sourceStatus.setText(message);
		refreshButton.setEnabled(true);
		exportButton.setEnabled(season != null && season.getEligibleCount() > 0);
	}

	void setExportedCard(File file)
	{
		openCardButton.setVisible(file != null && file.isFile());
	}

	YearMonth getSelectedMonth()
	{
		SeasonOption option = (SeasonOption) seasonSelector.getSelectedItem();
		return option == null ? YearMonth.now(ZoneOffset.UTC) : option.month;
	}

	int getDisplayedPlayerCount()
	{
		return listModel.size();
	}

	String getDisplayedPlayerName(int index)
	{
		return listModel.get(index).getName();
	}

	String getStateText()
	{
		return stateBadge.getText();
	}

	void selectView(MonthlyLeagueMetric metric)
	{
		viewSelector.setSelectedItem(metric);
	}

	private JPanel buildTop()
	{
		JPanel top = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
		JLabel title = new JLabel("MONTHLY LEAGUE", SwingConstants.CENTER);
		title.setFont(FontManager.getRunescapeBoldFont().deriveFont(24f));
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setAlignmentX(CENTER_ALIGNMENT);
		top.add(title);

		JLabel subtitle = new JLabel("One season. Two fair disciplines. A podium worth chasing.", SwingConstants.CENTER);
		subtitle.setFont(FontManager.getRunescapeSmallFont());
		subtitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		subtitle.setAlignmentX(CENTER_ALIGNMENT);
		top.add(subtitle);
		top.add(Box.createVerticalStrut(8));

		JPanel seasonBar = new JPanel(new GridLayout(1, 3, 10, 0));
		seasonBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		seasonBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		seasonBar.setAlignmentX(CENTER_ALIGNMENT);
		seasonBar.add(fieldRow("Season", seasonSelector));
		stateBadge.setFont(FontManager.getRunescapeBoldFont());
		stateBadge.setOpaque(true);
		stateBadge.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		stateBadge.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		seasonBar.add(stateBadge);
		countdownLabel.setFont(FontManager.getRunescapeSmallFont());
		countdownLabel.setForeground(SILVER);
		seasonBar.add(countdownLabel);
		top.add(seasonBar);
		top.add(Box.createVerticalStrut(4));

		sourceStatus.setFont(FontManager.getRunescapeSmallFont());
		sourceStatus.setForeground(SILVER);
		sourceStatus.setAlignmentX(CENTER_ALIGNMENT);
		sourceStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		top.add(sourceStatus);
		top.add(Box.createVerticalStrut(8));

		JPanel summary = new JPanel(new GridLayout(1, 5, 8, 0));
		summary.setBackground(ColorScheme.DARK_GRAY_COLOR);
		summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
		summary.add(summaryCell("TRACKED", trackedValue, "Players returned by Wise Old Man for this season."));
		summary.add(summaryCell("ELIGIBLE", eligibleValue, "Members tracked within the first 72 hours of the season."));
		summary.add(summaryCell("ACTIVE", activeValue, "Eligible members with EHP, EHB, or CLog gains."));
		summary.add(summaryCell("FRESH", freshValue, "Members with a recent WOM snapshot."));
		summary.add(summaryCell("PROVISIONAL ?", provisionalValue, PROVISIONAL_TOOLTIP));
		top.add(summary);
		top.add(Box.createVerticalStrut(9));

		JPanel podium = new JPanel(new GridLayout(1, 3, 10, 0));
		podium.setBackground(ColorScheme.DARK_GRAY_COLOR);
		podium.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
		podium.add(second.panel);
		podium.add(first.panel);
		podium.add(third.panel);
		top.add(podium);
		top.add(Box.createVerticalStrut(8));

		JPanel champions = new JPanel(new GridLayout(1, 3, 10, 0));
		champions.setBackground(ColorScheme.DARK_GRAY_COLOR);
		champions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
		champions.add(championCard("SKILLING CHAMPION", skillingChampion, GOLD, EHP_TOOLTIP));
		champions.add(championCard("PVM CHAMPION", pvmChampion, RED, EHB_TOOLTIP));
		champions.add(championCard("COLLECTION SPOTLIGHT", collectionChampion, TEAL, CLOG_TOOLTIP));
		top.add(champions);
		top.add(Box.createVerticalStrut(8));

		JLabel rules = new JLabel(
			"<html><center><b>League score</b> = 50% Skilling rank + 50% PvM rank &nbsp;•&nbsp; CLogs have a separate podium</center></html>",
			SwingConstants.CENTER);
		rules.setFont(FontManager.getRunescapeSmallFont());
		rules.setForeground(SILVER);
		rules.setToolTipText(SCORE_TOOLTIP);
		rules.setAlignmentX(CENTER_ALIGNMENT);
		top.add(rules);
		top.add(Box.createVerticalStrut(8));

		JPanel filters = new JPanel(new GridLayout(1, 2, 10, 0));
		filters.setBackground(ColorScheme.DARK_GRAY_COLOR);
		filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		filters.add(fieldRow("Rank by", viewSelector));
		filters.add(fieldRow("Find", searchField));
		top.add(filters);
		return top;
	}

	private JPanel buildLeaderboard()
	{
		standingsList.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		standingsList.setForeground(Color.WHITE);
		standingsList.setSelectionBackground(ColorScheme.MEDIUM_GRAY_COLOR);
		standingsList.setFixedCellHeight(50);
		standingsList.setCellRenderer(new StandingRenderer());
		JScrollPane scroll = new JScrollPane(standingsList);
		scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));

		JPanel heading = new JPanel(new BorderLayout());
		heading.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel title = new JLabel("SEASON STANDINGS");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		visiblePlayers.setFont(FontManager.getRunescapeSmallFont());
		visiblePlayers.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		heading.add(title, BorderLayout.WEST);
		heading.add(visiblePlayers, BorderLayout.EAST);

		JPanel header = rowPanel();
		addCell(header, columnLabel("RANK", SwingConstants.CENTER), 0, 0.45);
		addCell(header, columnLabel("PLAYER", SwingConstants.LEFT), 1, 1.8);
		addCell(header, scoreHeader, 2, 0.9);
		addCell(header, columnLabel("EHP", SwingConstants.CENTER, EHP_TOOLTIP), 3, 0.9);
		addCell(header, columnLabel("EHB", SwingConstants.CENTER, EHB_TOOLTIP), 4, 0.9);
		addCell(header, columnLabel("CLOGS", SwingConstants.CENTER, CLOG_TOOLTIP), 5, 0.8);
		addCell(header, columnLabel("STATUS", SwingConstants.CENTER, PROVISIONAL_TOOLTIP), 6, 1.0);

		JPanel headerBlock = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
		headerBlock.add(heading);
		headerBlock.add(Box.createVerticalStrut(4));
		headerBlock.add(header);
		JPanel container = new JPanel(new BorderLayout(0, 4));
		container.setBackground(ColorScheme.DARK_GRAY_COLOR);
		container.add(headerBlock, BorderLayout.NORTH);
		container.add(scroll, BorderLayout.CENTER);
		return container;
	}

	private JPanel buildFooter()
	{
		JPanel footer = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
		JPanel actions = new JPanel(new GridLayout(1, 3, 8, 0));
		actions.setBackground(ColorScheme.DARK_GRAY_COLOR);
		actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
		exportButton.setEnabled(false);
		exportButton.addActionListener(event ->
		{
			if (season != null)
			{
				exportCallback.accept(season);
			}
		});
		refreshButton.addActionListener(event -> refreshCallback.run());
		closeButton.addActionListener(event -> closeCallback.run());
		actions.add(exportButton);
		actions.add(refreshButton);
		actions.add(closeButton);
		footer.add(actions);
		footer.add(Box.createVerticalStrut(3));

		openCardButton.setVisible(false);
		openCardButton.setBorderPainted(false);
		openCardButton.setContentAreaFilled(false);
		openCardButton.setForeground(ColorScheme.BRAND_ORANGE);
		openCardButton.setFont(FontManager.getRunescapeSmallFont());
		openCardButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		openCardButton.setAlignmentX(CENTER_ALIGNMENT);
		openCardButton.addActionListener(event -> openCardCallback.run());
		footer.add(openCardButton);
		return footer;
	}

	private void wireControls()
	{
		seasonSelector.addActionListener(event ->
		{
			if (!changingSeason)
			{
				seasonCallback.accept(getSelectedMonth());
			}
		});
		viewSelector.addActionListener(event -> refreshList());
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				refreshList();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				refreshList();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				refreshList();
			}
		});
	}

	private void refreshView()
	{
		if (season == null)
		{
			listModel.clear();
			return;
		}
		stateBadge.setText(season.isLive() ? "LIVE" : "FINAL");
		stateBadge.setForeground(season.isLive() ? GREEN : GOLD);
		countdownLabel.setText(season.isLive() ? countdown(season.getTimeRemaining()) : "Season completed");
		sourceStatus.setText(season.getLabel() + "  |  WOM group #" + season.getGroupId()
			+ (season.isFinalized() ? "  |  Sealed " : "  |  Updated ")
			+ DateTimeFormatter.ofPattern("dd MMM, HH:mm")
			.withZone(ZoneOffset.UTC).format(season.getGeneratedAt()) + " UTC");
		refreshButton.setEnabled(season.isLive());
		refreshButton.setToolTipText(season.isLive()
			? "Refresh the current season within the WOM request limits"
			: "Final standings are sealed locally and cannot be recalculated");
		trackedValue.setText(String.valueOf(season.getStandings().size()));
		eligibleValue.setText(String.valueOf(season.getEligibleCount()));
		activeValue.setText(String.valueOf(season.getStandings().stream()
			.filter(MonthlyLeagueStanding::isEligible).filter(MonthlyLeagueStanding::isActive).count()));
		freshValue.setText(season.getFreshCount() + " / " + season.getStandings().size());
		provisionalValue.setText(String.valueOf(season.getProvisionalCount()));

		List<MonthlyLeagueStanding> overall = eligibleWithActivity(season.rankedBy(MonthlyLeagueMetric.OVERALL));
		first.set(overall.size() > 0 ? overall.get(0) : null);
		second.set(overall.size() > 1 ? overall.get(1) : null);
		third.set(overall.size() > 2 ? overall.get(2) : null);
		setChampion(skillingChampion, season.getChampion(MonthlyLeagueMetric.SKILLING), MonthlyLeagueMetric.SKILLING);
		setChampion(pvmChampion, season.getChampion(MonthlyLeagueMetric.PVM), MonthlyLeagueMetric.PVM);
		setChampion(collectionChampion, season.getChampion(MonthlyLeagueMetric.COLLECTION), MonthlyLeagueMetric.COLLECTION);
		refreshList();
	}

	private void refreshList()
	{
		listModel.clear();
		if (season == null)
		{
			visiblePlayers.setText("0 shown");
			return;
		}
		MonthlyLeagueMetric metric = selectedMetric();
		String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
		for (MonthlyLeagueStanding standing : season.rankedBy(metric))
		{
			if (query.isEmpty() || standing.getName().toLowerCase(Locale.ROOT).contains(query))
			{
				listModel.addElement(standing);
			}
		}
		scoreHeader.setText(metric == MonthlyLeagueMetric.OVERALL ? "LEAGUE" : metric.getLabel().toUpperCase(Locale.ROOT));
		visiblePlayers.setText(listModel.size() + " shown");
	}

	private MonthlyLeagueMetric selectedMetric()
	{
		MonthlyLeagueMetric metric = (MonthlyLeagueMetric) viewSelector.getSelectedItem();
		return metric == null ? MonthlyLeagueMetric.OVERALL : metric;
	}

	private void populateSeasons(YearMonth current)
	{
		changingSeason = true;
		seasonSelector.removeAllItems();
		for (int offset = 0; offset < 12; offset++)
		{
			seasonSelector.addItem(new SeasonOption(current.minusMonths(offset), offset == 0));
		}
		changingSeason = false;
	}

	private void selectMonth(YearMonth month)
	{
		changingSeason = true;
		for (int index = 0; index < seasonSelector.getItemCount(); index++)
		{
			SeasonOption option = seasonSelector.getItemAt(index);
			if (option.month.equals(month))
			{
				seasonSelector.setSelectedIndex(index);
				changingSeason = false;
				return;
			}
		}
		seasonSelector.insertItemAt(new SeasonOption(month, false), 0);
		seasonSelector.setSelectedIndex(0);
		changingSeason = false;
	}

	private static List<MonthlyLeagueStanding> eligibleWithActivity(List<MonthlyLeagueStanding> input)
	{
		List<MonthlyLeagueStanding> out = new ArrayList<>();
		for (MonthlyLeagueStanding standing : input)
		{
			if (standing.isEligible() && standing.getOverallScore() > 0.0)
			{
				out.add(standing);
			}
		}
		return out;
	}

	private static void setChampion(
		JLabel label,
		MonthlyLeagueStanding standing,
		MonthlyLeagueMetric metric)
	{
		if (standing == null)
		{
			label.setText("No activity");
			return;
		}
		String value;
		switch (metric)
		{
			case SKILLING:
				value = formatHours(standing.getEhpGained()) + " EHP";
				break;
			case PVM:
				value = formatHours(standing.getEhbGained()) + " EHB";
				break;
			case COLLECTION:
				value = "+" + standing.getCollectionsGained() + " CLogs";
				break;
			default:
				value = formatScore(standing.getOverallScore());
		}
		label.setText("<html><center><b>" + escapeHtml(standing.getName()) + "</b><br>" + value + "</center></html>");
	}

	private static String countdown(Duration remaining)
	{
		long days = remaining.toDays();
		long hours = remaining.minusDays(days).toHours();
		return days + "d " + hours + "h remaining";
	}

	private static JPanel summaryCell(String title, JLabel value, String tooltip)
	{
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			BorderFactory.createEmptyBorder(6, 4, 5, 4)));
		JLabel heading = new JLabel(title, SwingConstants.CENTER);
		heading.setFont(FontManager.getRunescapeSmallFont());
		heading.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		heading.setAlignmentX(CENTER_ALIGNMENT);
		panel.add(heading);
		panel.add(value);
		panel.setToolTipText(tooltip);
		heading.setToolTipText(tooltip);
		value.setToolTipText(tooltip);
		return panel;
	}

	private static JPanel championCard(String title, JLabel value, Color color, String tooltip)
	{
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createMatteBorder(3, 1, 1, 1, color));
		JLabel heading = new JLabel(title, SwingConstants.CENTER);
		heading.setFont(FontManager.getRunescapeSmallFont());
		heading.setForeground(color);
		heading.setAlignmentX(CENTER_ALIGNMENT);
		panel.add(Box.createVerticalStrut(5));
		panel.add(heading);
		panel.add(value);
		panel.setToolTipText(tooltip);
		heading.setToolTipText(tooltip);
		value.setToolTipText(tooltip);
		return panel;
	}

	private static JLabel summaryValue(Color color)
	{
		JLabel label = new JLabel("—", SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
		label.setForeground(color);
		label.setAlignmentX(CENTER_ALIGNMENT);
		return label;
	}

	private static JLabel championValue()
	{
		JLabel label = new JLabel("No activity", SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(Color.WHITE);
		label.setAlignmentX(CENTER_ALIGNMENT);
		return label;
	}

	private static JPanel fieldRow(String labelText, Component field)
	{
		JPanel panel = new JPanel(new BorderLayout(8, 0));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel label = new JLabel(labelText);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setPreferredSize(new Dimension(58, 28));
		panel.add(label, BorderLayout.WEST);
		panel.add(field, BorderLayout.CENTER);
		return panel;
	}

	private static JLabel columnLabel(String text, int alignment)
	{
		return columnLabel(text, alignment, null);
	}

	private static JLabel columnLabel(String text, int alignment, String tooltip)
	{
		JLabel label = new JLabel(text, alignment);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setToolTipText(tooltip);
		return label;
	}

	private static JPanel verticalPanel(Color color)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(color);
		return panel;
	}

	private static JPanel rowPanel()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));
		return panel;
	}

	private static void addCell(JPanel panel, Component component, int x, double weight)
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = 0;
		constraints.weightx = weight;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		panel.add(component, constraints);
	}

	private static String formatHours(double value)
	{
		return String.format(Locale.ROOT, value >= 100.0 ? "%.0f" : "%.2f", value);
	}

	private static String formatScore(double value)
	{
		return String.format(Locale.ROOT, "%.1f pts", value);
	}

	private static String escapeHtml(String value)
	{
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private final class StandingRenderer extends JLabel implements ListCellRenderer<MonthlyLeagueStanding>
	{
		private StandingRenderer()
		{
			setOpaque(true);
			setFont(FontManager.getRunescapeSmallFont());
			setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));
		}

		@Override
		public Component getListCellRendererComponent(
			JList<? extends MonthlyLeagueStanding> list,
			MonthlyLeagueStanding standing,
			int index,
			boolean selected,
			boolean focused)
		{
			setBackground(selected ? ColorScheme.MEDIUM_GRAY_COLOR
				: index % 2 == 0 ? ColorScheme.DARKER_GRAY_COLOR : new Color(35, 35, 35));
			int selectedRank = standing.rankFor(selectedMetric());
			double displayedScore = selectedMetric() == MonthlyLeagueMetric.SKILLING
				? standing.getSkillingScore() : selectedMetric() == MonthlyLeagueMetric.PVM
				? standing.getPvmScore() : standing.getOverallScore();
			String displayedRank = selectedRank == 0 ? "—" : "#" + selectedRank;
			String displayedLeague = standing.isEligible() && selectedMetric() != MonthlyLeagueMetric.COLLECTION
				? String.format(Locale.ROOT, "%.1f", displayedScore) : "—";
			String statusText = standing.isEligible() ? standing.isFresh() ? "Ready" : "Update due" : "Provisional";
			Color statusColor = standing.isEligible() ? standing.isFresh() ? GREEN : SILVER : BRONZE;
			int width = Math.max(860, list.getWidth() - 28);
			setText("<html><table width='" + width + "' cellspacing='0'><tr>"
				+ cell(displayedRank, 8, "center", rankColor(selectedRank), true)
				+ cell(escapeHtml(standing.getName()), 25, "left",
					standing.isEligible() ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR, true)
				+ cell(displayedLeague, 12, "center", GOLD, true)
				+ cell(formatHours(standing.getEhpGained()), 12, "center", SILVER, false)
				+ cell(formatHours(standing.getEhbGained()), 12, "center", RED, false)
				+ cell("+" + standing.getCollectionsGained(), 10, "center", TEAL, false)
				+ cell(statusText, 21, "center", statusColor, false)
				+ "</tr></table></html>");
			setToolTipText(standing.isEligible() ? "Eligible for this season" : PROVISIONAL_TOOLTIP);
			return this;
		}

		private String cell(String value, int width, String alignment, Color color, boolean bold)
		{
			return "<td width='" + width + "%' align='" + alignment + "'><font color='"
				+ htmlColor(color) + "'>" + (bold ? "<b>" : "") + value
				+ (bold ? "</b>" : "") + "</font></td>";
		}
	}

	private static Color rankColor(int rank)
	{
		if (rank == 1)
		{
			return GOLD;
		}
		if (rank == 2)
		{
			return SILVER;
		}
		if (rank == 3)
		{
			return BRONZE;
		}
		return Color.WHITE;
	}

	private static String htmlColor(Color color)
	{
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private static final class SeasonOption
	{
		private final YearMonth month;
		private final boolean live;

		private SeasonOption(YearMonth month, boolean live)
		{
			this.month = month;
			this.live = live;
		}

		@Override
		public String toString()
		{
			String value = month.format(MONTH_FORMAT);
			value = value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
			return live ? value + " · Live" : value;
		}
	}

	private static final class PodiumSlot
	{
		private final JPanel panel;
		private final int fallbackPlace;
		private final JLabel placeLabel = new JLabel("", SwingConstants.CENTER);
		private final JLabel name = new JLabel("Waiting for data", SwingConstants.CENTER);
		private final JLabel score = new JLabel("—", SwingConstants.CENTER);

		private PodiumSlot(int place, Color color)
		{
			fallbackPlace = place;
			panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
			panel.setBorder(BorderFactory.createMatteBorder(4, 1, 1, 1, color));
			placeLabel.setText(placeLabel(place));
			placeLabel.setFont(FontManager.getRunescapeBoldFont());
			placeLabel.setForeground(color);
			placeLabel.setAlignmentX(CENTER_ALIGNMENT);
			name.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
			name.setForeground(Color.WHITE);
			name.setAlignmentX(CENTER_ALIGNMENT);
			score.setFont(FontManager.getRunescapeSmallFont());
			score.setForeground(color);
			score.setAlignmentX(CENTER_ALIGNMENT);
			panel.add(Box.createVerticalStrut(6));
			panel.add(placeLabel);
			panel.add(Box.createVerticalStrut(3));
			panel.add(name);
			panel.add(score);
		}

		private void set(MonthlyLeagueStanding standing)
		{
			placeLabel.setText(placeLabel(standing == null ? fallbackPlace : standing.getOverallRank()));
			if (standing == null)
			{
				name.setText("No contender");
				score.setText("—");
				return;
			}
			name.setText(standing.getName());
			score.setText(formatScore(standing.getOverallScore()) + "  ·  "
				+ formatHours(standing.getEhpGained()) + " EHP  ·  "
				+ formatHours(standing.getEhbGained()) + " EHB");
		}

		private static String placeLabel(int rank)
		{
			if (rank == 1)
			{
				return "1ST";
			}
			if (rank == 2)
			{
				return "2ND";
			}
			if (rank == 3)
			{
				return "3RD";
			}
			return rank + "TH";
		}
	}
}
