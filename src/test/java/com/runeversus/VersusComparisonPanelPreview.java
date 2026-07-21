package com.runeversus;

import com.runeversus.model.DuelResult;
import com.runeversus.model.MetricResult;
import com.runeversus.model.MetricType;
import com.runeversus.model.PlayerProfile;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import net.runelite.client.game.SkillIconManager;

public class VersusComparisonPanelPreview
{
	public static void main(String[] args) throws Exception
	{
		File out = new File(args.length == 0
			? "build/previews/runeversus-versus-window-preview.png"
			: args[0]);
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				VersusComparisonPanel panel = new VersusComparisonPanel(
					"Close", () -> { }, () -> { }, () -> { },
					new RuneVersusIcons(new SkillIconManager(), null));
				panel.setResult(sampleResult(), null, "Elyas5 takes the overall lead.");
				panel.selectCategory(VersusComparisonPanel.Category.SKILLS);
				panel.setSize(1000, 820);
				layout(panel);
				BufferedImage image = new BufferedImage(1000, 820, BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = image.createGraphics();
				panel.printAll(graphics);
				graphics.dispose();
				File parent = out.getParentFile();
				if (parent != null && !parent.exists())
				{
					parent.mkdirs();
				}
				ImageIO.write(image, "png", out);
			}
			catch (Exception ex)
			{
				throw new IllegalStateException(ex);
			}
		});
		System.out.println(out.getAbsolutePath());
	}

	private static DuelResult sampleResult()
	{
		String[] names = {"Attack", "Strength", "Defence", "Hitpoints", "Ranged", "Prayer", "Magic",
			"Cooking", "Woodcutting", "Fletching", "Fishing", "Firemaking", "Crafting", "Smithing",
			"Mining", "Herblore", "Agility", "Thieving", "Slayer", "Farming", "Runecraft", "Hunter"};
		List<MetricResult> skills = new ArrayList<>();
		for (int i = 0; i < names.length; i++)
		{
			long left = i == 0 ? 35_700_000L : 8_000_000L + i * 1_420_000L;
			long right = i == 0 ? 59_800_000L : 37_000_000L - i * 780_000L;
			skills.add(new MetricResult(MetricType.SKILL, names[i], left, right));
		}
		return new DuelResult(
			new PlayerProfile("TalkMeFrench", null),
			new PlayerProfile("Elyas5", null),
			skills,
			Arrays.asList(
				new MetricResult(MetricType.BOSS, "Vorkath", 1_284, 927),
				new MetricResult(MetricType.BOSS, "Zulrah", 61, 2_212)),
			Arrays.asList(
				new MetricResult(MetricType.COLLECTION_LOG, "Collections Logged", 914, 872),
				new MetricResult(MetricType.COMBAT_ACHIEVEMENTS, "Combat Achievements", 5, 6)),
			Collections.singletonList(new MetricResult(MetricType.FORM_DAY, "Overall", 2_840_000, 1_120_000)),
			Collections.singletonList(new MetricResult(MetricType.FORM_WEEK, "Overall", 8_120_000, 11_480_000)),
			Collections.singletonList(new MetricResult(MetricType.FORM_MONTH, "Overall", 44_800_000, 39_250_000)),
			Instant.parse("2026-07-21T17:00:00Z"));
	}

	private static void layout(java.awt.Container root)
	{
		root.doLayout();
		for (java.awt.Component component : root.getComponents())
		{
			if (component instanceof java.awt.Container)
			{
				layout((java.awt.Container) component);
			}
		}
	}
}
