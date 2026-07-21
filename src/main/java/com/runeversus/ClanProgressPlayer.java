package com.runeversus;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class ClanProgressPlayer
{
	private final String name;
	private final Map<GainPeriod, ClanProgressGains> gains;

	public ClanProgressPlayer(String name, Map<GainPeriod, ClanProgressGains> gains)
	{
		this.name = name;
		EnumMap<GainPeriod, ClanProgressGains> copy = new EnumMap<>(GainPeriod.class);
		if (gains != null)
		{
			copy.putAll(gains);
		}
		this.gains = Collections.unmodifiableMap(copy);
	}

	public String getName()
	{
		return name;
	}

	public ClanProgressGains getGains(GainPeriod period)
	{
		return gains.getOrDefault(period, new ClanProgressGains(0L, 0L, 0L));
	}
}
