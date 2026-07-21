package com.runeversus;

import java.util.Locale;

public enum CombatAchievementTier
{
	UNKNOWN(-1, "n/a"),
	UNRANKED(0, "Unranked"),
	EASY(1, "Easy"),
	MEDIUM(2, "Medium"),
	HARD(3, "Hard"),
	ELITE(4, "Elite"),
	MASTER(5, "Master"),
	GRANDMASTER(6, "Grandmaster");

	private final int score;
	private final String displayName;

	CombatAchievementTier(int score, String displayName)
	{
		this.score = score;
		this.displayName = displayName;
	}

	public int getScore()
	{
		return score;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public boolean isKnown()
	{
		return this != UNKNOWN;
	}

	public static CombatAchievementTier parse(String value)
	{
		if (value == null || value.trim().isEmpty())
		{
			return UNKNOWN;
		}

		String normalized = value.trim()
			.toUpperCase(Locale.ROOT)
			.replace('-', '_')
			.replace(' ', '_');
		if ("NONE".equals(normalized) || "NO_TIER".equals(normalized))
		{
			return UNRANKED;
		}
		if ("GRAND_MASTER".equals(normalized))
		{
			return GRANDMASTER;
		}

		try
		{
			return valueOf(normalized);
		}
		catch (IllegalArgumentException ex)
		{
			return UNKNOWN;
		}
	}

	public static CombatAchievementTier fromScore(long score)
	{
		for (CombatAchievementTier tier : values())
		{
			if (tier.score == score)
			{
				return tier;
			}
		}
		return UNKNOWN;
	}
}
