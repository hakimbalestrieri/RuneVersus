package com.runeversus;

import java.time.Instant;

final class MonthlyLeagueMembership
{
	private final long playerId;
	private final String name;
	private final String accountType;
	private final Instant joinedAt;

	MonthlyLeagueMembership(long playerId, String name, String accountType, Instant joinedAt)
	{
		this.playerId = Math.max(0L, playerId);
		this.name = name == null ? "" : name.trim();
		this.accountType = accountType == null ? "unknown" : accountType.trim();
		this.joinedAt = joinedAt;
	}

	long getPlayerId()
	{
		return playerId;
	}

	String getName()
	{
		return name;
	}

	String getAccountType()
	{
		return accountType;
	}

	Instant getJoinedAt()
	{
		return joinedAt;
	}
}
