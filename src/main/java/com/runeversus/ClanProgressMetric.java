package com.runeversus;

public enum ClanProgressMetric
{
	XP("XP"),
	COLLECTIONS("CLogs"),
	BOSS_KC("Boss KC");

	private final String label;

	ClanProgressMetric(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
