package com.runeversus;

import com.runeversus.model.DuelResult;
import com.runeversus.model.MetricResult;
import com.runeversus.model.PlayerSide;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.inject.Singleton;

@Singleton
public class RuneVersusCardRenderer
{
	private static final int WIDTH = 1200;
	private static final int HEIGHT = 675;
	private static final DateTimeFormatter DATE_FORMAT =
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

	private static final Color BACKGROUND_TOP = new Color(20, 22, 28);
	private static final Color BACKGROUND_BOTTOM = new Color(8, 10, 15);
	private static final Color PANEL = new Color(27, 30, 38, 232);
	private static final Color PANEL_BORDER = new Color(230, 184, 92);
	private static final Color GOLD = new Color(246, 197, 92);
	private static final Color RED = new Color(194, 67, 76);
	private static final Color TEAL = new Color(73, 190, 176);
	private static final Color BLUE = new Color(80, 132, 213);
	private static final Color TEXT = new Color(244, 239, 225);
	private static final Color MUTED = new Color(169, 160, 145);

	public BufferedImage render(DuelResult duel)
	{
		return render(duel, RuneVersusCardTheme.AUTO, duel.getVerdict());
	}

	public BufferedImage render(DuelResult duel, RuneVersusCardTheme theme, String verdict)
	{
		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			setup(g);
			RuneVersusCardTheme resolvedTheme = RuneVersusFlavor.resolveTheme(duel, theme);
			drawBackground(g, resolvedTheme);
			drawHeader(g, duel, resolvedTheme);
			drawPlayerPanel(g, 58, 122, 430, 214, duel.getLeft().getName(), duel.getLeftTotalWins(), duel.getLeftBossKc(),
				duel.getLeftTotalXp(), duel.getLeftDayXp(), duel.getLeftWeekXp(), true, resolvedTheme);
			drawPlayerPanel(g, 712, 122, 430, 214, duel.getRight().getName(), duel.getRightTotalWins(), duel.getRightBossKc(),
				duel.getRightTotalXp(), duel.getRightDayXp(), duel.getRightWeekXp(), false, resolvedTheme);
			drawVersusCore(g, duel, resolvedTheme);
			drawCategoryStrip(g, duel);
			drawHighlights(g, duel, resolvedTheme);
			drawFooter(g, verdict);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	public BufferedImage renderIcon()
	{
		BufferedImage image = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			setup(g);
			g.setPaint(new GradientPaint(0, 0, new Color(39, 44, 56), 48, 48, new Color(12, 15, 23)));
			g.fillRoundRect(2, 2, 44, 44, 10, 10);
			g.setColor(GOLD);
			g.setStroke(new BasicStroke(3f));
			g.drawRoundRect(3, 3, 42, 42, 10, 10);
			g.setFont(new Font("SansSerif", Font.BOLD, 18));
			centerText(g, "VS", 24, 30, GOLD);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	private static void setup(Graphics2D g)
	{
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	}

	public BufferedImage renderRosterRecap(RosterLeaderboard leaderboard, RuneVersusCardTheme theme)
	{
		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			setup(g);
			RuneVersusCardTheme resolvedTheme = theme == RuneVersusCardTheme.AUTO ? RuneVersusCardTheme.CLAN_WAR : theme;
			drawBackground(g, resolvedTheme);
			g.setFont(new Font("Serif", Font.BOLD, 44));
			centerText(g, "RUNE VERSUS RECAP", WIDTH / 2, 64, resolvedTheme.getGold());
			g.setFont(new Font("SansSerif", Font.BOLD, 15));
			centerText(g, leaderboard.getLabel() + " | " + DATE_FORMAT.format(leaderboard.getCreatedAt()), WIDTH / 2, 92, MUTED);

			drawRecapHero(g, 58, 130, "PvM King", leaderboard.getTopBossKc(), "Boss KC", resolvedTheme.getLeftAccent());
			drawRecapHero(g, 430, 130, "Skilling King", leaderboard.getTopTotalXp(), "Total XP", resolvedTheme.getGold());
			drawRecapHero(g, 802, 130, "Collection Lord", leaderboard.getTopCollections(), "Collections", resolvedTheme.getRightAccent());

			drawRecapTable(g, 74, 338, "Top Boss KC", leaderboard.topByBossKc(6));
			drawRecapTable(g, 430, 338, "Top XP", leaderboard.topByTotalXp(6));
			drawRecapTable(g, 786, 338, "Current Form", leaderboard.topByWeekXp(6));

			g.setFont(new Font("SansSerif", Font.BOLD, 23));
			centerText(g, leaderboard.getHeadline(), WIDTH / 2, 642, TEXT);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	private static void drawBackground(Graphics2D g, RuneVersusCardTheme theme)
	{
		g.setPaint(new GradientPaint(0, 0, theme.getBackgroundTop(), 0, HEIGHT, theme.getBackgroundBottom()));
		g.fillRect(0, 0, WIDTH, HEIGHT);

		g.setComposite(AlphaComposite.SrcOver.derive(0.14f));
		g.setColor(theme.getGold());
		for (int x = -WIDTH; x < WIDTH * 2; x += 58)
		{
			g.drawLine(x, 0, x + 420, HEIGHT);
		}

		g.setComposite(AlphaComposite.SrcOver.derive(0.20f));
		g.setColor(theme.getLeftAccent());
		g.fillOval(70, 58, 210, 210);
		g.setColor(theme.getRightAccent());
		g.fillOval(930, 424, 220, 220);
		g.setComposite(AlphaComposite.SrcOver);
	}

	private static void drawHeader(Graphics2D g, DuelResult duel, RuneVersusCardTheme theme)
	{
		g.setFont(new Font("Serif", Font.BOLD, 46));
		centerText(g, "RUNE VERSUS", WIDTH / 2, 62, theme.getGold());
		g.setFont(new Font("SansSerif", Font.BOLD, 15));
		centerText(g, DATE_FORMAT.format(duel.getCreatedAt()), WIDTH / 2, 91, MUTED);

		g.setColor(new Color(255, 255, 255, 32));
		g.setStroke(new BasicStroke(1.4f));
		g.drawLine(325, 99, 875, 99);
	}

	private static void drawPlayerPanel(
		Graphics2D g,
		int x,
		int y,
		int w,
		int h,
		String name,
		int score,
		long bossKc,
		long totalXp,
		long dayXp,
		long weekXp,
		boolean left,
		RuneVersusCardTheme theme)
	{
		Color accent = left ? theme.getLeftAccent() : theme.getRightAccent();
		drawPanel(g, x, y, w, h, accent, theme.getGold());

		g.setFont(fitFont(g, name.toUpperCase(), Font.BOLD, 38, w - 42));
		g.setColor(TEXT);
		g.drawString(name.toUpperCase(), x + 24, y + 50);

		g.setFont(new Font("SansSerif", Font.BOLD, 12));
		g.setColor(MUTED);
		g.drawString(left ? "LEFT CONTENDER" : "RIGHT CONTENDER", x + 26, y + 74);

		g.setFont(new Font("Serif", Font.BOLD, 78));
		centerText(g, String.valueOf(score), x + w - 74, y + 92, accent);
		g.setFont(new Font("SansSerif", Font.BOLD, 12));
		centerText(g, "CROWNS", x + w - 74, y + 114, MUTED);

		drawTinyStat(g, x + 26, y + 122, "Boss KC", format(bossKc), accent);
		drawTinyStat(g, x + 206, y + 122, "Total XP", format(totalXp), accent);
		drawTinyStat(g, x + 26, y + 169, "24h XP", dayXp > 0 ? "+" + format(dayXp) : "locked", accent);
		drawTinyStat(g, x + 206, y + 169, "Week XP", weekXp > 0 ? "+" + format(weekXp) : "locked", accent);
	}

	private static void drawVersusCore(Graphics2D g, DuelResult duel, RuneVersusCardTheme theme)
	{
		int leftScore = duel.getLeftTotalWins();
		int rightScore = duel.getRightTotalWins();

		g.setColor(new Color(0, 0, 0, 120));
		g.fillOval(500, 150, 200, 200);
		g.setStroke(new BasicStroke(5f));
		g.setColor(theme.getGold());
		g.drawOval(505, 155, 190, 190);

		g.setFont(new Font("Serif", Font.BOLD, 42));
		centerText(g, leftScore + " - " + rightScore, WIDTH / 2, 230, TEXT);
		g.setFont(new Font("SansSerif", Font.BOLD, 18));
		centerText(g, "CATEGORY SCORE", WIDTH / 2, 263, MUTED);

		String winner = duel.getWinnerName();
		g.setFont(fitFont(g, winner.toUpperCase(), Font.BOLD, 26, 250));
		centerText(g, winner.toUpperCase(), WIDTH / 2, 307, theme.getGold());
	}

	private static void drawCategoryStrip(Graphics2D g, DuelResult duel)
	{
		int y = 372;
		drawScorePill(g, 58, y, "PvM", duel.getLeftBossWins(), duel.getRightBossWins());
		drawScorePill(g, 278, y, "Skills", duel.getLeftSkillWins(), duel.getRightSkillWins());
		drawScorePill(g, 498, y, "Activities", duel.getLeftActivityWins(), duel.getRightActivityWins());

		MetricResult clog = duel.getCollectionLogMetric();
		long leftClog = clog == null ? 0 : clog.getLeftValue();
		long rightClog = clog == null ? 0 : clog.getRightValue();
		drawScorePill(g, 718, y, "Clog", leftClog, rightClog);
		drawScorePill(g, 938, y, "Month XP", duel.getLeftMonthXp(), duel.getRightMonthXp());
	}

	private static void drawHighlights(Graphics2D g, DuelResult duel, RuneVersusCardTheme theme)
	{
		drawPanel(g, 58, 448, 520, 150, BLUE, theme.getGold());
		drawPanel(g, 622, 448, 520, 150, theme.getGold(), theme.getGold());

		MetricResult closest = duel.getClosestSteal();
		drawHighlight(g, 82, 478, "Closest Steal", closest, duel);

		MetricResult flex = duel.getBiggestFlex();
		drawHighlight(g, 646, 478, "Biggest Flex", flex, duel);

		List<MetricResult> topBosses = duel.getTopBosses(2);
		g.setFont(new Font("SansSerif", Font.BOLD, 14));
		g.setColor(MUTED);
		g.drawString("Boss gaps", 82, 570);
		drawCompactMetricList(g, topBosses, 164, 570);

		List<MetricResult> topSkills = duel.getTopSkills(2);
		g.setColor(MUTED);
		g.drawString("XP gaps", 646, 570);
		drawCompactMetricList(g, topSkills, 710, 570);
	}

	private static void drawHighlight(Graphics2D g, int x, int y, String title, MetricResult metric, DuelResult duel)
	{
		g.setFont(new Font("SansSerif", Font.BOLD, 15));
		g.setColor(GOLD);
		g.drawString(title.toUpperCase(), x, y);

		if (metric == null)
		{
			g.setFont(new Font("SansSerif", Font.PLAIN, 20));
			g.setColor(TEXT);
			g.drawString("No clear gap yet", x, y + 34);
			return;
		}

		String holder = metric.getWinner() == PlayerSide.LEFT ? duel.getLeft().getName() : duel.getRight().getName();
		g.setFont(fitFont(g, metric.getName(), Font.BOLD, 30, 388));
		g.setColor(TEXT);
		g.drawString(metric.getName(), x, y + 36);

		g.setFont(new Font("SansSerif", Font.BOLD, 16));
		g.setColor(MUTED);
		g.drawString(holder + " leads by " + format(metric.getGap()), x, y + 66);
	}

	private static void drawFooter(Graphics2D g, String verdict)
	{
		g.setFont(new Font("SansSerif", Font.BOLD, 22));
		centerText(g, verdict, WIDTH / 2, 648, TEXT);
	}

	private static void drawPanel(Graphics2D g, int x, int y, int w, int h, Color accent, Color border)
	{
		g.setColor(new Color(0, 0, 0, 90));
		g.fillRoundRect(x + 7, y + 9, w, h, 22, 22);
		g.setColor(PANEL);
		g.fillRoundRect(x, y, w, h, 22, 22);
		g.setStroke(new BasicStroke(2.2f));
		g.setColor(new Color(border.getRed(), border.getGreen(), border.getBlue(), 145));
		g.drawRoundRect(x, y, w, h, 22, 22);
		g.setColor(accent);
		g.fillRoundRect(x, y, 9, h, 10, 10);
	}

	private static void drawRecapHero(Graphics2D g, int x, int y, String title, RosterStanding standing, String metric, Color accent)
	{
		drawPanel(g, x, y, 340, 160, accent, GOLD);
		g.setFont(new Font("SansSerif", Font.BOLD, 15));
		g.setColor(GOLD);
		g.drawString(title.toUpperCase(), x + 24, y + 32);
		g.setFont(fitFont(g, standing == null ? "No data" : standing.getName(), Font.BOLD, 34, 292));
		g.setColor(TEXT);
		g.drawString(standing == null ? "No data" : standing.getName(), x + 24, y + 78);
		g.setFont(new Font("Serif", Font.BOLD, 42));
		g.setColor(accent);
		g.drawString(standing == null ? "-" : format(standing.valueFor(metric)), x + 24, y + 126);
		g.setFont(new Font("SansSerif", Font.BOLD, 12));
		g.setColor(MUTED);
		g.drawString(metric.toUpperCase(), x + 26, y + 145);
	}

	private static void drawRecapTable(Graphics2D g, int x, int y, String title, List<RosterStanding> standings)
	{
		drawPanel(g, x - 16, y - 32, 320, 240, BLUE, GOLD);
		g.setFont(new Font("SansSerif", Font.BOLD, 15));
		g.setColor(GOLD);
		g.drawString(title.toUpperCase(), x, y);
		g.setFont(new Font("SansSerif", Font.PLAIN, 16));
		int row = 30;
		int rank = 1;
		for (RosterStanding standing : standings)
		{
			String line = rank + ". " + standing.getName() + "  " + standing.bestDisplayFor(title);
			g.setColor(rank == 1 ? TEXT : MUTED);
			g.drawString(truncate(g, line, 270), x, y + row);
			row += 28;
			rank++;
		}
	}

	private static void drawTinyStat(Graphics2D g, int x, int y, String label, String value, Color accent)
	{
		g.setColor(new Color(255, 255, 255, 18));
		g.fillRoundRect(x, y, 158, 34, 12, 12);
		g.setFont(new Font("SansSerif", Font.BOLD, 11));
		g.setColor(MUTED);
		g.drawString(label.toUpperCase(), x + 12, y + 13);
		g.setFont(new Font("SansSerif", Font.BOLD, 17));
		g.setColor(accent);
		g.drawString(value, x + 12, y + 29);
	}

	private static void drawScorePill(Graphics2D g, int x, int y, String label, long left, long right)
	{
		g.setColor(new Color(255, 255, 255, 20));
		g.fillRoundRect(x, y, 202, 52, 18, 18);
		g.setStroke(new BasicStroke(1.2f));
		g.setColor(new Color(255, 255, 255, 38));
		g.drawRoundRect(x, y, 202, 52, 18, 18);

		g.setFont(new Font("SansSerif", Font.BOLD, 13));
		centerText(g, label.toUpperCase(), x + 101, y + 18, MUTED);
		g.setFont(new Font("Serif", Font.BOLD, 25));
		centerText(g, format(left) + " - " + format(right), x + 101, y + 43, TEXT);
	}

	private static void drawCompactMetricList(Graphics2D g, List<MetricResult> metrics, int x, int y)
	{
		g.setFont(new Font("SansSerif", Font.PLAIN, 13));
		g.setColor(TEXT);
		int offset = 0;
		for (MetricResult metric : metrics)
		{
			String s = metric.getName() + " +" + format(metric.getGap());
			g.drawString(truncate(g, s, 310), x, y + offset);
			offset += 16;
		}
	}

	private static Font fitFont(Graphics2D g, String text, int style, int startSize, int maxWidth)
	{
		int size = startSize;
		while (size > 12)
		{
			Font font = new Font("SansSerif", style, size);
			if (g.getFontMetrics(font).stringWidth(text) <= maxWidth)
			{
				return font;
			}
			size--;
		}
		return new Font("SansSerif", style, 12);
	}

	private static void centerText(Graphics2D g, String text, int centerX, int baselineY, Color color)
	{
		g.setColor(color);
		FontMetrics metrics = g.getFontMetrics();
		g.drawString(text, centerX - metrics.stringWidth(text) / 2, baselineY);
	}

	private static String truncate(Graphics2D g, String text, int maxWidth)
	{
		if (g.getFontMetrics().stringWidth(text) <= maxWidth)
		{
			return text;
		}
		String out = text;
		while (out.length() > 4 && g.getFontMetrics().stringWidth(out + "...") > maxWidth)
		{
			out = out.substring(0, out.length() - 1);
		}
		return out + "...";
	}

	private static String format(long value)
	{
		long absolute = Math.abs(value);
		if (absolute >= 1_000_000_000)
		{
			return String.format("%.1fb", value / 1_000_000_000.0);
		}
		if (absolute >= 1_000_000)
		{
			return String.format("%.1fm", value / 1_000_000.0);
		}
		if (absolute >= 10_000)
		{
			return String.format("%.1fk", value / 1_000.0);
		}
		return String.format("%,d", value);
	}
}
