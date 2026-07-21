package com.runeversus;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class MonthlyLeagueSeasonTest
{
	private static final Instant JULY_START = Instant.parse("2026-07-01T00:00:00Z");
	private static final Instant NOW = Instant.parse("2026-07-21T12:00:00Z");

	@Test
	public void scoresOnlyEhpAndEhbInOverallLeague()
	{
		MonthlyLeagueSeason season = season(
			participant("Balanced", 20, 20, 0, 0),
			participant("Skiller", 40, 0, 200, 0),
			participant("Pvmer", 0, 40, 1, 0));

		MonthlyLeagueStanding balanced = standing(season, "Balanced");
		MonthlyLeagueStanding skiller = standing(season, "Skiller");
		Assert.assertEquals(66.7, balanced.getOverallScore(), 0.01);
		Assert.assertEquals(50.0, skiller.getOverallScore(), 0.01);
		Assert.assertEquals(1, balanced.getOverallRank());
		Assert.assertEquals(2, skiller.getOverallRank());
		Assert.assertEquals("Skiller", season.getChampion(MonthlyLeagueMetric.COLLECTION).getName());
	}

	@Test
	public void excludesLateJoinersFromCompetitiveRanks()
	{
		MonthlyLeagueSeason season = season(
			participant("Founder", 10, 10, 0, 0),
			participant("Late joiner", 100, 100, 50, 10));

		MonthlyLeagueStanding late = standing(season, "Late joiner");
		Assert.assertFalse(late.isEligible());
		Assert.assertEquals(0, late.getOverallRank());
		Assert.assertEquals(0.0, late.getOverallScore(), 0.01);
		Assert.assertEquals("Founder", season.getChampion(MonthlyLeagueMetric.OVERALL).getName());
		Assert.assertEquals(1, season.getProvisionalCount());
	}

	@Test
	public void identifiesLiveSeasonAndRemainingTime()
	{
		MonthlyLeagueSeason season = season(participant("Alice", 1, 1, 1, 0));
		Assert.assertTrue(season.isLive());
		Assert.assertEquals(10, season.getTimeRemaining().toDays());
		Assert.assertEquals("July 2026", season.getLabel());
	}

	private static MonthlyLeagueSeason season(MonthlyLeagueParticipant... participants)
	{
		return new MonthlyLeagueSeason(42, YearMonth.of(2026, 7), NOW, Arrays.asList(participants));
	}

	private static MonthlyLeagueParticipant participant(
		String name, double ehp, double ehb, long clogs, long startDayOffset)
	{
		return new MonthlyLeagueParticipant(
			name,
			"regular",
			ehp,
			ehb,
			clogs,
			JULY_START.plusSeconds(startDayOffset * 86_400L),
			NOW.minusSeconds(3_600L));
	}

	private static MonthlyLeagueStanding standing(MonthlyLeagueSeason season, String name)
	{
		return season.getStandings().stream()
			.filter(value -> name.equals(value.getName()))
			.findFirst()
			.orElseThrow(AssertionError::new);
	}
}
