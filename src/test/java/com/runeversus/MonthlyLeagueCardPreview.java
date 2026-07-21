package com.runeversus;

import java.io.File;
import javax.imageio.ImageIO;

public final class MonthlyLeagueCardPreview
{
	private MonthlyLeagueCardPreview()
	{
	}

	public static void main(String[] args) throws Exception
	{
		File output = new File("build/previews/monthly-league-card.png");
		output.getParentFile().mkdirs();
		ImageIO.write(new RuneVersusCardRenderer().renderMonthlyLeague(
			MonthlyLeaguePanelTest.sampleSeason(), RuneVersusCardTheme.CLAN_WAR), "png", output);
		System.out.println(output.getAbsolutePath());
	}
}
