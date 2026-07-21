package com.runeversus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ClanProgressLeaderboard
{
	private final String label;
	private final int groupId;
	private final ProgressGroupType groupType;
	private final String sourceDescription;
	private final List<ClanProgressPlayer> players;
	private final Instant createdAt;

	public ClanProgressLeaderboard(String label, int groupId, List<ClanProgressPlayer> players)
	{
		this(label, groupId, ProgressGroupType.CLAN, "WOM group #" + groupId, players);
	}

	public ClanProgressLeaderboard(
		String label,
		ProgressGroupType groupType,
		String sourceDescription,
		List<ClanProgressPlayer> players)
	{
		this(label, 0, groupType, sourceDescription, players);
	}

	private ClanProgressLeaderboard(
		String label,
		int groupId,
		ProgressGroupType groupType,
		String sourceDescription,
		List<ClanProgressPlayer> players)
	{
		this.label = label;
		this.groupId = groupId;
		this.groupType = groupType == null ? ProgressGroupType.CLAN : groupType;
		this.sourceDescription = sourceDescription == null ? "Wise Old Man" : sourceDescription;
		this.players = Collections.unmodifiableList(new ArrayList<>(players));
		this.createdAt = Instant.now();
	}

	public String getLabel()
	{
		return label;
	}

	public int getGroupId()
	{
		return groupId;
	}

	public ProgressGroupType getGroupType()
	{
		return groupType;
	}

	public String getSourceDescription()
	{
		return sourceDescription;
	}

	public List<ClanProgressPlayer> getPlayers()
	{
		return players;
	}

	public Instant getCreatedAt()
	{
		return createdAt;
	}

	public ClanProgressPlayer getLeader(GainPeriod period, ClanProgressMetric metric)
	{
		Comparator<ClanProgressPlayer> comparator = Comparator
			.comparingLong((ClanProgressPlayer player) -> player.getGains(period).valueFor(metric))
			.reversed()
			.thenComparing(ClanProgressPlayer::getName, String.CASE_INSENSITIVE_ORDER);
		return players.stream()
			.filter(player -> player.getGains(period).valueFor(metric) > 0L)
			.min(comparator)
			.orElse(null);
	}

	public long getLeaderValue(GainPeriod period, ClanProgressMetric metric)
	{
		ClanProgressPlayer leader = getLeader(period, metric);
		return leader == null ? 0L : leader.getGains(period).valueFor(metric);
	}

	public ClanProgressPlayer getBossLeader(GainPeriod period, String bossName)
	{
		Comparator<ClanProgressPlayer> comparator = Comparator
			.comparingLong((ClanProgressPlayer player) -> player.getGains(period).getBossKc(bossName))
			.reversed()
			.thenComparing(ClanProgressPlayer::getName, String.CASE_INSENSITIVE_ORDER);
		return players.stream()
			.filter(player -> player.getGains(period).getBossKc(bossName) > 0L)
			.min(comparator)
			.orElse(null);
	}

	public List<String> getBossNames()
	{
		Set<String> names = new LinkedHashSet<>(BossKcRegistry.allNames());
		for (ClanProgressPlayer player : players)
		{
			for (GainPeriod period : GainPeriod.values())
			{
				names.addAll(player.getGains(period).getBossKcByBoss().keySet());
			}
		}
		List<String> sorted = new ArrayList<>(names);
		sorted.sort(String.CASE_INSENSITIVE_ORDER);
		return Collections.unmodifiableList(sorted);
	}

	public List<String> toChatLines()
	{
		List<String> lines = new ArrayList<>();
		for (GainPeriod period : GainPeriod.values())
		{
			lines.add("[RV] " + period.getLabel()
				+ " | XP: " + leaderText(period, ClanProgressMetric.XP)
				+ " | CLogs: " + leaderText(period, ClanProgressMetric.COLLECTIONS)
				+ " | Boss KC: " + leaderText(period, ClanProgressMetric.BOSS_KC));
		}
		return lines;
	}

	public String toDisplayText()
	{
		if (players.isEmpty())
		{
			return "[RuneVersus] No tracked players found for " + sourceDescription + ".";
		}
		StringBuilder out = new StringBuilder("[RuneVersus] ")
			.append(label)
			.append(" • ")
			.append(players.size())
			.append(" tracked player(s)\n");
		for (String line : toChatLines())
		{
			out.append(line.substring(5)).append('\n');
		}
		return out.toString().trim();
	}

	private String leaderText(GainPeriod period, ClanProgressMetric metric)
	{
		ClanProgressPlayer leader = getLeader(period, metric);
		if (leader == null)
		{
			return "no gain";
		}
		long value = leader.getGains(period).valueFor(metric);
		return leader.getName() + " " + valueText(period, value);
	}

	static String valueText(GainPeriod period, long value)
	{
		return (period.isAllTime() ? "" : "+") + RuneVersusFlavor.format(value);
	}
}
