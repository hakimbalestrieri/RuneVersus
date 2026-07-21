package com.runeversus;

import com.runeversus.model.DuelResult;
import com.runeversus.model.MetricResult;
import com.runeversus.model.MetricType;
import com.runeversus.model.PlayerProfile;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import javax.imageio.ImageIO;
import net.runelite.client.ui.FontManager;

public final class DiscordUseCasePreview
{
	private static final int WIDTH = 1200;
	private static final Color BACKGROUND = new Color(36, 36, 36);
	private static final Color PANEL = new Color(28, 28, 28);
	private static final Color PANEL_LIGHT = new Color(42, 42, 42);
	private static final Color BORDER = new Color(82, 82, 82);
	private static final Color GOLD = new Color(255, 193, 7);
	private static final Color ORANGE = new Color(255, 144, 64);
	private static final Color TEXT = new Color(224, 224, 224);
	private static final Color MUTED = new Color(170, 170, 170);

	private DiscordUseCasePreview()
	{
	}

	public static void main(String[] args) throws Exception
	{
		File directory = new File(args.length == 0 ? "build/discord-announcement" : args[0]);
		if (!directory.exists() && !directory.mkdirs())
		{
			throw new IllegalStateException("Could not create " + directory.getAbsolutePath());
		}

		File rightClick = new File(directory, "right-click-comparison.png");
		File chatCommand = new File(directory, "chat-command.png");
		ImageIO.write(renderRightClick(), "png", rightClick);
		ImageIO.write(renderChatCommand(), "png", chatCommand);
		System.out.println(rightClick.getAbsolutePath());
		System.out.println(chatCommand.getAbsolutePath());
	}

	private static BufferedImage renderRightClick()
	{
		BufferedImage image = canvas(675);
		Graphics2D graphics = graphics(image);
		drawHeader(graphics, "RIGHT-CLICK COMPARISON",
			"Start a matchup directly from a visible player or clan member.");

		drawPanel(graphics, 42, 122, 500, 440);
		drawCentered(graphics, "CLAN MEMBERS", bold(22f), GOLD, 292, 160);
		drawCentered(graphics, "Right-click any member", regular(17f), MUTED, 292, 188);
		String[] members = {"TalkMeFrench", "Elyas5", "Raid Captain", "Clog Hunter", "Clan Member 5"};
		for (int index = 0; index < members.length; index++)
		{
			int rowY = 212 + index * 61;
			if (index == 1)
			{
				graphics.setColor(new Color(69, 60, 38));
				graphics.fillRect(61, rowY, 462, 52);
				graphics.setColor(GOLD);
				graphics.drawRect(61, rowY, 461, 51);
			}
			else
			{
				graphics.setColor(new Color(55, 55, 55));
				graphics.drawLine(61, rowY + 52, 522, rowY + 52);
			}
			graphics.setColor(index < 3 ? new Color(255, 201, 87) : new Color(142, 191, 239));
			graphics.fillOval(78, rowY + 14, 18, 18);
			graphics.setFont(bold(19f));
			graphics.setColor(index == 1 ? Color.WHITE : TEXT);
			graphics.drawString(members[index], 115, rowY + 31);
			graphics.setFont(regular(15f));
			graphics.setColor(MUTED);
			graphics.drawString(index == 1 ? "Right-clicked" : "Clan member", 370, rowY + 30);
		}

		drawArrow(graphics, 567, 335, 645, 335);
		drawContextMenu(graphics, 674, 142, "Elyas5");

		graphics.setColor(new Color(25, 25, 25));
		graphics.fillRoundRect(155, 590, 890, 52, 8, 8);
		graphics.setColor(GOLD);
		graphics.drawRoundRect(155, 590, 889, 51, 8, 8);
		drawCentered(graphics, "Select VS Compare to compare Elyas5 with your logged-in character.",
			bold(18f), TEXT, WIDTH / 2, 622);
		graphics.dispose();
		return image;
	}

