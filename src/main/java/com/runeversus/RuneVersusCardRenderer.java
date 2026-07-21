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
import net.runelite.client.ui.FontManager;

@Singleton
public class RuneVersusCardRenderer
{
	private static final int WIDTH = 1200;
	private static final int HEIGHT = 675;
	private static final DateTimeFormatter DATE_FORMAT =
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

	private static final Color BACKGROUND_TOP = new Color(55, 45, 31);
	private static final Color BACKGROUND_BOTTOM = new Color(18, 14, 10);
	private static final Color PANEL = new Color(25, 22, 17, 246);
	private static final Color PANEL_INNER = new Color(12, 11, 9, 238);
	private static final Color STONE_LIGHT = new Color(126, 105, 72);
	private static final Color STONE_MID = new Color(75, 60, 42);
	private static final Color STONE_DARK = new Color(31, 24, 17);
	private static final Color GOLD = new Color(255, 152, 31);
	private static final Color RED = new Color(194, 67, 76);
	private static final Color TEAL = new Color(73, 190, 176);
	private static final Color BLUE = new Color(80, 132, 213);
	private static final Color TEXT = new Color(255, 255, 255);
	private static final Color MUTED = new Color(196, 180, 151);

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
			g.setPaint(new GradientPaint(0, 0, BACKGROUND_TOP, 48, 48, BACKGROUND_BOTTOM));
			g.fillRect(1, 1, 46, 46);
			drawBevelFrame(g, 2, 2, 43, 43, GOLD, false);
			g.setFont(osrsBold(20));
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
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	}

	public BufferedImage renderClanProgress(ClanProgressLeaderboard leaderboard, RuneVersusCardTheme theme)
	{
		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			setup(g);
			RuneVersusCardTheme resolvedTheme = theme == RuneVersusCardTheme.AUTO ? RuneVersusCardTheme.CLAN_WAR : theme;
			drawBackground(g, resolvedTheme);

			drawTitlePlaque(g, 355, 20, 490, 82, resolvedTheme.getGold());
			g.setFont(osrsBold(31));
			centerText(g, "RuneVersus", WIDTH / 2, 49, resolvedTheme.getGold());
			g.setFont(osrsBold(22));
			centerText(g, leaderboard.getGroupType().getCardTitle(), WIDTH / 2, 73, TEXT);
			g.setFont(osrsRegular(14));
			centerText(g, leaderboard.getLabel() + " • " + leaderboard.getPlayers().size() + " tracked players • "
				+ DATE_FORMAT.format(leaderboard.getCreatedAt()), WIDTH / 2, 94, MUTED);

			drawProgressHeader(g, 346, "XP / XP GAIN", resolvedTheme.getGold());
			drawProgressHeader(g, 661, "CLOGS / GAIN", TEAL);
			drawProgressHeader(g, 976, "BOSS KC / GAIN", RED);

			int rowY = 145;
			for (GainPeriod period : GainPeriod.values())
			{
				drawPeriodBadge(g, 58, rowY, period, resolvedTheme.getGold());
				drawProgressCell(g, 200, rowY, 292, 74, leaderboard, period,
					ClanProgressMetric.XP, resolvedTheme.getGold());
				drawProgressCell(g, 515, rowY, 292, 74, leaderboard, period,
					ClanProgressMetric.COLLECTIONS, TEAL);
				drawProgressCell(g, 830, rowY, 312, 74, leaderboard, period,
					ClanProgressMetric.BOSS_KC, RED);
				rowY += 86;
			}

			g.setFont(osrsRegular(15));
			centerText(g, "Wise Old Man snapshots • Boss KC is summed across every boss • "
				+ leaderboard.getSourceDescription(), WIDTH / 2, 604, MUTED);
			g.setFont(osrsBold(19));
			centerText(g, leaderboard.getGroupType().getCardTagline(), WIDTH / 2, 641, TEXT);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	private static void drawBackground(Graphics2D g, RuneVersusCardTheme theme)
	{
		g.setPaint(new GradientPaint(0, 0, BACKGROUND_TOP, 0, HEIGHT, BACKGROUND_BOTTOM));
		g.fillRect(0, 0, WIDTH, HEIGHT);

		// A deterministic, low-contrast stone texture keeps the image OSRS-like without external assets.
		g.setComposite(AlphaComposite.SrcOver.derive(0.13f));
		for (int y = 18; y < HEIGHT - 18; y += 9)
		{
			for (int x = 18 + (y % 17); x < WIDTH - 18; x += 19)
			{
				int shade = 70 + Math.abs((x * 17 + y * 31) % 45);
				g.setColor(new Color(shade, shade - 12, shade - 28));
				g.fillRect(x, y, 2, 1);
			}
		}

		g.setComposite(AlphaComposite.SrcOver);
		drawBevelFrame(g, 10, 10, WIDTH - 21, HEIGHT - 21, theme.getGold(), true);
	}

	private static void drawHeader(Graphics2D g, DuelResult duel, RuneVersusCardTheme theme)
	{
		drawTitlePlaque(g, 370, 22, 460, 78, theme.getGold());
		g.setFont(osrsBold(43));
		centerText(g, "RuneVersus", WIDTH / 2, 65, theme.getGold());
		g.setFont(osrsRegular(15));
		centerText(g, DATE_FORMAT.format(duel.getCreatedAt()), WIDTH / 2, 91, MUTED);
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

		g.setFont(osrsRegular(13));
		g.setColor(MUTED);
		g.drawString(left ? "LEFT CONTENDER" : "RIGHT CONTENDER", x + 26, y + 74);

		g.setFont(osrsBold(76));
		centerText(g, String.valueOf(score), x + w - 74, y + 92, accent);
		g.setFont(osrsRegular(13));
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

		g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(STONE_DARK);
		g.drawLine(535, 169, 668, 326);
		g.drawLine(665, 169, 532, 326);
		g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(STONE_LIGHT);
		g.drawLine(535, 169, 668, 326);
		g.drawLine(665, 169, 532, 326);

		Path2D shield = new Path2D.Double();
		shield.moveTo(600, 148);
		shield.lineTo(676, 176);
		shield.lineTo(685, 255);
		shield.lineTo(650, 320);
		shield.lineTo(600, 348);
		shield.lineTo(550, 320);
		shield.lineTo(515, 255);
		shield.lineTo(524, 176);
		shield.closePath();
		g.setColor(new Color(0, 0, 0, 150));
		g.fill(shield);
		g.setStroke(new BasicStroke(7f));
		g.setColor(STONE_DARK);
		g.draw(shield);
		g.setStroke(new BasicStroke(3f));
		g.setColor(theme.getGold());
		g.draw(shield);

		g.setFont(osrsBold(43));
		centerText(g, leftScore + " - " + rightScore, WIDTH / 2, 230, TEXT);
		g.setFont(osrsRegular(19));
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
		g.setFont(osrsRegular(15));
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
		g.setFont(osrsBold(16));
		g.setColor(GOLD);
		g.drawString(title.toUpperCase(), x, y);

		if (metric == null)
		{
			g.setFont(osrsRegular(21));
			g.setColor(TEXT);
			g.drawString("No clear gap yet", x, y + 34);
			return;
		}

		String holder = metric.getWinner() == PlayerSide.LEFT ? duel.getLeft().getName() : duel.getRight().getName();
		g.setFont(fitFont(g, metric.getName(), Font.BOLD, 30, 388));
		g.setColor(TEXT);
		g.drawString(metric.getName(), x, y + 36);

		g.setFont(osrsRegular(17));
		g.setColor(MUTED);
		g.drawString(holder + " leads by " + format(metric.getGap()), x, y + 66);
	}

	private static void drawFooter(Graphics2D g, String verdict)
	{
		g.setFont(osrsBold(23));
		centerText(g, verdict, WIDTH / 2, 648, TEXT);
	}

	private static void drawPanel(Graphics2D g, int x, int y, int w, int h, Color accent, Color border)
	{
		g.setColor(new Color(0, 0, 0, 130));
		g.fillRect(x + 7, y + 8, w, h);
		g.setColor(PANEL);
		g.fillRect(x, y, w, h);
		drawBevelFrame(g, x, y, w, h, border, false);
		g.setColor(accent);
		g.fillRect(x + 7, y + 7, 6, h - 13);
		g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));
		g.drawLine(x + 15, y + 8, x + 15, y + h - 8);
	}

	private static void drawTitlePlaque(Graphics2D g, int x, int y, int w, int h, Color accent)
	{
		g.setColor(new Color(0, 0, 0, 120));
		g.fillRect(x + 6, y + 7, w, h);
		g.setColor(PANEL);
		g.fillRect(x, y, w, h);
		drawBevelFrame(g, x, y, w, h, accent, false);
		drawRivet(g, x + 13, y + 13);
		drawRivet(g, x + w - 13, y + 13);
		drawRivet(g, x + 13, y + h - 13);
		drawRivet(g, x + w - 13, y + h - 13);
	}

	private static void drawBevelFrame(Graphics2D g, int x, int y, int w, int h, Color accent, boolean rivets)
	{
		g.setStroke(new BasicStroke(5f));
		g.setColor(STONE_DARK);
		g.drawRect(x, y, w, h);
		g.setStroke(new BasicStroke(2f));
		g.setColor(STONE_LIGHT);
		g.drawLine(x + 3, y + 3, x + w - 3, y + 3);
		g.drawLine(x + 3, y + 3, x + 3, y + h - 3);
		g.setColor(STONE_DARK);
		g.drawLine(x + 3, y + h - 3, x + w - 3, y + h - 3);
		g.drawLine(x + w - 3, y + 3, x + w - 3, y + h - 3);
		g.setStroke(new BasicStroke(1.3f));
		g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 155));
		g.drawRect(x + 6, y + 6, w - 12, h - 12);
		if (rivets)
		{
			drawRivet(g, x + 12, y + 12);
			drawRivet(g, x + w - 12, y + 12);
			drawRivet(g, x + 12, y + h - 12);
			drawRivet(g, x + w - 12, y + h - 12);
		}
	}

	private static void drawRivet(Graphics2D g, int x, int y)
	{
		g.setColor(STONE_DARK);
		g.fillOval(x - 5, y - 5, 10, 10);
		g.setColor(STONE_LIGHT);
		g.fillOval(x - 3, y - 3, 6, 6);
		g.setColor(STONE_MID);
		g.drawLine(x - 2, y, x + 2, y);
	}

	private static void drawInsetBox(Graphics2D g, int x, int y, int w, int h, Color accent)
	{
		g.setColor(PANEL_INNER);
		g.fillRect(x, y, w, h);
		g.setStroke(new BasicStroke(2f));
		g.setColor(STONE_DARK);
		g.drawLine(x, y, x + w, y);
		g.drawLine(x, y, x, y + h);
		g.setColor(STONE_MID);
		g.drawLine(x, y + h, x + w, y + h);
		g.drawLine(x + w, y, x + w, y + h);
		g.setStroke(new BasicStroke(1f));
		g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 115));
		g.drawRect(x + 3, y + 3, w - 6, h - 6);
	}

	private static void drawProgressHeader(Graphics2D g, int centerX, String label, Color color)
	{
		g.setFont(osrsBold(16));
		centerText(g, label, centerX, 127, color);
	}

	private static void drawPeriodBadge(Graphics2D g, int x, int y, GainPeriod period, Color accent)
	{
		drawInsetBox(g, x, y, 122, 74, accent);
		g.setFont(osrsBold(period == GainPeriod.DAY ? 25 : 19));
		centerText(g, period.getLabel().toUpperCase(), x + 61, y + 46, TEXT);
	}

	private static void drawProgressCell(
		Graphics2D g,
		int x,
		int y,
		int width,
		int height,
		ClanProgressLeaderboard leaderboard,
		GainPeriod period,
		ClanProgressMetric metric,
		Color accent)
	{
		drawInsetBox(g, x, y, width, height, accent);
		g.setColor(accent);
		g.fillRect(x + 4, y + 4, 5, height - 7);

		ClanProgressPlayer leader = leaderboard.getLeader(period, metric);
		String name = leader == null ? "No gain" : leader.getName();
		String value = leader == null ? "—" : ClanProgressLeaderboard.valueText(
			period, leader.getGains(period).valueFor(metric));
		g.setFont(fitFont(g, name, Font.BOLD, 25, width - 42));
		g.setColor(TEXT);
		g.drawString(name, x + 22, y + 31);
		g.setFont(osrsBold(26));
		g.setColor(accent);
		g.drawString(value, x + 22, y + 61);
	}

	private static void drawTinyStat(Graphics2D g, int x, int y, String label, String value, Color accent)
	{
		drawInsetBox(g, x, y, 158, 34, accent);
		g.setFont(osrsRegular(12));
		g.setColor(MUTED);
		g.drawString(label.toUpperCase(), x + 12, y + 13);
		g.setFont(osrsBold(18));
		g.setColor(accent);
		g.drawString(value, x + 12, y + 29);
	}

	private static void drawScorePill(Graphics2D g, int x, int y, String label, long left, long right)
	{
		drawInsetBox(g, x, y, 202, 52, GOLD);

		g.setFont(osrsRegular(14));
		centerText(g, label.toUpperCase(), x + 101, y + 18, MUTED);
		g.setFont(osrsBold(26));
		centerText(g, format(left) + " - " + format(right), x + 101, y + 43, TEXT);
	}

	private static void drawCompactMetricList(Graphics2D g, List<MetricResult> metrics, int x, int y)
	{
		g.setFont(osrsRegular(14));
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
			Font font = style == Font.BOLD ? osrsBold(size) : osrsRegular(size);
			if (g.getFontMetrics(font).stringWidth(text) <= maxWidth)
			{
				return font;
			}
			size--;
		}
		return style == Font.BOLD ? osrsBold(12) : osrsRegular(12);
	}

	private static Font osrsBold(float size)
	{
		return FontManager.getRunescapeBoldFont().deriveFont(size);
	}

	private static Font osrsRegular(float size)
	{
		return FontManager.getRunescapeFont().deriveFont(size);
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
