package com.runeversus;

import com.runeversus.model.DuelResult;
import com.runeversus.model.MetricResult;
import com.runeversus.model.MetricType;
import com.runeversus.model.PlayerSide;
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
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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

class VersusComparisonPanel extends JPanel
{
	private static final DateTimeFormatter UPDATED_FORMAT =
		DateTimeFormatter.ofPattern("dd MMM, HH:mm").withZone(ZoneId.systemDefault());
	private static final Color GOLD = new Color(246, 197, 92);
	private static final Color SILVER = new Color(205, 211, 222);
	private static final Color TEAL = new Color(82, 201, 181);
	private static final Color RED = new Color(224, 104, 104);
	private static final Color PURPLE = new Color(174, 140, 245);

	enum Category
	{
		OVERVIEW("Overview"),
		SKILLS("All skills"),
		BOSSES("Boss KC"),
		ACTIVITIES("Activities"),
		RECENT_XP("Recent XP");

		private final String label;

		Category(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private enum SortMode
	{
		DEFAULT("Recommended order"),
		GAP("Biggest difference"),
		NAME("Name");

		private final String label;

		SortMode(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private final Runnable backCallback;
	private final String backButtonText;
	private final Runnable exportCallback;
	private final Runnable openCardCallback;
	private final RuneVersusIcons icons;
	private final boolean spaciousLayout;
	private final JLabel sourceStatus = new JLabel("No comparison loaded.", SwingConstants.CENTER);
	private final JLabel leftPlayer = new JLabel("Player 1", SwingConstants.CENTER);
	private final JLabel rightPlayer = new JLabel("Player 2", SwingConstants.CENTER);
	private final JLabel winner = new JLabel("VS", SwingConstants.CENTER);
	private final JLabel categoryScore = new JLabel(" ", SwingConstants.CENTER);
	private final JLabel xpHighlight = highlightLabel();
	private final JLabel bossHighlight = highlightLabel();
	private final JLabel clogHighlight = highlightLabel();
	private final JLabel caHighlight = highlightLabel();
	private final JComboBox<Category> categorySelector = new JComboBox<>(Category.values());
	private final JComboBox<SortMode> sortSelector = new JComboBox<>(SortMode.values());
	private final JTextField searchField = new JTextField();
	private final JLabel visibleMetrics = new JLabel("0 shown", SwingConstants.RIGHT);
	private final DefaultListModel<DisplayMetric> listModel = new DefaultListModel<>();
	private final JList<DisplayMetric> metricList = new JList<>(listModel);
	private final JButton exportButton = new JButton("Export card again");
	private final JButton openCardButton = new JButton("Open saved card");

	private DuelResult result;
	private String verdict = "";

	VersusComparisonPanel(
		Runnable backCallback,
		Runnable exportCallback,
		Runnable openCardCallback)
	{
		this("Back", backCallback, exportCallback, openCardCallback, RuneVersusIcons.empty());
	}

	VersusComparisonPanel(
		Runnable backCallback,
		Runnable exportCallback,
		Runnable openCardCallback,
		RuneVersusIcons icons)
	{
		this("Back", backCallback, exportCallback, openCardCallback, icons, true);
	}

	VersusComparisonPanel(
		String backButtonText,
		Runnable backCallback,
		Runnable exportCallback,
		Runnable openCardCallback,
		RuneVersusIcons icons,
		boolean spaciousLayout)
	{
		this.backButtonText = backButtonText;
		this.backCallback = backCallback;
		this.exportCallback = exportCallback;
		this.openCardCallback = openCardCallback;
		this.icons = icons;
		this.spaciousLayout = spaciousLayout;
		if (!spaciousLayout)
		{
			configureIcons();
		}

		setLayout(new BorderLayout(0, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		add(buildTop(), BorderLayout.NORTH);
		add(buildMetricList(), BorderLayout.CENTER);
		add(buildFooter(), BorderLayout.SOUTH);
		wireControls();
	}

	VersusComparisonPanel(
		String backButtonText,
		Runnable backCallback,
		Runnable exportCallback,
		Runnable openCardCallback)
	{
		this(backButtonText, backCallback, exportCallback, openCardCallback, RuneVersusIcons.empty());
	}

	VersusComparisonPanel(
		String backButtonText,
		Runnable backCallback,
		Runnable exportCallback,
		Runnable openCardCallback,
		RuneVersusIcons icons)
	{
		this(backButtonText, backCallback, exportCallback, openCardCallback, icons, true);
	}

	void setLoading(String leftName, String rightName)
	{
		result = null;
		verdict = "";
		leftPlayer.setText(playerHtml(leftName, "Loading..."));
		rightPlayer.setText(playerHtml(rightName, "Loading..."));
		winner.setText("VS");
		categoryScore.setText("Fetching hiscores and optional recent XP...");
		sourceStatus.setText("Comparison in progress");
		listModel.clear();
		visibleMetrics.setText("0 shown");
		setHighlights("—", "—", "—", "—");
		exportButton.setEnabled(false);
		openCardButton.setVisible(false);
	}

	void setResult(DuelResult result, File exported, String verdict)
	{
		this.result = result;
		this.verdict = verdict == null ? "" : verdict;
		exportButton.setEnabled(true);
		setExportedCard(exported);
		refreshView();
	}

	void setError(String message)
	{
		sourceStatus.setText(message);
		categoryScore.setText("Use Back to change the players, then try again.");
	}

	void setExportedCard(File file)
	{
		openCardButton.setVisible(file != null && file.isFile());
	}

	int getDisplayedMetricCount()
	{
		return listModel.size();
	}

	String getDisplayedMetricName(int index)
	{
		return listModel.get(index).name;
	}

	String getDisplayedMetricHtml(int index)
	{
		return metricHtml(listModel.get(index), 245);
	}

	void selectCategory(Category category)
	{
		categorySelector.setSelectedItem(category);
	}

	void setSearchText(String text)
	{
		searchField.setText(text);
	}

	private JPanel buildTop()
	{
		return spaciousLayout ? buildSpaciousTop() : buildCompactTop();
	}

	private JPanel buildSpaciousTop()
	{
		JPanel top = verticalPanel(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("PLAYER COMPARISON", SwingConstants.CENTER);
		title.setFont(FontManager.getRunescapeBoldFont().deriveFont(22f));
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setAlignmentX(CENTER_ALIGNMENT);
		top.add(title);
		top.add(Box.createVerticalStrut(5));

		sourceStatus.setFont(FontManager.getRunescapeSmallFont().deriveFont(12f));
		sourceStatus.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		sourceStatus.setAlignmentX(CENTER_ALIGNMENT);
		sourceStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
		top.add(sourceStatus);
		top.add(Box.createVerticalStrut(10));

		JPanel matchup = new JPanel(new GridLayout(1, 3, 10, 0));
		matchup.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		matchup.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			BorderFactory.createEmptyBorder(13, 16, 13, 16)));
		matchup.setAlignmentX(CENTER_ALIGNMENT);
		matchup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));
		configurePlayerLabel(leftPlayer, true);
		configurePlayerLabel(rightPlayer, true);
		winner.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
		winner.setForeground(GOLD);
		matchup.add(leftPlayer);
		matchup.add(winner);
		matchup.add(rightPlayer);
		top.add(matchup);

		categoryScore.setFont(FontManager.getRunescapeSmallFont().deriveFont(12f));
		categoryScore.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		categoryScore.setAlignmentX(CENTER_ALIGNMENT);
		categoryScore.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		top.add(categoryScore);
		top.add(Box.createVerticalStrut(10));

		JPanel filters = new JPanel(new GridLayout(1, 3, 12, 0));
		filters.setBackground(ColorScheme.DARK_GRAY_COLOR);
		filters.setAlignmentX(CENTER_ALIGNMENT);
		filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
		filters.add(fieldRow("View", categorySelector, 62, 34));
		filters.add(fieldRow("Order", sortSelector, 62, 34));
		filters.add(fieldRow("Find", searchField, 62, 34));
		top.add(filters);
		top.add(Box.createVerticalStrut(12));

		JLabel highlightHeading = new JLabel("HIGHLIGHTS");
		highlightHeading.setFont(FontManager.getRunescapeBoldFont().deriveFont(15f));
		highlightHeading.setForeground(Color.WHITE);
		highlightHeading.setAlignmentX(LEFT_ALIGNMENT);
		top.add(highlightHeading);
		top.add(Box.createVerticalStrut(5));

		JPanel highlights = new JPanel(new GridLayout(1, 4, 10, 0));
		highlights.setBackground(ColorScheme.DARK_GRAY_COLOR);
		highlights.setAlignmentX(CENTER_ALIGNMENT);
		highlights.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
		highlights.add(highlightCard("TOTAL XP", xpHighlight, RuneVersusIcons.Kind.XP, GOLD));
		highlights.add(highlightCard("BOSS KC", bossHighlight, RuneVersusIcons.Kind.PVM, RED));
		highlights.add(highlightCard("COLLECTION LOG", clogHighlight, RuneVersusIcons.Kind.COLLECTION_LOG, TEAL));
		highlights.add(highlightCard("CA RANK", caHighlight, RuneVersusIcons.Kind.COMBAT_ACHIEVEMENTS, PURPLE));
		top.add(highlights);
		return top;
	}

	private JPanel buildCompactTop()
	{
		JPanel top = verticalPanel(ColorScheme.DARK_GRAY_COLOR);

		JPanel navigation = new JPanel(new BorderLayout());
		navigation.setBackground(ColorScheme.DARK_GRAY_COLOR);
		navigation.setAlignmentX(CENTER_ALIGNMENT);
		navigation.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		JButton back = new JButton(backButtonText);
		back.setPreferredSize(new Dimension(170, 30));
		back.addActionListener(e -> backCallback.run());
		navigation.add(back, BorderLayout.WEST);
		top.add(navigation);
		top.add(Box.createVerticalStrut(9));

		JLabel title = new JLabel("Player comparison", SwingConstants.CENTER);
		title.setFont(FontManager.getRunescapeBoldFont().deriveFont(17f));
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setAlignmentX(CENTER_ALIGNMENT);
		top.add(title);
		top.add(Box.createVerticalStrut(3));

		sourceStatus.setFont(FontManager.getRunescapeSmallFont());
		sourceStatus.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		sourceStatus.setAlignmentX(CENTER_ALIGNMENT);
		sourceStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
		top.add(sourceStatus);
		top.add(Box.createVerticalStrut(7));

		JPanel matchup = new JPanel(new GridLayout(1, 3, 4, 0));
		matchup.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		matchup.setBorder(BorderFactory.createEmptyBorder(7, 5, 7, 5));
		matchup.setAlignmentX(CENTER_ALIGNMENT);
		matchup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
		configurePlayerLabel(leftPlayer, false);
		configurePlayerLabel(rightPlayer, false);
		winner.setFont(FontManager.getRunescapeBoldFont());
		winner.setForeground(GOLD);
		matchup.add(leftPlayer);
		matchup.add(winner);
		matchup.add(rightPlayer);
		top.add(matchup);

		categoryScore.setFont(FontManager.getRunescapeSmallFont());
		categoryScore.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		categoryScore.setAlignmentX(CENTER_ALIGNMENT);
		categoryScore.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		top.add(categoryScore);
		top.add(Box.createVerticalStrut(7));

		top.add(fieldRow("View", categorySelector));
		top.add(Box.createVerticalStrut(4));
		top.add(fieldRow("Order", sortSelector));
		top.add(Box.createVerticalStrut(4));
		top.add(fieldRow("Find", searchField));
		top.add(Box.createVerticalStrut(8));

		JPanel highlights = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		highlights.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		highlights.setAlignmentX(CENTER_ALIGNMENT);
		highlights.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
		JLabel heading = new JLabel("Highlights");
		heading.setFont(FontManager.getRunescapeBoldFont());
		heading.setForeground(Color.WHITE);
		heading.setAlignmentX(LEFT_ALIGNMENT);
		highlights.add(heading);
		highlights.add(xpHighlight);
		highlights.add(bossHighlight);
		highlights.add(clogHighlight);
		highlights.add(caHighlight);
		top.add(highlights);
		return top;
	}

	private JPanel buildMetricList()
	{
		metricList.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		metricList.setForeground(Color.WHITE);
		metricList.setSelectionBackground(ColorScheme.MEDIUM_GRAY_COLOR);
		metricList.setFixedCellHeight(spaciousLayout ? 88 : 70);
		metricList.setCellRenderer(new MetricRenderer());
		JScrollPane scroll = new JScrollPane(metricList);
		scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));

		JPanel container = new JPanel(new BorderLayout(0, 4));
		container.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel title = new JLabel("Comparisons");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		visibleMetrics.setFont(FontManager.getRunescapeSmallFont());
		visibleMetrics.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		header.add(title, BorderLayout.WEST);
		header.add(visibleMetrics, BorderLayout.EAST);
		container.add(header, BorderLayout.NORTH);
		container.add(scroll, BorderLayout.CENTER);
		return container;
	}

