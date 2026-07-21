package com.runeversus;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public class ClanProgressPanelPreview
{
	public static void main(String[] args) throws Exception
	{
		File out = new File(args.length == 0
			? "build/previews/runeversus-clan-panel-preview.png"
			: args[0]);
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				ClanProgressPanel panel = new ClanProgressPanel(() -> { }, () -> { }, ignored -> { }, () -> { });
				panel.setLeaderboard(sampleLeaderboard());
				panel.selectPeriod(GainPeriod.ALL_TIME);
				panel.selectBoss("Vorkath");
				panel.setSize(1100, 820);
				layout(panel);
				BufferedImage image = new BufferedImage(1100, 820, BufferedImage.TYPE_INT_ARGB);
				panel.printAll(image.createGraphics());
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

	private static ClanProgressLeaderboard sampleLeaderboard()
	{
		List<ClanProgressPlayer> players = new ArrayList<>();
		for (int i = 1; i <= 18; i++)
		{
			Map<GainPeriod, ClanProgressGains> gains = new EnumMap<>(GainPeriod.class);
			for (GainPeriod period : GainPeriod.values())
			{
				long multiplier = period.ordinal() + 1L;
				gains.put(period, new ClanProgressGains(
					(19L - i) * 740_000L * multiplier,
					(i * 3L + period.ordinal()) % 11L,
					java.util.Map.of(
						"Vorkath", (19L - i) * 5L * multiplier,
						"Zulrah", (19L - i) * 3L * multiplier)));
			}
			players.add(new ClanProgressPlayer(i == 1 ? "Raid Captain" : "Clan Member " + i, gains));
		}
		return new ClanProgressLeaderboard("Clan progress", 139, players);
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
