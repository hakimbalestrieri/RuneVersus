package com.runeversus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListCellRenderer;
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

class ClanProgressPanel extends JPanel
{
	private static final String ALL_BOSSES = "All bosses";
	private static final DateTimeFormatter UPDATED_FORMAT =
		DateTimeFormatter.ofPattern("dd MMM, HH:mm").withZone(ZoneId.systemDefault());
	private static final Color GOLD = new Color(246, 197, 92);
	private static final Color SILVER = new Color(205, 211, 222);
	private static final Color BRONZE = new Color(205, 135, 82);
	private static final Color TEAL = new Color(82, 201, 181);
	private static final Color RED = new Color(224, 104, 104);
	private static final String TRACKED_TOOLTIP = "<html><b>Tracked</b><br>Members with Wise Old Man data returned for this group.<br>Each member is counted once.</html>";
	private static final String ACTIVE_TOOLTIP = "<html><b>Active</b><br>For a gain period: members who gained XP, unlocked a CLog,<br>or gained Boss KC. All-time uses non-zero totals.</html>";
	private static final String CLOG_TOOLTIP = "<html><b>CLogs</b><br>Collection-log unlocks summed across all members.<br><b>+3</b> means three unlocks in total, not necessarily three unique items.</html>";
	private static final String AVERAGE_TOOLTIP = "<html><b>Average active XP</b><br>Total clan XP divided by Active members.<br>Members with no tracked activity are excluded.</html>";

	private final Runnable closeCallback;
	private final Runnable refreshCallback;
	private final Consumer<ClanProgressLeaderboard> exportCallback;
	private final Runnable openCardCallback;
	private final RuneVersusIcons icons;
	private final JComboBox<GainPeriod> periodSelector = new JComboBox<>(GainPeriod.values());
	private final JComboBox<ClanProgressMetric> sortSelector = new JComboBox<>(ClanProgressMetric.values());
	private final JComboBox<String> bossSelector = new JComboBox<>();
	private final JTextField searchField = new JTextField();
	private final JLabel sourceStatus = new JLabel("No clan data loaded.", SwingConstants.CENTER);
	private final JLabel membersValue = summaryValue(GOLD);
	private final JLabel activeValue = summaryValue(SILVER);
	private final JLabel xpValue = summaryValue(GOLD);
	private final JLabel clogValue = summaryValue(TEAL);
	private final JLabel bossValue = summaryValue(RED);
	private final JLabel averageValue = summaryValue(SILVER);
	private final JLabel xpLeader = leaderValue();
	private final JLabel clogLeader = leaderValue();
	private final JLabel bossLeader = leaderValue();
	private final JLabel visibleMembers = new JLabel("0 shown", SwingConstants.RIGHT);
	private final JLabel bossColumnLabel = columnLabel("ALL BOSS KC", SwingConstants.CENTER);
	private final DefaultListModel<ClanProgressPlayer> listModel = new DefaultListModel<>();
	private final JList<ClanProgressPlayer> memberList = new JList<>(listModel);
	private final JButton refreshButton = new JButton("Refresh data");
	private final JButton closeButton = new JButton("Close");
	private final JButton exportButton = new JButton("Export five-period card");
	private final JButton openCardButton = new JButton("Open saved card");
	private JPanel summaryPanel;
	private JPanel leadersPanel;
	private JPanel columnHeader;
	private JPanel trackedSummaryCard;
	private JPanel activeSummaryCard;
	private JPanel xpSummaryCard;
	private JPanel clogSummaryCard;
	private JPanel bossSummaryCard;
	private JPanel averageSummaryCard;
	private JPanel xpLeaderCard;
	private JPanel clogLeaderCard;
	private JPanel bossLeaderCard;

	private ClanProgressLeaderboard leaderboard;

	ClanProgressPanel(
		Runnable closeCallback,
		Runnable refreshCallback,
		Consumer<ClanProgressLeaderboard> exportCallback,
		Runnable openCardCallback)
	{
		this(closeCallback, refreshCallback, exportCallback, openCardCallback, RuneVersusIcons.empty());
	}