	private JPanel buildFooter()
	{
		JPanel footer = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
		exportButton.setEnabled(false);
		exportButton.addActionListener(e -> exportCallback.run());
		if (spaciousLayout)
		{
			JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
			actions.setBackground(ColorScheme.DARK_GRAY_COLOR);
			actions.setAlignmentX(CENTER_ALIGNMENT);
			actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
			JButton back = new JButton(backButtonText);
			back.addActionListener(e -> backCallback.run());
			actions.add(back);
			actions.add(exportButton);
			footer.add(actions);
		}
		else
		{
			exportButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
			exportButton.setAlignmentX(CENTER_ALIGNMENT);
			footer.add(exportButton);
		}

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

	private JPanel highlightCard(
		String title,
		JLabel value,
		RuneVersusIcons.Kind iconKind,
		Color accent)
	{
		JPanel card = new JPanel(new BorderLayout(0, 4));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(3, 0, 0, 0, accent),
			BorderFactory.createEmptyBorder(8, 10, 8, 10)));
		JLabel heading = new JLabel(title, SwingConstants.CENTER);
		heading.setFont(FontManager.getRunescapeBoldFont().deriveFont(13f));
		heading.setForeground(accent);
		icons.apply(heading, iconKind, 20);
		value.setHorizontalAlignment(SwingConstants.CENTER);
		value.setFont(FontManager.getRunescapeBoldFont().deriveFont(13f));
		value.setForeground(Color.WHITE);
		card.add(heading, BorderLayout.NORTH);
		card.add(value, BorderLayout.CENTER);
		return card;
	}

