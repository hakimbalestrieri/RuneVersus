package com.runeversus;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToDoubleFunction;

public class MonthlyLeagueSeason
{
	private static final Duration ENTRY_GRACE = Duration.ofHours(72);
	private static final Duration FRESHNESS_WINDOW = Duration.ofHours(48);
	private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

	private final int groupId;
	private final YearMonth month;
	private final Instant startsAt;
	private final Instant endsAt;
	private final Instant generatedAt;
	private final boolean finalized;
	private final List<MonthlyLeagueStanding> standings;

	public MonthlyLeagueSeason(
		int groupId,
		YearMonth month,
		Instant generatedAt,
		List<MonthlyLeagueParticipant> participants)
	{
		this(groupId, month, generatedAt, participants, false);
	}

	public MonthlyLeagueSeason(
		int groupId,
		YearMonth month,
		Instant generatedAt,
		List<MonthlyLeagueParticipant> participants,
		boolean finalized)
	{
		this.groupId = groupId;
		this.month = month;
		this.startsAt = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		this.endsAt = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		this.generatedAt = generatedAt;
		this.finalized = finalized;
		this.standings = Collections.unmodifiableList(score(
			participants == null ? Collections.emptyList() : participants));
	}

	public int getGroupId()
	{
		return groupId;
	}

	public YearMonth getMonth()
	{
		return month;
	}

	public Instant getStartsAt()
	{
		return startsAt;
	}

	public Instant getEndsAt()
	{
		return endsAt;
	}

	public Instant getGeneratedAt()
	{
		return generatedAt;
	}