	private static BufferedImage renderChatCommand()
	{
		BufferedImage image = canvas(590);
		Graphics2D graphics = graphics(image);
		drawHeader(graphics, "QUICK CHAT COMPARISON",
			"One command produces one compact result line inside RuneLite.");

		drawPanel(graphics, 42, 126, 1116, 366);
		graphics.setColor(new Color(72, 62, 47));
		graphics.fillRect(62, 146, 1076, 258);
		graphics.setPaint(new GradientPaint(62, 146, new Color(91, 77, 57), 62, 404,
			new Color(58, 51, 40)));
		graphics.fillRect(62, 146, 1076, 258);
		graphics.setColor(new Color(160, 138, 96));
		graphics.drawRect(62, 146, 1075, 257);

		graphics.setFont(bold(19f));
		graphics.setColor(new Color(144, 205, 255));
		graphics.drawString("TalkMeFrench:", 88, 202);
		graphics.setColor(Color.WHITE);
		graphics.drawString("!vs Elyas5", 227, 202);

		graphics.setFont(regular(18f));
		graphics.setColor(GOLD);
		graphics.drawString("[RuneVersus]", 88, 252);
		graphics.setColor(TEXT);
		graphics.drawString("Generating TalkMeFrench vs Elyas5...", 218, 252);

		String result = RuneVersusChatFormatter.format(chatResult());
		graphics.setColor(new Color(255, 152, 31));
		graphics.fillRoundRect(80, 286, 1040, 62, 8, 8);
		graphics.setColor(new Color(43, 35, 25));
		graphics.fillRoundRect(83, 289, 1034, 56, 7, 7);
		drawCentered(graphics, result, bold(20f), Color.WHITE, WIDTH / 2, 326);

		graphics.setFont(regular(15f));
		graphics.setColor(new Color(222, 204, 164));
		graphics.drawString("The result is rendered locally. RuneVersus never sends an automatic reply for you.",
			88, 382);

		String[] tabs = {"All", "Game", "Public", "Private", "Clan", "Trade", "Report"};
		int tabX = 67;
		for (String tab : tabs)
		{
			int tabWidth = tab.equals("Private") ? 132 : 118;
			graphics.setColor(tab.equals("Clan") ? new Color(73, 62, 39) : new Color(40, 40, 40));
			graphics.fillRect(tabX, 423, tabWidth - 7, 42);
			graphics.setColor(tab.equals("Clan") ? GOLD : BORDER);
			graphics.drawRect(tabX, 423, tabWidth - 7, 42);
			drawCentered(graphics, tab, bold(16f), tab.equals("Clan") ? GOLD : TEXT,
				tabX + (tabWidth - 7) / 2, 450);
			tabX += tabWidth;
		}

		graphics.setColor(new Color(25, 25, 25));
		graphics.fillRoundRect(155, 515, 890, 48, 8, 8);
		graphics.setColor(GOLD);
		graphics.drawRoundRect(155, 515, 889, 47, 8, 8);
		drawCentered(graphics, "Use !vs <player>, !vs Alice, Bob, or quoted names with spaces.",
			bold(18f), TEXT, WIDTH / 2, 545);
		graphics.dispose();
		return image;
	}

	private static DuelResult chatResult()
	{
		return new DuelResult(
			new PlayerProfile("TalkMeFrench", null),
			new PlayerProfile("Elyas5", null),
			Collections.singletonList(new MetricResult(
				MetricType.SKILL, "Attack", 318_500_000L, 443_400_000L)),
			Collections.emptyList(),
			Collections.singletonList(new MetricResult(
				MetricType.COLLECTION_LOG, "Collections Logged", 872, 914)),
			Collections.emptyList(),
			Collections.emptyList(),
			Collections.emptyList());
	}

