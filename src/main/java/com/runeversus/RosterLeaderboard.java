package com.runeversus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RosterLeaderboard
{
	private final String label;
	private final List<RosterStanding> standings;
	private final Instant createdAt;

	public RosterLeaderboard(String label, List<RosterStanding> standings)
	{
		this.label = label;
		this.standings = Collections.unmodifiableList(new ArrayList<>(standings));
		this.createdAt = Instant.now();
	}

	public String getLabel()
	{
		return label;
	}

	public List<RosterStanding> getStandings()
	{
		return standings;
	}

	public Instant getCreatedAt()
	{
		return createdAt;
	}

	public RosterStanding getTopBossKc()
	{
		return top(Comparator.comparingLong(RosterStanding::getBossKc));
	}

	public RosterStanding getTopTotalXp()
	{
		return top(Comparator.comparingLong(RosterStanding::getTotalXp));
	}

	public RosterStanding getTopCollections()
	{
		return top(Comparator.comparingLong(RosterStanding::getCollections));
	}

	public RosterStanding getTopWeekXp()
	{
		return top(Comparator.comparingLong(RosterStanding::getWeekXp));
	}

	public List<RosterStanding> topByBossKc(int limit)
	{
		return topList(Comparator.comparingLong(RosterStanding::getBossKc), limit);
	}

	public List<RosterStanding> topByTotalXp(int limit)
	{
		return topList(Comparator.comparingLong(RosterStanding::getTotalXp), limit);
	}

	public List<RosterStanding> topByWeekXp(int limit)
	{
		Comparator<RosterStanding> comparator = Comparator
			.comparingLong((RosterStanding s) -> Math.max(s.getWeekXp(), s.getDayXp()))
			.thenComparingLong(RosterStanding::getTotalXp);
		return topList(comparator, limit);
	}

	public String getHeadline()
	{
		RosterStanding pvm = getTopBossKc();
		RosterStanding skiller = getTopTotalXp();
		RosterStanding form = getTopWeekXp();
		if (pvm == null)
		{
			return "No ranked players found.";
		}
		if (form != null && Math.max(form.getWeekXp(), form.getDayXp()) > 0)
		{
			return form.getName() + " owns current form, " + pvm.getName() + " owns PvM.";
		}
		return pvm.getName() + " owns PvM, " + skiller.getName() + " owns skilling.";
	}

	public String toCompactText()
	{
		RosterStanding pvm = getTopBossKc();
		RosterStanding skiller = getTopTotalXp();
		RosterStanding clog = getTopCollections();
		RosterStanding form = getTopWeekXp();
		if (pvm == null)
		{
			return "[RuneVersus] " + label + ": no ranked players found.";
		}

		StringBuilder sb = new StringBuilder("[RuneVersus] ")
			.append(label)
			.append(" | PvM King: ")
			.append(pvm.getName())
			.append(" ")
			.append(RuneVersusFlavor.format(pvm.getBossKc()))
			.append(" KC | Skilling King: ")
			.append(skiller.getName())
			.append(" ")
			.append(RuneVersusFlavor.format(skiller.getTotalXp()))
			.append(" XP");
		if (clog != null && clog.getCollections() > 0)
		{
			sb.append(" | Collection Lord: ")
				.append(clog.getName())
				.append(" ")
				.append(RuneVersusFlavor.format(clog.getCollections()));
		}
		if (form != null && Math.max(form.getWeekXp(), form.getDayXp()) > 0)
		{
			sb.append(" | Current Form: ")
				.append(form.getName())
				.append(" +")
				.append(RuneVersusFlavor.format(Math.max(form.getWeekXp(), form.getDayXp())));
		}
		return sb.toString();
	}

	private RosterStanding top(Comparator<RosterStanding> comparator)
	{
		return standings.stream().max(comparator).orElse(null);
	}

	private List<RosterStanding> topList(Comparator<RosterStanding> comparator, int limit)
	{
		List<RosterStanding> out = new ArrayList<>(standings);
		out.sort(comparator.reversed().thenComparing(RosterStanding::getName, String.CASE_INSENSITIVE_ORDER));
		if (out.size() <= limit)
		{
			return out;
		}
		return new ArrayList<>(out.subList(0, limit));
	}
}
