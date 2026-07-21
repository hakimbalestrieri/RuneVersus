package com.runeversus;

import java.time.Instant;

public class MonthlyLeagueParticipant
{
	private final String name;
	private final String accountType;
	private final double ehpGained;
	private final double ehbGained;
	private final long collectionsGained;
	private final Instant trackedFrom;
	private final Instant trackedUntil;

	public MonthlyLeagueParticipant(
		String name,
		String accountType,
		double ehpGained,
		double ehbGained,
		long collectionsGained,
		Instant trackedFrom,
		Instant trackedUntil)
	{
		this.name = name == null ? "" : name.trim();
		this.accountType = accountType == null ? "unknown" : accountType.trim();
		this.ehpGained = finitePositive(ehpGained);
		this.ehbGained = finitePositive(ehbGained);
		this.collectionsGained = Math.max(0L, collectionsGained);
		this.trackedFrom = trackedFrom;
		this.trackedUntil = trackedUntil;
	}

	public String getName()
	{
		return name;
	}

	public String getAccountType()
	{
		return accountType;
	}

	public double getEhpGained()
	{
		return ehpGained;
	}

	public double getEhbGained()
	{
		return ehbGained;
	}

	public long getCollectionsGained()
	{
		return collectionsGained;
	}

	public Instant getTrackedFrom()
	{
		return trackedFrom;
	}

	public Instant getTrackedUntil()
	{
		return trackedUntil;
	}

	public boolean isActive()
	{
		return ehpGained > 0.0 || ehbGained > 0.0 || collectionsGained > 0L;
	}

	private static double finitePositive(double value)
	{
		return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
	}
}