	private static void drawContextMenu(Graphics2D graphics, int x, int y, String target)
	{
		int width = 450;
		int height = 378;
		graphics.setColor(new Color(16, 13, 10));
		graphics.fillRect(x, y, width, height);
		graphics.setColor(new Color(91, 77, 49));
		graphics.setStroke(new BasicStroke(3f));
		graphics.drawRect(x + 1, y + 1, width - 3, height - 3);
		graphics.setStroke(new BasicStroke(1f));
		drawCentered(graphics, "Choose Option", bold(20f), new Color(95, 214, 255),
			x + width / 2, y + 34);

		String[] options = {"VS Compare", "VS Set A", "VS Set B", "Lookup", "Message", "Cancel"};
		for (int index = 0; index < options.length; index++)
		{
			int optionY = y + 54 + index * 49;
			boolean compare = index == 0;
			if (compare)
			{
				graphics.setColor(new Color(74, 59, 29));
				graphics.fillRect(x + 10, optionY - 1, width - 20, 43);
				graphics.setColor(GOLD);
				graphics.drawRect(x + 10, optionY - 1, width - 21, 42);
			}
			graphics.setFont(bold(19f));
			graphics.setColor(compare ? GOLD : TEXT);
			String option = options[index];
			int textX = x + 28;
			graphics.drawString(option, textX, optionY + 27);
			if (!"Cancel".equals(option))
			{
				int optionWidth = graphics.getFontMetrics().stringWidth(option);
				graphics.setColor(ORANGE);
				graphics.drawString(target, textX + optionWidth + 12, optionY + 27);
			}
		}

		graphics.setColor(GOLD);
		graphics.fillOval(x - 10, y + 65, 20, 20);
		graphics.setColor(new Color(20, 20, 20));
		graphics.fillOval(x - 4, y + 71, 8, 8);
	}

	private static BufferedImage canvas(int height)
	{
		return new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB);
	}

	private static Graphics2D graphics(BufferedImage image)
	{
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		graphics.setColor(BACKGROUND);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
		return graphics;
	}

	private static void drawHeader(Graphics2D graphics, String title, String subtitle)
	{
		graphics.setColor(new Color(31, 31, 31));
		graphics.fillRect(0, 0, WIDTH, 100);
		drawCentered(graphics, title, bold(30f), GOLD, WIDTH / 2, 45);
		drawCentered(graphics, subtitle, regular(17f), MUTED, WIDTH / 2, 76);
		graphics.setColor(new Color(255, 193, 7, 140));
		graphics.fillRect(42, 98, WIDTH - 84, 2);
	}

	private static void drawPanel(Graphics2D graphics, int x, int y, int width, int height)
	{
		graphics.setColor(PANEL);
		graphics.fillRoundRect(x, y, width, height, 8, 8);
		graphics.setColor(BORDER);
		graphics.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
		graphics.setColor(PANEL_LIGHT);
		graphics.drawLine(x + 1, y + 1, x + width - 2, y + 1);
	}

	private static void drawArrow(Graphics2D graphics, int startX, int startY, int endX, int endY)
	{
		graphics.setColor(GOLD);
		graphics.setStroke(new BasicStroke(4f));
		graphics.drawLine(startX, startY, endX - 18, endY);
		Polygon arrow = new Polygon();
		arrow.addPoint(endX, endY);
		arrow.addPoint(endX - 21, endY - 12);
		arrow.addPoint(endX - 21, endY + 12);
		graphics.fillPolygon(arrow);
		graphics.setStroke(new BasicStroke(1f));
	}

	private static void drawCentered(
		Graphics2D graphics,
		String text,
		Font font,
		Color color,
		int centerX,
		int baseline)
	{
		graphics.setFont(font);
		graphics.setColor(color);
		FontMetrics metrics = graphics.getFontMetrics();
		graphics.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
	}

	private static Font bold(float size)
	{
		return FontManager.getRunescapeBoldFont().deriveFont(size);
	}

	private static Font regular(float size)
	{
		return FontManager.getRunescapeFont().deriveFont(size);
	}
}
