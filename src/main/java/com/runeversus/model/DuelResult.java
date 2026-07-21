package com.runeversus.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DuelResult
{
	private final PlayerProfile left;
	private final PlayerProfile right;
	private final List<MetricResult> skills;
	private final List<MetricResult> bosses;
	private final List<MetricResult> activities;
	private final List<MetricResult> dayForm;
	private final List<MetricResult> weekForm;
	private final List<MetricResult> monthForm;
	private final Instant createdAt;

	public DuelResult(
		PlayerProfile left,
		PlayerProfile right,
		List<MetricResult> skills,
		List<MetricResult> bosses,
		List<MetricResult> activities,
		List<MetricResult> dayForm,
		List<MetricResult> weekForm,
		List<MetricResult> monthForm)
	{
		this(left, right, skills, bosses, activities, dayForm, weekForm, monthForm, Instant.now());
	}

	public DuelResult(
		PlayerProfile left,
		PlayerProfile right,
		List<MetricResult> skills,
		List<MetricResult> bosses,
		List<MetricResult> activities,
		List<MetricResult> dayForm,
		List<MetricResult> weekForm,
		List<MetricResult> monthForm,
		Instant createdAt)
	{
		this.left = left;
		this.right = right;
		this.skills = immutable(skills);
		this.bosses = immutable(bosses);
		this.activities = immutable(activities);
		this.dayForm = immutable(dayForm);
		this.weekForm = immutable(weekForm);
		this.monthForm = immutable(monthForm);
		this.createdAt = createdAt == null ? Instant.now() : createdAt;
	}

	private static List<MetricResult> immutable(List<MetricResult> metrics)
	{
		if (metrics == null || metrics.isEmpty())
		{
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(new ArrayList<>(metrics));
	}

	public PlayerProfile getLeft()
	{
		return left;
	}

	public PlayerProfile getRight()
	{
		return right;
	}

	public List<MetricResult> getSkills()
	{
		return skills;
	}

	public List<MetricResult> getBosses()
	{
		return bosses;
	}

	public List<MetricResult> getActivities()
	{
		return activities;
	}

	public List<MetricResult> getDayForm()
	{
		return dayForm;
	}

	public List<MetricResult> getWeekForm()
	{
		return weekForm;
	}

	public List<MetricResult> getMonthForm()
	{
		return monthForm;
	}

	public Instant getCreatedAt()
	{
		return createdAt;
	}

	public int getLeftSkillWins()
	{
		return wins(skills, PlayerSide.LEFT);
	}

	public int getRightSkillWins()
	{
		return wins(skills, PlayerSide.RIGHT);
	}

	public int getLeftBossWins()
	{
		return wins(bosses, PlayerSide.LEFT);
	}

	public int getRightBossWins()
	{
		return wins(bosses, PlayerSide.RIGHT);
	}

	public int getLeftActivityWins()
	{
		return wins(activities, PlayerSide.LEFT);
	}

	public int getRightActivityWins()
	{
		return wins(activities, PlayerSide.RIGHT);
	}

	public int getLeftTotalWins()
	{
		return getLeftSkillWins() + getLeftBossWins() + getLeftActivityWins() + wins(dayForm, PlayerSide.LEFT);
	}

	public int getRightTotalWins()
	{
		return getRightSkillWins() + getRightBossWins() + getRightActivityWins() + wins(dayForm, PlayerSide.RIGHT);
	}

	public long getLeftTotalXp()
	{
		return totalSkillValue(skills, PlayerSide.LEFT);
	}

	public long getRightTotalXp()
	{
		return totalSkillValue(skills, PlayerSide.RIGHT);
	}

	public long getLeftBossKc()
	{
		return sum(bosses, PlayerSide.LEFT);
	}

	public long getRightBossKc()
	{
		return sum(bosses, PlayerSide.RIGHT);
	}

	public long getLeftDayXp()
	{
		return metricValue(dayForm, "Overall", PlayerSide.LEFT);
	}

	public long getRightDayXp()
	{
		return metricValue(dayForm, "Overall", PlayerSide.RIGHT);
	}

	public long getLeftWeekXp()
	{
		return metricValue(weekForm, "Overall", PlayerSide.LEFT);
	}

	public long getRightWeekXp()
	{
		return metricValue(weekForm, "Overall", PlayerSide.RIGHT);
	}

	public long getLeftMonthXp()
	{
		return metricValue(monthForm, "Overall", PlayerSide.LEFT);
	}

	public long getRightMonthXp()
	{
		return metricValue(monthForm, "Overall", PlayerSide.RIGHT);
	}

	public MetricResult getCollectionLogMetric()
	{
		return activities.stream()
			.filter(m -> "Collections Logged".equals(m.getName()))
			.findFirst()
			.orElse(null);
	}

	public MetricResult getCombatAchievementMetric()
	{
		return activities.stream()
			.filter(m -> m.getType() == MetricType.COMBAT_ACHIEVEMENTS)
			.findFirst()
			.orElse(null);
	}

	public MetricResult getClosestSteal()
	{
		return allCompetitiveMetrics().stream()
			.filter(m -> m.getWinner() != PlayerSide.TIE)
			.filter(m -> m.getGap() > 0)
			.min(Comparator.comparingLong(MetricResult::getGap))
			.orElse(null);
	}

	public MetricResult getBiggestFlex()
	{
		return allCompetitiveMetrics().stream()
			.filter(m -> m.getWinner() != PlayerSide.TIE)
			.max(Comparator.comparingLong(MetricResult::getGap))
			.orElse(null);
	}

	public List<MetricResult> getTopBosses(int limit)
	{
		return bosses.stream()
			.filter(m -> m.getLeftValue() > 0 || m.getRightValue() > 0)
			.sorted(Comparator.comparingLong(MetricResult::getGap).reversed())
			.limit(limit)
			.collect(Collectors.toList());
	}

	public List<MetricResult> getTopSkills(int limit)
	{
		return skills.stream()
			.sorted(Comparator.comparingLong(MetricResult::getGap).reversed())
			.limit(limit)
			.collect(Collectors.toList());
	}

	public String getWinnerName()
	{
		if (getLeftTotalWins() == getRightTotalWins())
		{
			return "Tie";
		}
		return getLeftTotalWins() > getRightTotalWins() ? left.getName() : right.getName();
	}

	public String getVerdict()
	{
		if (getLeftTotalWins() == getRightTotalWins())
		{
			return "Dead even. One category can swing the matchup.";
		}

		String winner = getWinnerName();
		if (Math.max(getLeftBossWins(), getRightBossWins()) >= Math.max(getLeftSkillWins(), getRightSkillWins()) + 5)
		{
			return winner + " wins the bossing war.";
		}
		if (Math.max(getLeftSkillWins(), getRightSkillWins()) >= Math.max(getLeftBossWins(), getRightBossWins()) + 5)
		{
			return winner + " wins through skilling depth.";
		}
		return winner + " edges the all-around account clash.";
	}

	private List<MetricResult> allCompetitiveMetrics()
	{
		List<MetricResult> all = new ArrayList<>();
		all.addAll(skills);
		all.addAll(bosses);
		all.addAll(activities);
		return all;
	}

	private static int wins(List<MetricResult> metrics, PlayerSide side)
	{
		int count = 0;
		for (MetricResult metric : metrics)
		{
			if (metric.getWinner() == side)
			{
				count++;
			}
		}
		return count;
	}

	private static long sum(List<MetricResult> metrics, PlayerSide side)
	{
		long total = 0;
		for (MetricResult metric : metrics)
		{
			total = saturatedAdd(total,
				side == PlayerSide.LEFT ? metric.getLeftValue() : metric.getRightValue());
		}
		return total;
	}

	private static long totalSkillValue(List<MetricResult> metrics, PlayerSide side)
	{
		long total = 0L;
		for (MetricResult metric : metrics)
		{
			if (!"Overall".equals(metric.getName()))
			{
				total = saturatedAdd(total,
					side == PlayerSide.LEFT ? metric.getLeftValue() : metric.getRightValue());
			}
		}
		return total;
	}

	private static long saturatedAdd(long left, long right)
	{
		if (right > 0L && left > Long.MAX_VALUE - right)
		{
			return Long.MAX_VALUE;
		}
		if (right < 0L && left < Long.MIN_VALUE - right)
		{
			return Long.MIN_VALUE;
		}
		return left + right;
	}

	private static long metricValue(List<MetricResult> metrics, String name, PlayerSide side)
	{
		return metrics.stream()
			.filter(m -> name.equals(m.getName()))
			.findFirst()
			.map(m -> side == PlayerSide.LEFT ? m.getLeftValue() : m.getRightValue())
			.orElse(0L);
	}
}
