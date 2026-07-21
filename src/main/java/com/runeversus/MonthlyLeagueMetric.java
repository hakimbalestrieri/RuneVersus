package com.runeversus;

public enum MonthlyLeagueMetric
{
	OVERALL("Overall"),
	SKILLING("Skilling"),
	PVM("PvM"),
	COLLECTION("Collection");

	private final String label;

	MonthlyLeagueMetric(String label)
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
