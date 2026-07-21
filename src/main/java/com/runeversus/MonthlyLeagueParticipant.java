package com.runeversus;

import java.time.Instant;

public class MonthlyLeagueParticipant
{
	private final long playerId;
	private final String name;
	private final String accountType;
	private final double ehpGained;
	private final double ehbGained;
	private final long collectionsGained;
	private final Instant trackedFrom;
	private final Instant trackedUntil;
	private final Instant joinedAt;
	private final boolean rosterEligible;

	public MonthlyLeagueParticipant(
		long playerId,
		String name,
		String accountType,
		double ehpGained,
		double ehbGained,
		long collectionsGained,
		Instant trackedFrom,
		Instant trackedUntil)
	{
		this(playerId, name, accountType, ehpGained, ehbGained, collectionsGained,
			trackedFrom, trackedUntil, null, false);
	}

	public MonthlyLeagueParticipant(
		long playerId,
		String name,
		String accountType,
		double ehpGained,
		double ehbGained,
		long collectionsGained,
		Instant trackedFrom,
		Instant trackedUntil,
		Instant joinedAt,
		boolean rosterEligible)
	{
		this.playerId = Math.max(0L, playerId);
		this.name = normalizePlayerName(name);
		this.accountType = normalizeAccountType(accountType);
		this.ehpGained = finitePositive(ehpGained);
		this.ehbGained = finitePositive(ehbGained);
		this.collectionsGained = Math.max(0L, collectionsGained);
		this.trackedFrom = trackedFrom;
		this.trackedUntil = trackedUntil;
		this.joinedAt = joinedAt;
		this.rosterEligible = rosterEligible;
	}

	public long getPlayerId()
	{
		return playerId;
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

	public Instant getJoinedAt()
	{
		return joinedAt;
	}

	public boolean isRosterEligible()
	{
		return rosterEligible;
	}

	public boolean isActive()
	{
		return ehpGained > 0.0 || ehbGained > 0.0 || collectionsGained > 0L;
	}

	private static double finitePositive(double value)
	{
		return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
	}

	static String normalizePlayerName(String value)
	{
		try
		{
			return OsrsPlayerName.requireValid(value);
		}
		catch (IllegalArgumentException ex)
		{
			return "";
		}
	}

	static String normalizeAccountType(String value)
	{
		String type = value == null ? "" : value.trim();
		if (type.isEmpty() || type.length() > 32)
		{
			return "unknown";
		}
		for (int index = 0; index < type.length(); index++)
		{
			char character = type.charAt(index);
			if (!(character >= 'A' && character <= 'Z')
				&& !(character >= 'a' && character <= 'z')
				&& !(character >= '0' && character <= '9')
				&& character != '-'
				&& character != '_')
			{
				return "unknown";
			}
		}
		return type;
	}
}
