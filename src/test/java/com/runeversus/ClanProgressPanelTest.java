package com.runeversus;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.Assert;
import org.junit.Test;

public class ClanProgressPanelTest
{
	@Test
	public void changesPeriodSortAndSearchWithoutReloading() throws Exception
	{
		ClanProgressLeaderboard leaderboard = new ClanProgressLeaderboard(
			"Clan progress",
			42,
			Arrays.asList(
				player("Alice", gains(100, 10, 2), gains(500, 1, 8)),
				player("Bob", gains(200, 1, 20), gains(300, 5, 12)),
				player("Cora", gains(50, 5, 30), gains(700, 2, 40))));
		AtomicReference<ClanProgressPanel> panelRef = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() ->
		{
			ClanProgressPanel panel = new ClanProgressPanel(() -> { }, () -> { }, ignored -> { }, () -> { });
			panel.setLeaderboard(leaderboard);
			panelRef.set(panel);
		});

		ClanProgressPanel panel = panelRef.get();
		Assert.assertEquals(3, panel.getDisplayedPlayerCount());
		Assert.assertEquals("Bob", panel.getDisplayedPlayerName(0));
		Assert.assertTrue(panel.getTrackedTooltip().contains("public Hiscores"));
		Assert.assertTrue(panel.getActiveTooltip().contains("24h"));
		Assert.assertTrue(panel.getClogTooltip().contains("+3"));
		Assert.assertTrue(panel.getAverageTooltip().contains("divided by Active"));

		SwingUtilities.invokeAndWait(() -> panel.selectSortMetric(ClanProgressMetric.COLLECTIONS));
		Assert.assertEquals("Alice", panel.getDisplayedPlayerName(0));

		SwingUtilities.invokeAndWait(() ->
		{
			panel.selectPeriod(GainPeriod.WEEK);
			panel.selectSortMetric(ClanProgressMetric.BOSS_KC);
		});
		Assert.assertEquals("Cora", panel.getDisplayedPlayerName(0));
		Assert.assertTrue(panel.getBossSelectorCount() > 20);
		Assert.assertEquals(4, panel.getLeaderboardColumnCount());

		SwingUtilities.invokeAndWait(() -> panel.selectBoss("Vorkath"));
		Assert.assertEquals("Cora", panel.getDisplayedPlayerName(0));
		Assert.assertEquals(40L, panel.getDisplayedBossKc(0));
		Assert.assertEquals(2, panel.getLeaderboardColumnCount());
		Assert.assertTrue(panel.getActiveTooltip().contains("Week"));
		Assert.assertTrue(panel.getClogTooltip().contains("+3"));

		SwingUtilities.invokeAndWait(() -> panel.setSearchText("bo"));
		Assert.assertEquals(1, panel.getDisplayedPlayerCount());
		Assert.assertEquals("Bob", panel.getDisplayedPlayerName(0));

		SwingUtilities.invokeAndWait(() -> panel.selectPeriod(GainPeriod.ALL_TIME));
		Assert.assertTrue(panel.getActiveTooltip().contains("Vorkath"));

		SwingUtilities.invokeAndWait(() -> panel.selectBoss("All bosses"));
		Assert.assertEquals(4, panel.getLeaderboardColumnCount());
		Assert.assertTrue(panel.getActiveTooltip().contains("latest Wise Old Man snapshot"));
		Assert.assertTrue(panel.getClogTooltip().contains("current collection-log count"));
		Assert.assertTrue(panel.getAverageTooltip().contains("Total group XP"));

		ClanProgressLeaderboard friendsChat = new ClanProgressLeaderboard(
			"Raid Friends",
			ProgressGroupType.FRIENDS_CHAT,
			"Friend Chat · Raid Friends",
			java.util.Collections.singletonList(player("Friend", gains(25, 1, 2), gains(100, 2, 8))));
		SwingUtilities.invokeAndWait(() ->
		{
			panel.setSearchText("");
			panel.selectPeriod(GainPeriod.DAY);
			panel.setLeaderboard(friendsChat);
		});
		Assert.assertEquals("FRIEND CHAT PERFORMANCE", panel.getPerformanceTitle());
		Assert.assertEquals(1, panel.getDisplayedPlayerCount());
		Assert.assertEquals("Friend", panel.getDisplayedPlayerName(0));
	}

	private static ClanProgressPlayer player(String name, ClanProgressGains day, ClanProgressGains week)
	{
		Map<GainPeriod, ClanProgressGains> gains = new EnumMap<>(GainPeriod.class);
		gains.put(GainPeriod.DAY, day);
		gains.put(GainPeriod.WEEK, week);
		return new ClanProgressPlayer(name, gains);
	}

	private static ClanProgressGains gains(long xp, long collections, long bossKc)
	{
		return new ClanProgressGains(xp, collections, java.util.Collections.singletonMap("Vorkath", bossKc));
	}
}
