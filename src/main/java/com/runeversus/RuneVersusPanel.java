package com.runeversus;

import com.runeversus.model.DuelResult;
import com.runeversus.model.MetricResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
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

public class RuneVersusPanel extends PluginPanel
{
	enum RosterKind
	{
		PARTY,
		CLAN_ONLINE,
		CLAN_ALL
	}

	enum SocialAction
	{
		PARTY_BOARD,
		CLAN_BOARD,
		CLAN_RECAP,
		FIGHT_NIGHT,
		WATCHLIST
	}

	private final JTextField leftField = new JTextField();
	private final JTextField rightField = new JTextField();
	private final JComboBox<String> leftRoster = new JComboBox<>();
	private final JComboBox<String> rightRoster = new JComboBox<>();
	private final JLabel rosterStatus = new JLabel("Load Party or Clan to pick players.", SwingConstants.CENTER);
	private final JLabel status = new JLabel("Ready.", SwingConstants.CENTER);
	private final JTextArea summary = new JTextArea();
	private final JButton exportAgainButton = new JButton("Export Card Again");

	private BiConsumer<String, String> compareCallback;
	private Consumer<RosterKind> rosterCallback;
	private Consumer<SocialAction> socialActionCallback;
	private Supplier<String> localPlayerSupplier;
	private Runnable exportAgainCallback;

	RuneVersusPanel()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(title("RuneVersus"));
		add(subtitle("Generate OSRS duel cards"));
		add(Box.createVerticalStrut(8));
		add(manualPanel());
		add(Box.createVerticalStrut(8));
		add(rosterPanel());
		add(Box.createVerticalStrut(8));
		add(actionPanel());
		add(Box.createVerticalStrut(8));
		add(socialPanel());
		add(Box.createVerticalStrut(8));
		add(status);
		add(Box.createVerticalStrut(8));
		add(summaryPanel());

		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		status.setFont(FontManager.getRunescapeSmallFont());
		rosterStatus.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		rosterStatus.setFont(FontManager.getRunescapeSmallFont());

		exportAgainButton.setEnabled(false);
		exportAgainButton.addActionListener(e ->
		{
			if (exportAgainCallback != null)
			{
				exportAgainCallback.run();
			}
		});
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

	void setStatus(String message)
	{
		SwingUtilities.invokeLater(() -> status.setText(message));
	}

	void setPlayerA(String name)
	{
		SwingUtilities.invokeLater(() -> leftField.setText(name));
	}

	void setPlayerB(String name)
	{
		SwingUtilities.invokeLater(() -> rightField.setText(name));
	}

	void showText(String text)
	{
		SwingUtilities.invokeLater(() ->
		{
			summary.setText(text);
			summary.setCaretPosition(0);
		});
	}

	void setRoster(String label, List<String> names)
	{
		SwingUtilities.invokeLater(() ->
		{
			DefaultComboBoxModel<String> leftModel = new DefaultComboBoxModel<>();
			DefaultComboBoxModel<String> rightModel = new DefaultComboBoxModel<>();
			for (String name : names)
			{
				leftModel.addElement(name);
				rightModel.addElement(name);
			}
			leftRoster.setModel(leftModel);
			rightRoster.setModel(rightModel);
			if (rightModel.getSize() > 1)
			{
				rightRoster.setSelectedIndex(1);
			}
			rosterStatus.setText(label + ": " + names.size() + " player(s)");
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
			exportAgainButton.setEnabled(true);
			status.setText(exported == null ? "Comparison complete." : "Card exported: " + exported.getName());
			summary.setText(buildSummary(result, exported, verdict));
			summary.setCaretPosition(0);
		});
	}

	private JPanel manualPanel()
	{
		JPanel panel = cardPanel();
		panel.add(label("Manual RSNs"));
		panel.add(fieldRow("Player A", leftField));
		panel.add(fieldRow("Player B", rightField));

		JButton me = new JButton("Set A = Me");
		me.addActionListener(e ->
		{
			if (localPlayerSupplier != null)
			{
				leftField.setText(localPlayerSupplier.get());
			}
		});
		panel.add(me);
		return panel;
	}

	private JPanel rosterPanel()
	{
		JPanel panel = cardPanel();
		panel.add(label("Party / Clan Picker"));

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
		buttons.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		buttons.add(rosterButton("Party", RosterKind.PARTY));
		buttons.add(Box.createHorizontalStrut(4));
		buttons.add(rosterButton("Online Clan", RosterKind.CLAN_ONLINE));
		buttons.add(Box.createHorizontalStrut(4));
		buttons.add(rosterButton("All Clan", RosterKind.CLAN_ALL));
		panel.add(buttons);

		panel.add(rosterStatus);
		panel.add(fieldRow("A", leftRoster));
		panel.add(fieldRow("B", rightRoster));

		JButton use = new JButton("Use Selected Players");
		use.addActionListener(e ->
		{
			if (leftRoster.getSelectedItem() != null)
			{
				leftField.setText(leftRoster.getSelectedItem().toString());
			}
			if (rightRoster.getSelectedItem() != null)
			{
				rightField.setText(rightRoster.getSelectedItem().toString());
			}
		});
		panel.add(use);
		return panel;
	}

	private JButton rosterButton(String text, RosterKind kind)
	{
		JButton button = new JButton(text);
		button.addActionListener(e ->
		{
			if (rosterCallback != null)
			{
				rosterCallback.accept(kind);
			}
		});
		return button;
	}

