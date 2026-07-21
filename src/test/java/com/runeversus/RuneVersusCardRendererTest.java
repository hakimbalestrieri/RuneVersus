package com.runeversus;

import com.runeversus.model.DuelResult;
import com.runeversus.model.MetricResult;
import com.runeversus.model.MetricType;
import com.runeversus.model.PlayerProfile;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class RuneVersusCardRendererTest
{
	@Test
	public void rendersNonBlankDuelCard()
	{
		DuelResult duel = new DuelResult(
			new PlayerProfile("Hakim", null),
			new PlayerProfile("Rival", null),
			Arrays.asList(
				new MetricResult(MetricType.SKILL, "Attack", 20_000_000, 13_000_000),
				new MetricResult(MetricType.SKILL, "Magic", 15_000_000, 32_000_000)),
			Arrays.asList(
				new MetricResult(MetricType.BOSS, "Vorkath", 1200, 930),
				new MetricResult(MetricType.BOSS, "Zulrah", 54, 2200)),
			Collections.singletonList(new MetricResult(MetricType.COLLECTION_LOG, "Collections Logged", 890, 820)),
			Collections.singletonList(new MetricResult(MetricType.FORM_DAY, "Overall", 240_000, 110_000)),
			Collections.emptyList(),
			Collections.emptyList());

		BufferedImage image = new RuneVersusCardRenderer().render(duel);
		Assert.assertEquals(1200, image.getWidth());
		Assert.assertEquals(675, image.getHeight());

		int differentPixels = 0;
		int first = image.getRGB(0, 0);
		for (int y = 0; y < image.getHeight(); y += 25)
		{
			for (int x = 0; x < image.getWidth(); x += 25)
			{
				if (image.getRGB(x, y) != first)
				{
					differentPixels++;
				}
			}
		}
		Assert.assertTrue(differentPixels > 50);
	}

	@Test
	public void rendersNonBlankClanProgressCard()
	{
		Map<GainPeriod, ClanProgressGains> gains = new EnumMap<>(GainPeriod.class);
		gains.put(GainPeriod.DAY, new ClanProgressGains(2_800_000, 4, 86));
		gains.put(GainPeriod.WEEK, new ClanProgressGains(12_400_000, 9, 311));
		gains.put(GainPeriod.MONTH, new ClanProgressGains(48_000_000, 22, 840));
		gains.put(GainPeriod.YEAR, new ClanProgressGains(510_000_000, 117, 4_800));
		ClanProgressLeaderboard leaderboard = new ClanProgressLeaderboard(
			"Clan progress", 42,
			Collections.singletonList(new ClanProgressPlayer("Hakim", gains)));

		BufferedImage image = new RuneVersusCardRenderer().renderClanProgress(
			leaderboard, RuneVersusCardTheme.CLAN_WAR);
		Assert.assertEquals(1200, image.getWidth());
		Assert.assertEquals(675, image.getHeight());
		Assert.assertNotEquals(image.getRGB(0, 0), image.getRGB(346, 180));
	}

	@Test
	public void rendersNonBlankMonthlyLeagueCard()
	{
		BufferedImage image = new RuneVersusCardRenderer().renderMonthlyLeague(
			MonthlyLeaguePanelTest.sampleSeason(), RuneVersusCardTheme.CLAN_WAR);
		Assert.assertEquals(1200, image.getWidth());
		Assert.assertEquals(675, image.getHeight());
		Assert.assertNotEquals(image.getRGB(0, 0), image.getRGB(600, 220));
	}
}
