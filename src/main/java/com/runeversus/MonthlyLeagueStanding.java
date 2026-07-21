package com.runeversus;

public class MonthlyLeagueStanding
{
	private final MonthlyLeagueParticipant participant;
	private final boolean eligible;
	private final boolean fresh;
	private final double skillingScore;
	private final double pvmScore;
	private final double overallScore;
	private final int overallRank;
	private final int skillingRank;
	private final int pvmRank;
	private final int collectionRank;

	MonthlyLeagueStanding(
		MonthlyLeagueParticipant participant,
		boolean eligible,
		boolean fresh,
		double skillingScore,
		double pvmScore,
		double overallScore,
		int overallRank,
		int skillingRank,
		int pvmRank,
		int collectionRank)
	{
		this.participant = participant;
		this.eligible = eligible;
		this.fresh = fresh;
		this.skillingScore = skillingScore;
		this.pvmScore = pvmScore;
		this.overallScore = overallScore;
		this.overallRank = overallRank;
		this.skillingRank = skillingRank;
		this.pvmRank = pvmRank;
		this.collectionRank = collectionRank;
	}

	public String getName()
	{
		return participant.getName();
	}

	public String getAccountType()
	{
		return participant.getAccountType();
	}

	public double getEhpGained()
	{
		return participant.getEhpGained();
	}

	public double getEhbGained()
	{
		return participant.getEhbGained();
	}

	public long getCollectionsGained()
	{
		return participant.getCollectionsGained();
	}

	public boolean isEligible()
	{
		return eligible;
	}

	public boolean isFresh()
	{
		return fresh;
	}

	public boolean isActive()
	{
		return participant.isActive();
	}

	public double getSkillingScore()
	{
		return skillingScore;
	}

	public double getPvmScore()
	{
		return pvmScore;
	}

	public double getOverallScore()
	{
		return overallScore;
	}

	public int getOverallRank()
	{
		return overallRank;
	}

	public int getSkillingRank()
	{
		return skillingRank;
	}

	public int getPvmRank()
	{
		return pvmRank;
	}

	public int getCollectionRank()
	{
		return collectionRank;
	}

	public int rankFor(MonthlyLeagueMetric metric)
	{
		switch (metric)
		{
			case SKILLING:
				return skillingRank;
			case PVM:
				return pvmRank;
			case COLLECTION:
				return collectionRank;
			case OVERALL:
			default:
				return overallRank;
		}
	}
}