	ClanProgressPanel(
		Runnable closeCallback,
		Runnable refreshCallback,
		Consumer<ClanProgressLeaderboard> exportCallback,
		Runnable openCardCallback,
		RuneVersusIcons icons)
	{
		this.closeCallback = closeCallback;
		this.refreshCallback = refreshCallback;
		this.exportCallback = exportCallback;
		this.openCardCallback = openCardCallback;
		this.icons = icons;

		setLayout(new BorderLayout(0, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
		add(buildTop(), BorderLayout.NORTH);
		add(buildMemberTable(), BorderLayout.CENTER);
		add(buildFooter(), BorderLayout.SOUTH);
		wireControls();
	}

	void setLeaderboard(ClanProgressLeaderboard leaderboard)
	{
		this.leaderboard = leaderboard;
		populateBossSelector(leaderboard == null ? BossKcRegistry.allNames() : leaderboard.getBossNames());
		refreshButton.setEnabled(true);
		exportButton.setEnabled(leaderboard != null && !leaderboard.getPlayers().isEmpty());
		refreshView();
	}

	void setLoading(String message)
	{
		sourceStatus.setText(message);
		refreshButton.setEnabled(false);
		exportButton.setEnabled(false);
	}

	void setError(String message)
	{
		sourceStatus.setText(message);
		refreshButton.setEnabled(true);
		exportButton.setEnabled(leaderboard != null && !leaderboard.getPlayers().isEmpty());
	}

	void setExportedCard(File file)
	{
		openCardButton.setVisible(file != null && file.isFile());
	}

	int getDisplayedPlayerCount()
	{
		return listModel.size();
	}

	String getDisplayedPlayerName(int index)
	{
		return listModel.get(index).getName();
	}

	String getTrackedTooltip()
	{
		return membersValue.getToolTipText();
	}

	String getActiveTooltip()
	{
		return activeValue.getToolTipText();
	}

	String getClogTooltip()
	{
		return clogValue.getToolTipText();
	}

	String getAverageTooltip()
	{
		return averageValue.getToolTipText();
	}

	void selectPeriod(GainPeriod period)
	{
		periodSelector.setSelectedItem(period);
	}

	void selectSortMetric(ClanProgressMetric metric)
	{
		sortSelector.setSelectedItem(metric);
	}

	void setSearchText(String text)
	{
		searchField.setText(text);
	}

	void selectBoss(String bossName)
	{
		bossSelector.setSelectedItem(bossName == null ? ALL_BOSSES : bossName);
	}

	int getBossSelectorCount()
	{
		return bossSelector.getItemCount();
	}

	long getDisplayedBossKc(int index)
	{
		return listModel.get(index).getGains(selectedPeriod()).getBossKc(selectedBoss());
	}

	int getLeaderboardColumnCount()
	{
		return columnHeader.getComponentCount();
	}

	private JPanel buildTop()
	{
		JPanel top = verticalPanel(ColorScheme.DARK_GRAY_COLOR);

		JLabel performanceTitle = new JLabel("CLAN PERFORMANCE", SwingConstants.CENTER);
		performanceTitle.setFont(FontManager.getRunescapeBoldFont().deriveFont(22f));
		performanceTitle.setForeground(ColorScheme.BRAND_ORANGE);
		performanceTitle.setAlignmentX(CENTER_ALIGNMENT);
		top.add(performanceTitle);

		JLabel subtitle = new JLabel("Every member. Every period. One clear leaderboard.", SwingConstants.CENTER);
		subtitle.setFont(FontManager.getRunescapeSmallFont());
		subtitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		subtitle.setAlignmentX(CENTER_ALIGNMENT);
		top.add(subtitle);
		top.add(Box.createVerticalStrut(3));

		sourceStatus.setFont(FontManager.getRunescapeSmallFont());
		sourceStatus.setForeground(SILVER);
		sourceStatus.setAlignmentX(CENTER_ALIGNMENT);
		sourceStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
		top.add(sourceStatus);
		top.add(Box.createVerticalStrut(8));

		JPanel filters = new JPanel(new GridLayout(1, 4, 10, 0));
		filters.setBackground(ColorScheme.DARK_GRAY_COLOR);
		filters.setAlignmentX(CENTER_ALIGNMENT);
		filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
		filters.add(fieldRow("Period", periodSelector));
		filters.add(fieldRow("Rank by", sortSelector));
		filters.add(fieldRow("Boss", bossSelector));
		filters.add(fieldRow("Find", searchField));
		top.add(filters);
		top.add(Box.createVerticalStrut(9));

		summaryPanel = new JPanel();
		summaryPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		summaryPanel.setAlignmentX(CENTER_ALIGNMENT);
		summaryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
		trackedSummaryCard = summaryCell("TRACKED ?", membersValue, TRACKED_TOOLTIP);
		activeSummaryCard = summaryCell("ACTIVE ?", activeValue, ACTIVE_TOOLTIP);
		xpSummaryCard = summaryCell("TOTAL XP", xpValue, null, RuneVersusIcons.Kind.XP);
		clogSummaryCard = summaryCell("CLOGS ?", clogValue, CLOG_TOOLTIP, RuneVersusIcons.Kind.COLLECTION_LOG);
		bossSummaryCard = summaryCell("BOSS KC", bossValue, null, RuneVersusIcons.Kind.PVM);
		averageSummaryCard = summaryCell("AVG ACTIVE XP ?", averageValue, AVERAGE_TOOLTIP);
		top.add(summaryPanel);
		top.add(Box.createVerticalStrut(9));

		leadersPanel = new JPanel();
		leadersPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		leadersPanel.setAlignmentX(CENTER_ALIGNMENT);
		leadersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
		xpLeaderCard = leaderCard("XP CHAMPION", xpLeader, GOLD);
		clogLeaderCard = leaderCard("CLOG CHAMPION", clogLeader, TEAL, CLOG_TOOLTIP);
		bossLeaderCard = leaderCard("BOSS KC CHAMPION", bossLeader, RED);
		showOverviewCards();
		top.add(leadersPanel);
		return top;
	}

	private void showOverviewCards()
	{
		summaryPanel.removeAll();
		summaryPanel.setLayout(new GridLayout(1, 6, 7, 0));
		summaryPanel.add(trackedSummaryCard);
		summaryPanel.add(activeSummaryCard);
		summaryPanel.add(xpSummaryCard);
		summaryPanel.add(clogSummaryCard);
		summaryPanel.add(bossSummaryCard);
		summaryPanel.add(averageSummaryCard);

		leadersPanel.removeAll();
		leadersPanel.setLayout(new GridLayout(1, 3, 9, 0));
		leadersPanel.add(xpLeaderCard);
		leadersPanel.add(clogLeaderCard);
		leadersPanel.add(bossLeaderCard);
	}

	private void updateFocusedBossMode(String bossName)
	{
		boolean focused = bossName != null;
		sortSelector.setEnabled(!focused);
		if (focused)
		{
			summaryPanel.removeAll();
			summaryPanel.setLayout(new GridLayout(1, 3, 9, 0));
			summaryPanel.add(trackedSummaryCard);
			summaryPanel.add(activeSummaryCard);
			summaryPanel.add(bossSummaryCard);

			leadersPanel.removeAll();
			leadersPanel.setLayout(new GridLayout(1, 1));
			leadersPanel.add(bossLeaderCard);
		}
		else
		{
			showOverviewCards();
		}

		columnHeader.removeAll();
		columnHeader.setLayout(new GridLayout(1, focused ? 2 : 4));
		columnHeader.add(columnLabel("RANK & MEMBER", SwingConstants.LEFT));
		if (focused)
		{
			bossColumnLabel.setText(bossName.toUpperCase(Locale.ROOT) + " KC");
			bossColumnLabel.setIcon(icons.bossIcon(bossName, 18, bossColumnLabel));
			bossColumnLabel.setIconTextGap(6);
			columnHeader.add(bossColumnLabel);
		}
		else
		{
			columnHeader.add(columnLabel("XP", SwingConstants.CENTER));
			columnHeader.add(columnLabel("CLOGS", SwingConstants.CENTER, CLOG_TOOLTIP));
			bossColumnLabel.setText("ALL BOSS KC");
			bossColumnLabel.setIcon(icons.icon(RuneVersusIcons.Kind.PVM, 18, bossColumnLabel));
			bossColumnLabel.setIconTextGap(6);
			columnHeader.add(bossColumnLabel);
		}

		summaryPanel.revalidate();
		leadersPanel.revalidate();
		columnHeader.revalidate();
		repaint();
	}

	private JPanel buildMemberTable()
	{
		memberList.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		memberList.setForeground(Color.WHITE);
		memberList.setSelectionBackground(ColorScheme.MEDIUM_GRAY_COLOR);
		memberList.setFixedCellHeight(46);
		memberList.setCellRenderer(new MemberRenderer());
		JScrollPane scroll = new JScrollPane(memberList);
		scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));

		JPanel container = new JPanel(new BorderLayout(0, 4));
		container.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JPanel heading = new JPanel(new BorderLayout());
		heading.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel title = new JLabel("MEMBER LEADERBOARD");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		visibleMembers.setFont(FontManager.getRunescapeSmallFont());
		visibleMembers.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		heading.add(title, BorderLayout.WEST);
		heading.add(visibleMembers, BorderLayout.EAST);

		columnHeader = new JPanel(new GridLayout(1, 4));
		columnHeader.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		columnHeader.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
		columnHeader.add(columnLabel("RANK & MEMBER", SwingConstants.LEFT));
		columnHeader.add(columnLabel("XP", SwingConstants.CENTER));
		columnHeader.add(columnLabel("CLOGS", SwingConstants.CENTER, CLOG_TOOLTIP));
		columnHeader.add(bossColumnLabel);

		JPanel headerBlock = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
		headerBlock.add(heading);
		headerBlock.add(Box.createVerticalStrut(4));
		headerBlock.add(columnHeader);
		container.add(headerBlock, BorderLayout.NORTH);
		container.add(scroll, BorderLayout.CENTER);
		return container;
	}

