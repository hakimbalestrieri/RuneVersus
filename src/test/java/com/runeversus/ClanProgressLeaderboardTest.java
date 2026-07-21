package com.runeversus;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ClanProgressLeaderboardTest
{
	@Test
	public void selectsASeparateLeaderForEveryPeriodAndMetric()
	{
		ClanProgressPlayer alice = player("Alice",
			gains(GainPeriod.DAY, 100, 3, 40),
			gains(GainPeriod.WEEK, 900, 8, 100),
			gains(GainPeriod.MONTH, 5_000, 12, 200),
			gains(GainPeriod.YEAR, 20_000, 50, 600),
			gains(GainPeriod.ALL_TIME, 1_000_000, 100, 1_000));
		ClanProgressPlayer bob = player("Bob",
			gains(GainPeriod.DAY, 200, 1, 80),
			gains(GainPeriod.WEEK, 700, 9, 90),
			gains(GainPeriod.MONTH, 6_000, 11, 250),
			gains(GainPeriod.YEAR, 18_000, 60, 700),
			gains(GainPeriod.ALL_TIME, 2_000_000, 80, 1_500));

		ClanProgressLeaderboard leaderboard = new ClanProgressLeaderboard(
			"Clan progress", 42, Arrays.asList(alice, bob));

		Assert.assertEquals("Bob", leaderboard.getLeader(GainPeriod.DAY, ClanProgressMetric.XP).getName());
		Assert.assertEquals("Alice", leaderboard.getLeader(GainPeriod.DAY, ClanProgressMetric.COLLECTIONS).getName());
		Assert.assertEquals("Bob", leaderboard.getLeader(GainPeriod.YEAR, ClanProgressMetric.BOSS_KC).getName());
		Assert.assertTrue(leaderboard.getBossNames().contains("Vorkath"));
		List<String> lines = leaderboard.toChatLines();
		Assert.assertEquals(5, lines.size());
		Assert.assertEquals("[RV] 24h | XP: Bob +200 | CLogs: Alice +3 | Boss KC: Bob +80", lines.get(0));
		Assert.assertEquals("[RV] All-time | XP: Bob 2.0m | CLogs: Alice 100 | Boss KC: Bob 1,500", lines.get(4));
	}

	private static ClanProgressPlayer player(String name, PeriodGains... periodGains)
	{
		Map<GainPeriod, ClanProgressGains> gains = new EnumMap<>(GainPeriod.class);
		for (PeriodGains item : periodGains)
		{
			gains.put(item.period, item.gains);
		}
		return new ClanProgressPlayer(name, gains);
	}

	private static PeriodGains gains(GainPeriod period, long xp, long collections, long bossKc)
	{
		return new PeriodGains(period, new ClanProgressGains(xp, collections, bossKc));
	}

	private static class PeriodGains
	{
		private final GainPeriod period;
		private final ClanProgressGains gains;

		private PeriodGains(GainPeriod period, ClanProgressGains gains)
		{
			this.period = period;
			this.gains = gains;
		}
	}
}
