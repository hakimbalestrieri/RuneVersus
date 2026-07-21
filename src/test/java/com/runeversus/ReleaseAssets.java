package com.runeversus;

import java.io.File;
import javax.imageio.ImageIO;

public final class ReleaseAssets
{
	private ReleaseAssets()
	{
	}

	public static void main(String[] args) throws Exception
	{
		File images = new File("docs/images");
		if (!images.exists() && !images.mkdirs())
		{
			throw new IllegalStateException("Could not create " + images.getAbsolutePath());
		}

		VersusComparisonPanelPreview.main(new String[]{
			new File(images, "player-comparison.png").getPath()});
		MonthlyLeaguePanelPreview.main(new String[]{
			new File(images, "monthly-league.png").getPath()});
		DuelCardPreview.main(new String[]{
			new File(images, "duel-card.png").getPath()});
		ImageIO.write(new RuneVersusCardRenderer().renderIcon(), "png", new File("icon.png"));
	}
}
