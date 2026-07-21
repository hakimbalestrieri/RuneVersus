package com.runeversus.model;

public enum MetricType
{
	SKILL("Skills"),
	BOSS("Boss KC"),
	ACTIVITY("Activities"),
	FORM_DAY("24h Form"),
	FORM_WEEK("Week Form"),
	FORM_MONTH("Month Form"),
	PERSONAL_BEST("Boss PB"),
	COMBAT_ACHIEVEMENTS("Combat Achievements"),
	COLLECTION_LOG("Collection Log");

	private final String label;

	MetricType(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}
}
