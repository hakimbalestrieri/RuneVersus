package com.runeversus;

public class RosterStanding
{
	private final String name;
	private final long totalXp;
	private final long bossKc;
	private final long collections;
	private final long dayXp;
	private final long weekXp;
	private final long monthXp;

	public RosterStanding(String name, long totalXp, long bossKc, long collections, long dayXp, long weekXp, long monthXp)
	{
		this.name = name;
		this.totalXp = totalXp;
		this.bossKc = bossKc;
		this.collections = collections;
		this.dayXp = dayXp;
		this.weekXp = weekXp;
		this.monthXp = monthXp;
	}

	public String getName()
	{
		return name;
	}

	public long getTotalXp()
	{
		return totalXp;
	}

	public long getBossKc()
	{
		return bossKc;
	}

	public long getCollections()
	{
		return collections;
	}

	public long getDayXp()
	{
		return dayXp;
	}

	public long getWeekXp()
	{
		return weekXp;
	}

	public long getMonthXp()
	{
		return monthXp;
	}

	long valueFor(String metric)
	{
		switch (metric)
		{
			case "Boss KC":
				return bossKc;
			case "Total XP":
				return totalXp;
			case "Collections":
				return collections;
			case "24h XP":
				return dayXp;
			case "Week XP":
				return weekXp;
			case "Month XP":
				return monthXp;
			default:
				return Math.max(Math.max(bossKc, totalXp), collections);
		}
	}

	String bestDisplayFor(String title)
	{
		if (title.contains("Boss"))
		{
			return RuneVersusFlavor.format(bossKc);
		}
		if (title.contains("Form"))
		{
			long value = weekXp > 0 ? weekXp : dayXp;
			return value > 0 ? "+" + RuneVersusFlavor.format(value) : "locked";
		}
		return RuneVersusFlavor.format(totalXp);
	}
}