	public String getLabel()
	{
		String label = month.format(LABEL_FORMAT);
		return label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1);
	}

	public boolean isLive()
	{
		return !finalized && generatedAt != null
			&& !generatedAt.isBefore(startsAt) && generatedAt.isBefore(endsAt);
	}

	public boolean isFinalized()
	{
		return finalized;
	}

	public Duration getTimeRemaining()
	{
		if (!isLive())
		{
			return Duration.ZERO;
		}
		return Duration.between(generatedAt, endsAt);
	}

	public List<MonthlyLeagueStanding> getStandings()
	{
		return standings;
	}

	public List<MonthlyLeagueStanding> rankedBy(MonthlyLeagueMetric metric)
	{
		List<MonthlyLeagueStanding> sorted = new ArrayList<>(standings);
		sorted.sort(comparator(metric));
		return sorted;
	}

	public MonthlyLeagueStanding getChampion(MonthlyLeagueMetric metric)
	{
		for (MonthlyLeagueStanding standing : rankedBy(metric))
		{
			if (standing.isEligible() && standing.rankFor(metric) == 1)
			{
				return standing;
			}
		}
		return null;
	}

	public int getEligibleCount()
	{
		return (int) standings.stream().filter(MonthlyLeagueStanding::isEligible).count();
	}

	public int getProvisionalCount()
	{
		return standings.size() - getEligibleCount();
	}

	public int getFreshCount()
	{
		return (int) standings.stream().filter(MonthlyLeagueStanding::isFresh).count();
	}

	private List<MonthlyLeagueStanding> score(List<MonthlyLeagueParticipant> participants)
	{
		List<MonthlyLeagueParticipant> valid = new ArrayList<>();
		for (MonthlyLeagueParticipant participant : participants)
		{
			if (participant != null && !participant.getName().isEmpty())
			{
				valid.add(participant);
			}
		}

		Map<String, Boolean> eligibility = new HashMap<>();
		Map<String, Boolean> freshness = new HashMap<>();
		for (MonthlyLeagueParticipant participant : valid)
		{
			String key = key(participant);
			Instant trackedFrom = participant.getTrackedFrom();
			Instant joinedAt = participant.getJoinedAt();
			Instant target = isLive() ? generatedAt : endsAt;
			Instant trackedUntil = participant.getTrackedUntil();
			boolean isFresh = trackedUntil != null && target != null
				&& !trackedUntil.isBefore(target.minus(FRESHNESS_WINDOW));
			freshness.put(key, isFresh);
			boolean wasReadyAtStart = participant.isRosterEligible()
				&& joinedAt != null
				&& trackedFrom != null
				&& !joinedAt.isAfter(startsAt.plus(ENTRY_GRACE))
				&& !trackedFrom.isAfter(startsAt.plus(ENTRY_GRACE));
			eligibility.put(key, wasReadyAtStart && (!finalized || isFresh));
		}

		int eligibleCount = (int) eligibility.values().stream().filter(Boolean::booleanValue).count();
		Map<String, RankedValue> skill = category(valid, eligibility, eligibleCount,
			MonthlyLeagueParticipant::getEhpGained);
		Map<String, RankedValue> pvm = category(valid, eligibility, eligibleCount,
			MonthlyLeagueParticipant::getEhbGained);
		Map<String, RankedValue> collection = category(valid, eligibility, eligibleCount,
			participant -> participant.getCollectionsGained());
		boolean hasSkilling = skill.values().stream().anyMatch(value -> value.score > 0.0);
		boolean hasPvm = pvm.values().stream().anyMatch(value -> value.score > 0.0);

		Map<String, Double> overallScores = new HashMap<>();
		for (MonthlyLeagueParticipant participant : valid)
		{
			String key = key(participant);
			double total = 0.0;
			int categories = 0;
			if (hasSkilling)
			{
				total += skill.get(key).score;
				categories++;
			}
			if (hasPvm)
			{
				total += pvm.get(key).score;
				categories++;
			}
			overallScores.put(key, categories == 0 || !eligibility.get(key) ? 0.0 : total / categories);
		}
		Map<String, Integer> overallRanks = ranks(valid, eligibility,
			participant -> overallScores.get(key(participant)));

		List<MonthlyLeagueStanding> result = new ArrayList<>();
		for (MonthlyLeagueParticipant participant : valid)
		{
			String key = key(participant);
			result.add(new MonthlyLeagueStanding(
				participant,
				eligibility.get(key),
				freshness.get(key),
				skill.get(key).score,
				pvm.get(key).score,
				round(overallScores.get(key)),
				overallRanks.getOrDefault(key, 0),
				skill.get(key).rank,
				pvm.get(key).rank,
				collection.get(key).rank));
		}
		result.sort(comparator(MonthlyLeagueMetric.OVERALL));
		return result;
	}

	private static Map<String, RankedValue> category(
		List<MonthlyLeagueParticipant> participants,
		Map<String, Boolean> eligibility,
		int eligibleCount,
		ToDoubleFunction<MonthlyLeagueParticipant> valueFunction)
	{
		Map<String, Integer> ranks = ranks(participants, eligibility, valueFunction);
		Map<String, RankedValue> values = new HashMap<>();
		for (MonthlyLeagueParticipant participant : participants)
		{
			String key = key(participant);
			int rank = ranks.getOrDefault(key, 0);
			double raw = valueFunction.applyAsDouble(participant);
			double score = !eligibility.get(key) || raw <= 0.0 || eligibleCount == 0
				? 0.0 : 100.0 * (eligibleCount - rank + 1.0) / eligibleCount;
			values.put(key, new RankedValue(rank, round(score)));
		}
		return values;
	}

	private static Map<String, Integer> ranks(
		List<MonthlyLeagueParticipant> participants,
		Map<String, Boolean> eligibility,
		ToDoubleFunction<MonthlyLeagueParticipant> valueFunction)
	{
		List<MonthlyLeagueParticipant> ranked = new ArrayList<>();
		for (MonthlyLeagueParticipant participant : participants)
		{
			if (eligibility.get(key(participant)) && valueFunction.applyAsDouble(participant) > 0.0)
			{
				ranked.add(participant);
			}
		}
		ranked.sort(Comparator.comparingDouble(valueFunction).reversed()
			.thenComparing(MonthlyLeagueParticipant::getName, String.CASE_INSENSITIVE_ORDER));
		Map<String, Integer> out = new HashMap<>();
		int position = 0;
		double previous = Double.NaN;
		int currentRank = 0;
		for (MonthlyLeagueParticipant participant : ranked)
		{
			position++;
			double value = valueFunction.applyAsDouble(participant);
			if (position == 1 || Double.compare(value, previous) != 0)
			{
				currentRank = position;
				previous = value;
			}
			out.put(key(participant), currentRank);
		}
		return out;
	}

	private static Comparator<MonthlyLeagueStanding> comparator(MonthlyLeagueMetric metric)
	{
		return Comparator
			.comparing((MonthlyLeagueStanding standing) -> !standing.isEligible())
			.thenComparingInt(standing -> standing.rankFor(metric) == 0 ? Integer.MAX_VALUE : standing.rankFor(metric))
			.thenComparing(MonthlyLeagueStanding::getName, String.CASE_INSENSITIVE_ORDER);
	}

	private static String key(MonthlyLeagueParticipant participant)
	{
		return participant.getName().toLowerCase(Locale.ROOT);
	}

	private static double round(double value)
	{
		return Math.round(value * 10.0) / 10.0;
	}

	private static final class RankedValue
	{
		private final int rank;
		private final double score;

		private RankedValue(int rank, double score)
		{
			this.rank = rank;
			this.score = score;
		}
	}
}
