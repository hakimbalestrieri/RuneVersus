package com.runeversus;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class MonthlyLeagueRosterTest
{
	private static final Instant START = Instant.parse("2026-07-01T00:00:00Z");

	@Test
	public void firstCaptureMarksOnlyVerifiedEarlyMembersAsEligible()
	{
		List<MonthlyLeagueParticipant> gains = Arrays.asList(
			gain(1L, "Early", START.plusSeconds(3_600L)),
			gain(2L, "Unknown", START.plusSeconds(3_600L)));
		List<MonthlyLeagueMembership> memberships = Collections.singletonList(
			new MonthlyLeagueMembership(1L, "Early", "regular", START.minusSeconds(86_400L)));

		List<MonthlyLeagueParticipant> merged = RuneVersusService.mergeMonthlyParticipants(
			START, null, gains, memberships);

		Assert.assertTrue(find(merged, "Early").isRosterEligible());
		Assert.assertFalse(find(merged, "Unknown").isRosterEligible());
	}

	@Test
	public void capturesEligibleMembersWhoJoinDuringGraceBeforeRosterFreeze()
	{
		MonthlyLeagueParticipant early = new MonthlyLeagueParticipant(
			1L, "Early", "regular", 1, 1, 0,
			START, START.plusSeconds(3_600L), START.minusSeconds(3_600L), true);
		MonthlyLeagueArchiveStore.Archive previous = new MonthlyLeagueArchiveStore.Archive(
			null, null, Collections.singletonList(early));
		List<MonthlyLeagueParticipant> gains = Arrays.asList(
			gain(1L, "Early", START),
			gain(2L, "Grace", START.plusSeconds(2 * 86_400L)));
		List<MonthlyLeagueMembership> memberships = Arrays.asList(
			new MonthlyLeagueMembership(1L, "Early", "regular", START.minusSeconds(3_600L)),
			new MonthlyLeagueMembership(2L, "Grace", "regular", START.plusSeconds(2 * 86_400L)));

		List<MonthlyLeagueParticipant> merged = RuneVersusService.mergeMonthlyParticipants(
			START, previous, gains, memberships);

		Assert.assertTrue(find(merged, "Early").isRosterEligible());
		Assert.assertTrue(find(merged, "Grace").isRosterEligible());
	}

	@Test
	public void laterCapturesCannotAddCompetitiveMembersOrDropFrozenOnes()
	{
		MonthlyLeagueParticipant frozen = new MonthlyLeagueParticipant(
			1L, "Frozen", "regular", 5, 4, 1,
			START, START.plusSeconds(86_400L), START.minusSeconds(86_400L), true);
		MonthlyLeagueArchiveStore.Archive previous = new MonthlyLeagueArchiveStore.Archive(
			START.plusSeconds(3_600L), null, Collections.singletonList(frozen));
		List<MonthlyLeagueParticipant> currentGains = Collections.singletonList(
			gain(2L, "Newcomer", START.plusSeconds(2 * 86_400L)));
		List<MonthlyLeagueMembership> memberships = Collections.singletonList(
			new MonthlyLeagueMembership(2L, "Newcomer", "regular", START.plusSeconds(2 * 86_400L)));

		List<MonthlyLeagueParticipant> merged = RuneVersusService.mergeMonthlyParticipants(
			START, previous, currentGains, memberships);

		Assert.assertTrue(find(merged, "Frozen").isRosterEligible());
		Assert.assertFalse(find(merged, "Newcomer").isRosterEligible());
	}

	@Test
	public void preservesEarlyMemberCandidateUntilTheirFirstSnapshotAppears()
	{
		MonthlyLeagueMembership membership = new MonthlyLeagueMembership(
			3L, "Pending", "regular", START.minusSeconds(3_600L));
		List<MonthlyLeagueParticipant> first = RuneVersusService.mergeMonthlyParticipants(
			START, null, Collections.emptyList(), Collections.singletonList(membership));
		MonthlyLeagueParticipant pending = find(first, "Pending");
		Assert.assertTrue(pending.isRosterEligible());

		MonthlyLeagueArchiveStore.Archive previous = new MonthlyLeagueArchiveStore.Archive(
			null, null, first);
		List<MonthlyLeagueParticipant> second = RuneVersusService.mergeMonthlyParticipants(
			START, previous,
			Collections.singletonList(gain(3L, "Pending", START.plusSeconds(2 * 3_600L))),
			Collections.singletonList(membership));

		Assert.assertTrue(find(second, "Pending").isRosterEligible());
		MonthlyLeagueSeason season = new MonthlyLeagueSeason(
			42, java.time.YearMonth.of(2026, 7), START.plusSeconds(86_400L), second);
		Assert.assertEquals(1, season.getEligibleCount());
	}

	private static MonthlyLeagueParticipant gain(long id, String name, Instant trackedFrom)
	{
		return new MonthlyLeagueParticipant(
			id, name, "regular", 1, 1, 1, trackedFrom, trackedFrom.plusSeconds(3_600L));
	}

	private static MonthlyLeagueParticipant find(List<MonthlyLeagueParticipant> participants, String name)
	{
		return participants.stream()
			.filter(participant -> name.equals(participant.getName()))
			.findFirst()
			.orElseThrow(AssertionError::new);
	}
}