	private JPanel actionPanel()
	{
		JPanel panel = cardPanel();
		JButton compare = new JButton("Generate Duel Card");
		compare.setFont(FontManager.getRunescapeBoldFont());
		compare.addActionListener(e ->
		{
			if (compareCallback != null)
			{
				compareCallback.accept(leftField.getText(), rightField.getText());
			}
		});
		panel.add(compare);
		panel.add(Box.createVerticalStrut(5));
		panel.add(exportAgainButton);
		return panel;
	}

	private JPanel socialPanel()
	{
		JPanel panel = cardPanel();
		panel.add(label("Social Modes"));
		panel.add(socialButton("Party Leaderboard", SocialAction.PARTY_BOARD));
		panel.add(socialButton("Clan Leaderboard", SocialAction.CLAN_BOARD));
		panel.add(socialButton("Export Clan Recap", SocialAction.CLAN_RECAP));
		panel.add(socialButton("Fight Night", SocialAction.FIGHT_NIGHT));
		panel.add(socialButton("Watchlist Snipes", SocialAction.WATCHLIST));
		return panel;
	}

	private JButton socialButton(String text, SocialAction action)
	{
		JButton button = new JButton(text);
		button.setAlignmentX(LEFT_ALIGNMENT);
		button.setMaximumSize(new Dimension(PANEL_WIDTH, 27));
		button.addActionListener(e ->
		{
			if (socialActionCallback != null)
			{
				socialActionCallback.accept(action);
			}
		});
		return button;
	}

	private JScrollPane summaryPanel()
	{
		summary.setEditable(false);
		summary.setLineWrap(true);
		summary.setWrapStyleWord(true);
		summary.setFont(FontManager.getRunescapeSmallFont());
		summary.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		summary.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		summary.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		JScrollPane scroll = new JScrollPane(summary);
		scroll.setPreferredSize(new Dimension(PANEL_WIDTH, 265));
		scroll.setMaximumSize(new Dimension(PANEL_WIDTH, 600));
		scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		return scroll;
	}

	private static JPanel cardPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		panel.setMaximumSize(new Dimension(PANEL_WIDTH, 220));
		return panel;
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

	private static JLabel label(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Color.WHITE);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static JPanel fieldRow(String label, JTextField field)
	{
		JPanel row = baseRow(label);
		field.setMaximumSize(new Dimension(180, 26));
		row.add(field, BorderLayout.CENTER);
		return row;
	}

	private static JPanel fieldRow(String label, JComboBox<String> comboBox)
	{
		JPanel row = baseRow(label);
		comboBox.setMaximumSize(new Dimension(180, 26));
		row.add(comboBox, BorderLayout.CENTER);
		return row;
	}

	private static JPanel baseRow(String labelText)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		JLabel label = new JLabel(labelText);
		label.setPreferredSize(new Dimension(62, 24));
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setFont(FontManager.getRunescapeSmallFont());
		row.add(label, BorderLayout.WEST);
		row.setMaximumSize(new Dimension(PANEL_WIDTH, 32));
		return row;
	}

	private static String buildSummary(DuelResult result, File exported, String verdict)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(result.getLeft().getName()).append(" vs ").append(result.getRight().getName()).append('\n');
		sb.append("Score: ").append(result.getLeftTotalWins()).append(" - ").append(result.getRightTotalWins()).append('\n');
		sb.append("PvM: ").append(result.getLeftBossWins()).append(" - ").append(result.getRightBossWins()).append('\n');
		sb.append("Skills: ").append(result.getLeftSkillWins()).append(" - ").append(result.getRightSkillWins()).append('\n');
		sb.append("Activities: ").append(result.getLeftActivityWins()).append(" - ").append(result.getRightActivityWins()).append('\n');
		if (!result.getDayForm().isEmpty())
		{
			sb.append("24h XP: ").append(format(result.getLeftDayXp())).append(" - ").append(format(result.getRightDayXp())).append('\n');
			sb.append("Week XP: ").append(format(result.getLeftWeekXp())).append(" - ").append(format(result.getRightWeekXp())).append('\n');
			sb.append("Month XP: ").append(format(result.getLeftMonthXp())).append(" - ").append(format(result.getRightMonthXp())).append('\n');
		}
		sb.append('\n').append(verdict).append('\n');
		sb.append(RuneVersusFlavor.snipeCallout(result)).append('\n');

		MetricResult closest = result.getClosestSteal();
		if (closest != null)
		{
			sb.append("\nClosest steal: ").append(closest.getName())
				.append(" by ").append(format(closest.getGap())).append('.');
		}

		MetricResult flex = result.getBiggestFlex();
		if (flex != null)
		{
			sb.append("\nBiggest flex: ").append(flex.getName())
				.append(" by ").append(format(flex.getGap())).append('.');
		}

		if (result.getDayForm().isEmpty())
		{
			sb.append("\n\n24h/week/month gains: enable Wise Old Man gains in config.");
		}

		sb.append("\n\nPB and full collection log: reserved for opt-in sync data.");
		if (exported != null)
		{
			sb.append("\n\nPNG: ").append(exported.getAbsolutePath());
		}
		return sb.toString();
	}

	private static String format(long value)
	{
		return String.format("%,d", value);
	}
}