	private JPanel buildFooter()
	{
		JPanel footer = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
		JPanel actions = new JPanel(new GridLayout(1, 3, 8, 0));
		actions.setBackground(ColorScheme.DARK_GRAY_COLOR);
		actions.setAlignmentX(CENTER_ALIGNMENT);
		actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		closeButton.addActionListener(e -> closeCallback.run());
		refreshButton.addActionListener(e -> refreshCallback.run());
		exportButton.setEnabled(false);
		exportButton.addActionListener(e ->
		{
			if (leaderboard != null)
			{
				exportCallback.accept(leaderboard);
			}
		});
		actions.add(closeButton);
		actions.add(exportButton);
		actions.add(refreshButton);
		footer.add(actions);
		footer.add(Box.createVerticalStrut(2));

		openCardButton.setVisible(false);
		openCardButton.setBorderPainted(false);
		openCardButton.setContentAreaFilled(false);
		openCardButton.setForeground(ColorScheme.BRAND_ORANGE);
		openCardButton.setFont(FontManager.getRunescapeSmallFont());
		openCardButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		openCardButton.setAlignmentX(CENTER_ALIGNMENT);
		openCardButton.addActionListener(e -> openCardCallback.run());
		footer.add(openCardButton);
		return footer;
	}

	private void wireControls()
	{
		bossSelector.setMaximumRowCount(18);
		bossSelector.setToolTipText("Choose a boss to compare every clan member's KC");
		bossSelector.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(
				JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				JLabel label = (JLabel) super.getListCellRendererComponent(
					list, value, index, isSelected, cellHasFocus);
				String bossName = value instanceof String ? (String) value : null;
				label.setIcon(bossName == null || ALL_BOSSES.equals(bossName)
					? icons.icon(RuneVersusIcons.Kind.PVM, 18, bossSelector)
					: icons.bossIcon(bossName, 18, bossSelector));
				label.setIconTextGap(6);
				return label;
			}
		});
		sortSelector.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(
				JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				JLabel label = (JLabel) super.getListCellRendererComponent(
					list, value, index, isSelected, cellHasFocus);
				ClanProgressMetric metric = value instanceof ClanProgressMetric
					? (ClanProgressMetric) value : ClanProgressMetric.XP;
				label.setIcon(icons.icon(metricIcon(metric), 18, sortSelector));
				label.setIconTextGap(6);
				return label;
			}
		});
		periodSelector.setSelectedItem(GainPeriod.DAY);
		sortSelector.setSelectedItem(ClanProgressMetric.XP);
		populateBossSelector(BossKcRegistry.allNames());
		periodSelector.addActionListener(e -> refreshView());
		sortSelector.addActionListener(e -> refreshList());
		bossSelector.addActionListener(e ->
		{
			if (selectedBoss() != null && sortSelector.getSelectedItem() != ClanProgressMetric.BOSS_KC)
			{
				sortSelector.setSelectedItem(ClanProgressMetric.BOSS_KC);
			}
			refreshView();
		});
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

	private void populateBossSelector(List<String> bossNames)
	{
		String selected = bossSelector.getSelectedItem() instanceof String
			? (String) bossSelector.getSelectedItem() : ALL_BOSSES;
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		model.addElement(ALL_BOSSES);
		for (String bossName : bossNames)
		{
			model.addElement(bossName);
		}
		bossSelector.setModel(model);
		if (model.getIndexOf(selected) >= 0)
		{
			bossSelector.setSelectedItem(selected);
		}
		else
		{
			bossSelector.setSelectedItem(ALL_BOSSES);
		}
	}

	private static RuneVersusIcons.Kind metricIcon(ClanProgressMetric metric)
	{
		switch (metric)
		{
			case COLLECTIONS:
				return RuneVersusIcons.Kind.COLLECTION_LOG;
			case BOSS_KC:
				return RuneVersusIcons.Kind.PVM;
			case XP:
			default:
				return RuneVersusIcons.Kind.XP;
		}
	}

	private void refreshView()
	{
		if (leaderboard == null)
		{
			listModel.clear();
			return;
		}

		GainPeriod period = selectedPeriod();
		String bossName = selectedBoss();
		updateFocusedBossMode(bossName);
		long totalXp = 0L;
		long totalClogs = 0L;
		long totalBossKc = 0L;
		int active = 0;
		for (ClanProgressPlayer player : leaderboard.getPlayers())
		{
			ClanProgressGains gains = player.getGains(period);
			totalXp += gains.getXp();
			totalClogs += gains.getCollections();
			totalBossKc += gains.getBossKc(bossName);
			if (bossName == null
				? gains.getXp() > 0L || gains.getCollections() > 0L || gains.getBossKc() > 0L
				: gains.getBossKc(bossName) > 0L)
			{
				active++;
			}
		}

		long averageXp = active == 0 ? 0L : totalXp / active;
		membersValue.setText(String.valueOf(leaderboard.getPlayers().size()));
		activeValue.setText(active + " / " + leaderboard.getPlayers().size());
		xpValue.setText(valueText(period, totalXp));
		clogValue.setText(valueText(period, totalClogs));
		bossValue.setText(valueText(period, totalBossKc));
		String bossHelp = bossTooltip(period, bossName);
		bossValue.setToolTipText(bossHelp);
		bossLeader.setToolTipText(bossHelp);
		bossColumnLabel.setToolTipText(bossHelp);
		averageValue.setText(valueText(period, averageXp));
		String activeHelp = bossName == null ? activeTooltip(period) : bossActiveTooltip(period, bossName);
		String clogHelp = clogTooltip(period);
		String averageHelp = averageTooltip(period);
		activeValue.setToolTipText(activeHelp);
		clogValue.setToolTipText(clogHelp);
		averageValue.setToolTipText(averageHelp);
		clogLeader.setToolTipText(clogHelp);
		memberList.setToolTipText(bossName == null ? clogHelp : bossHelp);
		sourceStatus.setText((period.isAllTime() ? "Latest totals" : period.getLabel() + " gains")
			+ "  ·  WOM group #" + leaderboard.getGroupId()
			+ (bossName == null ? "" : "  ·  Boss: " + bossName)
			+ "  ·  Updated " + UPDATED_FORMAT.format(leaderboard.getCreatedAt()));

		if (bossName == null)
		{
			setLeaderText(xpLeader, period, ClanProgressMetric.XP);
			setLeaderText(clogLeader, period, ClanProgressMetric.COLLECTIONS);
		}
		setBossLeaderText(period, bossName);
		refreshList();
	}

	private void setBossLeaderText(GainPeriod period, String bossName)
	{
		ClanProgressPlayer leader = leaderboard.getBossLeader(period, bossName);
		if (leader == null)
		{
			bossLeader.setText("No activity");
			return;
		}
		long value = leader.getGains(period).getBossKc(bossName);
		bossLeader.setText("<html><center><b>" + escapeHtml(leader.getName()) + "</b><br>"
			+ valueText(period, value) + "</center></html>");
	}

	private void setLeaderText(JLabel label, GainPeriod period, ClanProgressMetric metric)
	{
		ClanProgressPlayer leader = leaderboard.getLeader(period, metric);
		if (leader == null)
		{
			label.setText("No activity");
			return;
		}
		long value = leader.getGains(period).valueFor(metric);
		label.setText("<html><center><b>" + escapeHtml(leader.getName()) + "</b><br>"
			+ valueText(period, value) + "</center></html>");
	}

	private void refreshList()
	{
		listModel.clear();
		if (leaderboard == null)
		{
			visibleMembers.setText("0 shown");
			return;
		}

		GainPeriod period = selectedPeriod();
		ClanProgressMetric metric = selectedMetric();
		String bossName = selectedBoss();
		String filter = searchField.getText().trim().toLowerCase(Locale.ROOT);
		List<ClanProgressPlayer> players = new ArrayList<>();
		for (ClanProgressPlayer player : leaderboard.getPlayers())
		{
			if (filter.isEmpty() || player.getName().toLowerCase(Locale.ROOT).contains(filter))
			{
				players.add(player);
			}
		}

		Comparator<ClanProgressPlayer> comparator = Comparator
			.comparingLong((ClanProgressPlayer player) -> metricValue(player, period, metric, bossName))
			.reversed()
			.thenComparingLong(player -> player.getGains(period).getXp() * -1L)
			.thenComparing(ClanProgressPlayer::getName, String.CASE_INSENSITIVE_ORDER);
		players.sort(comparator);
		for (ClanProgressPlayer player : players)
		{
			listModel.addElement(player);
		}
		visibleMembers.setText(players.size() + " shown");
	}

	private static long metricValue(
		ClanProgressPlayer player,
		GainPeriod period,
		ClanProgressMetric metric,
		String bossName)
	{
		return metric == ClanProgressMetric.BOSS_KC
			? player.getGains(period).getBossKc(bossName)
			: player.getGains(period).valueFor(metric);
	}

	private GainPeriod selectedPeriod()
	{
		GainPeriod period = (GainPeriod) periodSelector.getSelectedItem();
		return period == null ? GainPeriod.DAY : period;
	}

	private ClanProgressMetric selectedMetric()
	{
		ClanProgressMetric metric = (ClanProgressMetric) sortSelector.getSelectedItem();
		return metric == null ? ClanProgressMetric.XP : metric;
	}

	private String selectedBoss()
	{
		String bossName = bossSelector.getSelectedItem() instanceof String
			? (String) bossSelector.getSelectedItem() : ALL_BOSSES;
		return ALL_BOSSES.equals(bossName) ? null : bossName;
	}

	private static JPanel fieldRow(String labelText, Component field)
	{
		JPanel row = new JPanel(new BorderLayout(7, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setAlignmentX(CENTER_ALIGNMENT);
		row.setPreferredSize(new Dimension(280, 29));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
		JLabel label = new JLabel(labelText);
		label.setPreferredSize(new Dimension(55, 27));
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		row.add(label, BorderLayout.WEST);
		row.add(field, BorderLayout.CENTER);
		return row;
	}

	private JPanel summaryCell(String title, JLabel value)
	{
		return summaryCell(title, value, null);
	}

	private JPanel summaryCell(String title, JLabel value, String tooltip)
	{
		return summaryCell(title, value, tooltip, null);
	}

	private JPanel summaryCell(
		String title,
		JLabel value,
		String tooltip,
		RuneVersusIcons.Kind iconKind)
	{
		JPanel cell = new JPanel(new BorderLayout());
		cell.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		cell.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			BorderFactory.createEmptyBorder(4, 5, 4, 5)));
		JLabel heading = new JLabel(title, SwingConstants.CENTER);
		heading.setFont(FontManager.getRunescapeSmallFont());
		heading.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		if (iconKind != null)
		{
			icons.apply(heading, iconKind, 17);
		}
		cell.setToolTipText(tooltip);
		heading.setToolTipText(tooltip);
		value.setToolTipText(tooltip);
		cell.add(heading, BorderLayout.NORTH);
		cell.add(value, BorderLayout.CENTER);
		return cell;
	}

	private static JLabel summaryValue(Color color)
	{
		JLabel label = new JLabel("—", SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(color);
		return label;
	}

	private static JLabel leaderValue()
	{
		JLabel label = new JLabel("—", SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(Color.WHITE);
		return label;
	}

	private static JPanel leaderCard(String title, JLabel value, Color accent)
	{
		return leaderCard(title, value, accent, null);
	}

	private static JPanel leaderCard(String title, JLabel value, Color accent, String tooltip)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(3, 0, 0, 0, accent),
			BorderFactory.createEmptyBorder(4, 8, 4, 8)));
		JLabel heading = new JLabel(title, SwingConstants.CENTER);
		heading.setFont(FontManager.getRunescapeSmallFont());
		heading.setForeground(accent);
		panel.setToolTipText(tooltip);
		heading.setToolTipText(tooltip);
		value.setToolTipText(tooltip);
		panel.add(heading, BorderLayout.NORTH);
		panel.add(value, BorderLayout.CENTER);
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

	private static String activeTooltip(GainPeriod period)
	{
		if (period.isAllTime())
		{
			return "<html><b>Active - All-time</b><br>Members with at least one non-zero XP, CLog, or Boss KC total<br>in their latest Wise Old Man snapshot.</html>";
		}
		return "<html><b>Active - " + period.getLabel() + "</b><br>Members who gained XP, unlocked a CLog, or gained Boss KC<br>during this period.</html>";
	}

	private static String bossActiveTooltip(GainPeriod period, String bossName)
	{
		return "<html><b>Active - " + escapeHtml(bossName) + "</b><br>Members with "
			+ (period.isAllTime() ? "at least one recorded KC" : "KC gained")
			+ " for this boss during " + period.getLabel() + ".</html>";
	}

	private static String clogTooltip(GainPeriod period)
	{
		if (period.isAllTime())
		{
			return "<html><b>CLogs - All-time</b><br>The sum of every member's current collection-log count<br>in the latest Wise Old Man snapshots.</html>";
		}
		return "<html><b>CLogs - " + period.getLabel() + "</b><br>Collection-log unlocks summed across members during this period.<br><b>+3</b> means three unlocks in total; different members can unlock the same item.</html>";
	}

	private static String averageTooltip(GainPeriod period)
	{
		if (period.isAllTime())
		{
			return "<html><b>Average active XP - All-time</b><br>Total clan XP divided by members with at least one non-zero<br>XP, CLog, or Boss KC total.</html>";
		}
		return "<html><b>Average active XP - " + period.getLabel() + "</b><br>Clan XP gained during this period divided by Active members.<br>Members with no tracked activity are excluded.</html>";
	}

	private static String bossTooltip(GainPeriod period, String bossName)
	{
		String metric = bossName == null ? "all bosses combined" : escapeHtml(bossName);
		String valueKind = period.isAllTime() ? "current KC" : "KC gained";
		return "<html><b>Boss KC - " + metric + "</b><br>Shows " + valueKind
			+ " for " + metric + " during " + period.getLabel() + ".</html>";
	}

	private static JPanel verticalPanel(Color background)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(background);
		return panel;
	}

	private static String valueText(GainPeriod period, long value)
	{
		return value == 0L ? "0" : ClanProgressLeaderboard.valueText(period, value);
	}

	private static String htmlColor(Color color)
	{
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private static String escapeHtml(String value)
	{
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private class MemberRenderer extends JLabel implements ListCellRenderer<ClanProgressPlayer>
	{
		private MemberRenderer()
		{
			setOpaque(true);
			setVerticalAlignment(SwingConstants.CENTER);
			setFont(FontManager.getRunescapeSmallFont());
			setForeground(Color.WHITE);
			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
				BorderFactory.createEmptyBorder(3, 8, 3, 8)));
		}

		@Override
		public Component getListCellRendererComponent(
			JList<? extends ClanProgressPlayer> list,
			ClanProgressPlayer player,
			int index,
			boolean isSelected,
			boolean cellHasFocus)
		{
			setBackground(isSelected ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			Color rankColor = index == 0 ? GOLD : index == 1 ? SILVER : index == 2 ? BRONZE : Color.WHITE;
			ClanProgressGains gains = player.getGains(selectedPeriod());
			ClanProgressMetric selected = selectedMetric();
			String bossName = selectedBoss();
			long displayedBossKc = gains.getBossKc(bossName);
			int width = Math.max(760, list.getWidth() - 32);
			if (bossName != null)
			{
				setText("<html><table width='" + width + "' cellspacing='0'><tr>"
					+ "<td width='50%'><b><font color='" + htmlColor(rankColor) + "'>#" + (index + 1)
					+ "  " + escapeHtml(player.getName()) + "</font></b></td>"
					+ "<td width='50%' align='center'><font color='" + htmlColor(RED) + "'><b>"
					+ valueText(selectedPeriod(), displayedBossKc) + " KC</b></font></td>"
					+ "</tr></table></html>");
				return this;
			}
			setText("<html><table width='" + width + "' cellspacing='0'><tr>"
				+ "<td width='25%'><b><font color='" + htmlColor(rankColor) + "'>#" + (index + 1)
				+ "  " + escapeHtml(player.getName()) + "</font></b></td>"
				+ statCell(gains.getXp(), selected == ClanProgressMetric.XP, GOLD)
				+ statCell(gains.getCollections(), selected == ClanProgressMetric.COLLECTIONS, TEAL)
				+ statCell(displayedBossKc, selected == ClanProgressMetric.BOSS_KC, RED)
				+ "</tr></table></html>");
			return this;
		}

		private String statCell(long value, boolean highlighted, Color accent)
		{
			String color = highlighted ? htmlColor(accent) : "#d0d0d0";
			return "<td width='25%' align='center'><font color='" + color + "'><b>"
				+ valueText(selectedPeriod(), value) + "</b></font></td>";
		}
	}
}
