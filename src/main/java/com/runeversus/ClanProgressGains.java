package com.runeversus;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class ClanProgressGains
{
	private final long xp;
	private final long collections;
	private final long bossKc;
	private final Map<String, Long> bossKcByBoss;

	public ClanProgressGains(long xp, long collections, long bossKc)
	{
		this(xp, collections, bossKc, Collections.emptyMap());
	}

	public ClanProgressGains(long xp, long collections, Map<String, Long> bossKcByBoss)
	{
		this(xp, collections, totalBossKc(bossKcByBoss), bossKcByBoss);
	}

	private ClanProgressGains(
		long xp,
		long collections,
		long bossKc,
		Map<String, Long> bossKcByBoss)
	{
		this.xp = Math.max(0L, xp);
		this.collections = Math.max(0L, collections);
		this.bossKc = Math.max(0L, bossKc);
		TreeMap<String, Long> copy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		if (bossKcByBoss != null)
		{
			for (Map.Entry<String, Long> entry : bossKcByBoss.entrySet())
			{
				if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0L)
				{
					copy.put(entry.getKey(), entry.getValue());
				}
			}
		}
		this.bossKcByBoss = Collections.unmodifiableMap(copy);
	}

	public long getXp()
	{
		return xp;
	}

	public long getCollections()
	{
		return collections;
	}

	public long getBossKc()
	{
		return bossKc;
	}

	public long getBossKc(String bossName)
	{
		if (bossName == null || bossName.trim().isEmpty())
		{
			return bossKc;
		}
		return bossKcByBoss.getOrDefault(bossName, 0L);
	}

	public Map<String, Long> getBossKcByBoss()
	{
		return bossKcByBoss;
	}

	public long valueFor(ClanProgressMetric metric)
	{
		switch (metric)
		{
			case XP:
				return xp;
			case COLLECTIONS:
				return collections;
			case BOSS_KC:
				return bossKc;
			default:
				return 0L;
		}
	}

	private static long totalBossKc(Map<String, Long> bossKcByBoss)
	{
		long total = 0L;
		if (bossKcByBoss != null)
		{
			for (Long value : bossKcByBoss.values())
			{
				if (value != null && value > 0L)
				{
					total += value;
				}
			}
		}
		return total;
	}
}
