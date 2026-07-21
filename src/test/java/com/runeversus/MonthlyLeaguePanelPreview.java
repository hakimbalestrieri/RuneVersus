package com.runeversus;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public final class MonthlyLeaguePanelPreview
{
	private MonthlyLeaguePanelPreview()
	{
	}

	public static void main(String[] args) throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				MonthlyLeaguePanel panel = new MonthlyLeaguePanel(
					() -> { }, ignored -> { }, () -> { }, ignored -> { }, () -> { });
				panel.setSeason(MonthlyLeaguePanelTest.sampleSeason());
				panel.setSize(1180, 860);
				layout(panel);
				BufferedImage image = new BufferedImage(1180, 860, BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = image.createGraphics();
				panel.paint(graphics);
				graphics.dispose();
				File output = new File(args.length == 0
					? "build/previews/monthly-league-panel.png"
					: args[0]);
				output.getParentFile().mkdirs();
				ImageIO.write(image, "png", output);
				System.out.println(output.getAbsolutePath());
			}
			catch (Exception ex)
			{
				throw new IllegalStateException(ex);
			}
		});
	}

	private static void layout(Container root)
	{
		root.doLayout();
		for (Component component : root.getComponents())
		{
			if (component instanceof Container)
			{
				layout((Container) component);
			}
		}
	}
}