	private void wireControls()
	{
		categorySelector.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(
				JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				JLabel label = (JLabel) super.getListCellRendererComponent(
					list, value, index, isSelected, cellHasFocus);
				Category category = value instanceof Category ? (Category) value : Category.OVERVIEW;
				RuneVersusIcons.Kind iconKind = categoryIcon(category);
				label.setIcon(iconKind == null ? null : icons.icon(iconKind, 18, categorySelector));
				label.setIconTextGap(6);
				return label;
			}
		});
		categorySelector.setSelectedItem(Category.OVERVIEW);
		sortSelector.setSelectedItem(SortMode.DEFAULT);
		categorySelector.addActionListener(e -> refreshList());
		sortSelector.addActionListener(e -> refreshList());
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

	private void configureIcons()
	{
		icons.apply(xpHighlight, RuneVersusIcons.Kind.XP, 19);
		icons.apply(bossHighlight, RuneVersusIcons.Kind.PVM, 19);
		icons.apply(clogHighlight, RuneVersusIcons.Kind.COLLECTION_LOG, 19);
		icons.apply(caHighlight, RuneVersusIcons.Kind.COMBAT_ACHIEVEMENTS, 19);
	}

	private static RuneVersusIcons.Kind categoryIcon(Category category)
	{
		switch (category)
		{
			case BOSSES:
				return RuneVersusIcons.Kind.PVM;
			case ACTIVITIES:
				// Activities mix clues, minigames, collection logs, CAs, and PBs.
				// A single OSRS sprite would imply a narrower category than it is.
				return null;
			case OVERVIEW:
			case SKILLS:
			case RECENT_XP:
			default:
				return RuneVersusIcons.Kind.XP;
		}
	}

	private void refreshView()
	{
		if (result == null)
		{
			return;
		}

		String leftName = result.getLeft().getName();
		String rightName = result.getRight().getName();
		leftPlayer.setText(playerHtml(leftName, result.getLeftTotalWins() + " wins"));
		rightPlayer.setText(playerHtml(rightName, result.getRightTotalWins() + " wins"));
		if ("Tie".equals(result.getWinnerName()))
		{
			winner.setText("TIE");
		}
		else
		{
			winner.setText("<html><center>WINNER<br>" + escapeHtml(result.getWinnerName()) + "</center></html>");
		}
		categoryScore.setText(leftName + " " + result.getLeftTotalWins() + " wins"
			+ "  |  " + rightName + " " + result.getRightTotalWins() + " wins");
		sourceStatus.setText("<html><center>"
			+ escapeHtml(verdict.isEmpty() ? "Comparison complete" : verdict)
			+ "<br><font color='#8f8f8f'>Updated " + UPDATED_FORMAT.format(result.getCreatedAt())
			+ "</font></center></html>");

		setHighlights(
			leadText("Total XP", result.getLeftTotalXp(), result.getRightTotalXp()),
			leadText("Boss KC", result.getLeftBossKc(), result.getRightBossKc()),
			activityText("CLogs", result.getCollectionLogMetric()),
			combatAchievementText(result.getCombatAchievementMetric()));
		refreshList();
	}

	private void refreshList()
	{
		listModel.clear();
		if (result == null)
		{
			visibleMetrics.setText("0 shown");
			return;
		}

		List<DisplayMetric> metrics = metricsFor((Category) categorySelector.getSelectedItem());
		String filter = searchField.getText().trim().toLowerCase(Locale.ROOT);
		if (!filter.isEmpty())
		{
			metrics.removeIf(metric -> !metric.name.toLowerCase(Locale.ROOT).contains(filter));
		}
		if (sortSelector.getSelectedItem() == SortMode.NAME)
		{
			metrics.sort(Comparator.comparing(metric -> metric.name, String.CASE_INSENSITIVE_ORDER));
		}
		else if (sortSelector.getSelectedItem() == SortMode.GAP)
		{
			metrics.sort(Comparator.comparingLong((DisplayMetric metric) -> metric.metric.getGap())
				.reversed().thenComparing(metric -> metric.name, String.CASE_INSENSITIVE_ORDER));
		}
		for (DisplayMetric metric : metrics)
		{
			listModel.addElement(metric);
		}
		visibleMetrics.setText(metrics.size() + " shown");
	}

	private List<DisplayMetric> metricsFor(Category category)
	{
		List<DisplayMetric> metrics = new ArrayList<>();
		Category selected = category == null ? Category.OVERVIEW : category;
		switch (selected)
		{
			case SKILLS:
				addAll(metrics, result.getSkills(), "");
				break;
			case BOSSES:
				addAll(metrics, result.getBosses(), "");
				break;
			case ACTIVITIES:
				addAll(metrics, result.getActivities(), "");
				break;
			case RECENT_XP:
				addAll(metrics, result.getDayForm(), "24h · ");
				addAll(metrics, result.getWeekForm(), "Week · ");
				addAll(metrics, result.getMonthForm(), "Month · ");
				metrics.clear();
				addOverall(metrics, result.getDayForm(), "24h XP");
				addOverall(metrics, result.getWeekForm(), "Week XP");
				addOverall(metrics, result.getMonthForm(), "Month XP");
				metrics.add(totalXpMetric());
				break;
			case OVERVIEW:
			default:
				metrics.add(totalXpMetric());
				metrics.add(new DisplayMetric("Total boss KC", new MetricResult(
					MetricType.BOSS, "Total boss KC", result.getLeftBossKc(), result.getRightBossKc())));
				addIfPresent(metrics, result.getCollectionLogMetric());
				addIfPresent(metrics, result.getCombatAchievementMetric());
				addOverall(metrics, result.getDayForm(), "24h XP");
				addOverall(metrics, result.getWeekForm(), "Week XP");
				addOverall(metrics, result.getMonthForm(), "Month XP");
				metrics.sort(Comparator.comparingInt(VersusComparisonPanel::overviewOrder));
		}
		return metrics;
	}

	private DisplayMetric totalXpMetric()
	{
		return new DisplayMetric("Total XP", new MetricResult(
			MetricType.SKILL, "Total XP", result.getLeftTotalXp(), result.getRightTotalXp()));
	}

	private static int overviewOrder(DisplayMetric metric)
	{
		switch (metric.name)
		{
			case "24h XP": return 0;
			case "Week XP": return 1;
			case "Month XP": return 2;
			case "Total XP": return 3;
			case "Total boss KC": return 4;
			case "Collections Logged": return 5;
			case "Combat Achievements": return 6;
			default: return 7;
		}
	}

	private void setHighlights(String xp, String boss, String clogs, String ca)
	{
		xpHighlight.setText(xp);
		bossHighlight.setText(boss);
		clogHighlight.setText(clogs);
		caHighlight.setText(ca);
	}

	private String leadText(String label, long left, long right)
	{
		if (left == right)
		{
			return spaciousLayout
				? "<html><center><b>Tie</b><br>" + RuneVersusFlavor.format(left) + " each</center></html>"
				: label + "  ·  Tie at " + RuneVersusFlavor.format(left);
		}
		String leader = left > right ? result.getLeft().getName() : result.getRight().getName();
		String gap = RuneVersusFlavor.format(Math.abs(left - right));
		return spaciousLayout
			? "<html><center><b>" + escapeHtml(leader) + "</b><br><font color='#f6c55c'>+"
				+ gap + " ahead</font></center></html>"
			: label + "  ·  " + leader + "  +" + gap;
	}

	private String activityText(String label, MetricResult metric)
	{
		return metric == null ? (spaciousLayout ? "Not available" : label + "  ·  Not available")
			: leadText(label, metric.getLeftValue(), metric.getRightValue());
	}

	private String combatAchievementText(MetricResult metric)
	{
		if (metric == null)
		{
			return spaciousLayout ? "Not available" : "CA rank  ·  Not available";
		}
		String ranks = CombatAchievementTier.fromScore(metric.getLeftValue()).getDisplayName()
			+ " vs " + CombatAchievementTier.fromScore(metric.getRightValue()).getDisplayName();
		return spaciousLayout ? "<html><center><b>Tier comparison</b><br>" + ranks + "</center></html>"
			: "CA rank  ·  " + ranks;
	}

	private static void addAll(List<DisplayMetric> out, List<MetricResult> source, String prefix)
	{
		for (MetricResult metric : source)
		{
			out.add(new DisplayMetric(prefix + metric.getName(), metric));
		}
	}

	private static void addIfPresent(List<DisplayMetric> out, MetricResult metric)
	{
		if (metric != null)
		{
			out.add(new DisplayMetric(metric.getName(), metric));
		}
	}

	private static void addOverall(List<DisplayMetric> out, List<MetricResult> source, String name)
	{
		for (MetricResult metric : source)
		{
			if ("Overall".equals(metric.getName()))
			{
				out.add(new DisplayMetric(name, metric));
				return;
			}
		}
	}

	private static JPanel fieldRow(String labelText, Component field)
	{
		return fieldRow(labelText, field, 53, 29);
	}

	private static JPanel fieldRow(String labelText, Component field, int labelWidth, int height)
	{
		JPanel row = new JPanel(new BorderLayout(7, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setAlignmentX(CENTER_ALIGNMENT);
		row.setPreferredSize(new Dimension(260, height));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		JLabel label = new JLabel(labelText);
		label.setPreferredSize(new Dimension(labelWidth, height - 2));
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		row.add(label, BorderLayout.WEST);
		row.add(field, BorderLayout.CENTER);
		return row;
	}

	private static void configurePlayerLabel(JLabel label, boolean spacious)
	{
		label.setFont(spacious
			? FontManager.getRunescapeBoldFont().deriveFont(15f)
			: FontManager.getRunescapeSmallFont());
		label.setForeground(Color.WHITE);
	}

	private static JLabel highlightLabel()
	{
		JLabel label = new JLabel("—", SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setAlignmentX(CENTER_ALIGNMENT);
		return label;
	}

	private static JPanel verticalPanel(Color background)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(background);
		return panel;
	}

	private static String playerHtml(String name, String score)
	{
		return "<html><center><b>" + escapeHtml(name) + "</b><br><font color='#b8b8b8'>"
			+ escapeHtml(score) + "</font></center></html>";
	}

	private static String metricValue(MetricResult metric, long value)
	{
		if (metric.getType() == MetricType.COMBAT_ACHIEVEMENTS)
		{
			return CombatAchievementTier.fromScore(value).getDisplayName();
		}
		return RuneVersusFlavor.format(value);
	}

	private static String gapValue(MetricResult metric)
	{
		if (metric.getWinner() == PlayerSide.TIE)
		{
			return "Tie";
		}
		if (metric.getType() == MetricType.COMBAT_ACHIEVEMENTS)
		{
			return metric.getGap() + (metric.getGap() == 1L ? " tier" : " tiers");
		}
		return "Gap " + RuneVersusFlavor.format(metric.getGap());
	}

	private String metricLeadText(MetricResult metric)
	{
		if (metric.getWinner() == PlayerSide.TIE)
		{
			return "Tied";
		}
		String leader = metric.getWinner() == PlayerSide.LEFT
			? result.getLeft().getName() : result.getRight().getName();
		String gap = metric.getType() == MetricType.COMBAT_ACHIEVEMENTS
			? metric.getGap() + (metric.getGap() == 1L ? " tier" : " tiers")
			: RuneVersusFlavor.format(metric.getGap());
		return leader + " leads by " + gap;
	}

	private String metricHtml(DisplayMetric display, int tableWidth)
	{
		MetricResult metric = display.metric;
		Color leftColor = metric.getWinner() == PlayerSide.LEFT ? GOLD
			: metric.getWinner() == PlayerSide.TIE ? SILVER : Color.WHITE;
		Color rightColor = metric.getWinner() == PlayerSide.RIGHT ? GOLD
			: metric.getWinner() == PlayerSide.TIE ? SILVER : Color.WHITE;
		String leftName = result == null ? "Player 1" : result.getLeft().getName();
		String rightName = result == null ? "Player 2" : result.getRight().getName();
		String leadColor = metric.getWinner() == PlayerSide.TIE ? htmlColor(SILVER) : htmlColor(GOLD);
		return "<html><table width='" + tableWidth + "' cellspacing='0'>"
			+ "<tr><td colspan='2' align='center'><b>" + escapeHtml(display.name) + "</b></td></tr><tr>"
			+ "<td width='50%' align='left'><font color='" + htmlColor(leftColor) + "'><b>"
			+ escapeHtml(leftName) + "</b>  " + metricValue(metric, metric.getLeftValue()) + "</font></td>"
			+ "<td width='50%' align='right'><font color='" + htmlColor(rightColor) + "'>"
			+ metricValue(metric, metric.getRightValue()) + "  <b>" + escapeHtml(rightName) + "</b></font></td>"
			+ "</tr><tr><td colspan='2' align='center'><font color='" + leadColor + "'><b>"
			+ escapeHtml(metricLeadText(metric)) + "</b></font></td></tr></table></html>";
	}

	private static String htmlColor(Color color)
	{
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private static String escapeHtml(String value)
	{
		return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static final class DisplayMetric
	{
		private final String name;
		private final MetricResult metric;

		private DisplayMetric(String name, MetricResult metric)
		{
			this.name = name;
			this.metric = metric;
		}
	}

	private final class MetricRenderer extends JLabel implements ListCellRenderer<DisplayMetric>
	{
		private MetricRenderer()
		{
			setOpaque(true);
			setVerticalAlignment(SwingConstants.CENTER);
			setFont(spaciousLayout
				? FontManager.getRunescapeSmallFont().deriveFont(13f)
				: FontManager.getRunescapeSmallFont());
			setForeground(Color.WHITE);
			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
				BorderFactory.createEmptyBorder(4, 8, 4, 8)));
		}

		@Override
		public Component getListCellRendererComponent(
			JList<? extends DisplayMetric> list,
			DisplayMetric display,
			int index,
			boolean isSelected,
			boolean cellHasFocus)
		{
			setBackground(isSelected ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			setIcon(icons.metricIcon(display.metric, display.name, 22, list));
			setIconTextGap(8);
			int tableWidth = Math.max(245, list.getWidth() - (getIcon() == null ? 32 : 62));
			setText(metricHtml(display, tableWidth));
			return this;
		}
	}
}
