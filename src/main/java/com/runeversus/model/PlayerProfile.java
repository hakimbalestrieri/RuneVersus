package com.runeversus.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;

public class PlayerProfile
{
	private final String name;
	private final HiscoreResult hiscores;
	private final Map<HiscoreSkill, Long> dayGains;
	private final Map<HiscoreSkill, Long> weekGains;
	private final Map<HiscoreSkill, Long> monthGains;
	private final boolean gainsAvailable;

	public PlayerProfile(String name, HiscoreResult hiscores)
	{
		this(name, hiscores, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);
	}

	public PlayerProfile(
		String name,
		HiscoreResult hiscores,
		Map<HiscoreSkill, Long> dayGains,
		Map<HiscoreSkill, Long> weekGains,
		Map<HiscoreSkill, Long> monthGains,
		boolean gainsAvailable)
	{
		this.name = name;
		this.hiscores = hiscores;
		this.dayGains = copy(dayGains);
		this.weekGains = copy(weekGains);
		this.monthGains = copy(monthGains);
		this.gainsAvailable = gainsAvailable;
	}

	private static Map<HiscoreSkill, Long> copy(Map<HiscoreSkill, Long> source)
	{
		if (source == null || source.isEmpty())
		{
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(new EnumMap<>(source));
	}

	public String getName()
	{
		return name;
	}

	public HiscoreResult getHiscores()
	{
		return hiscores;
	}

	public Map<HiscoreSkill, Long> getDayGains()
	{
		return dayGains;
	}

	public Map<HiscoreSkill, Long> getWeekGains()
	{
		return weekGains;
	}

	public Map<HiscoreSkill, Long> getMonthGains()
	{
		return monthGains;
	}

	public boolean isGainsAvailable()
	{
		return gainsAvailable;
	}
}
