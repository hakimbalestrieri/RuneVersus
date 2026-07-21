package com.runeversus;

public enum GainPeriod
{
	DAY("day", "24h"),
	WEEK("week", "Week"),
	MONTH("month", "Month"),
	YEAR("year", "Year"),
	ALL_TIME("all_time", "All-time");

	private final String apiValue;
	private final String label;

	GainPeriod(String apiValue, String label)
	{
		this.apiValue = apiValue;
		this.label = label;
	}

	public String getApiValue()
	{
		return apiValue;
	}

	public String getLabel()
	{
		return label;
	}

	public boolean isAllTime()
	{
		return this == ALL_TIME;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
