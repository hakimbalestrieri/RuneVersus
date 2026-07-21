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
		this.name = MonthlyLeagueParticipant.normalizePlayerName(name);
		this.accountType = MonthlyLeagueParticipant.normalizeAccountType(accountType);
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
