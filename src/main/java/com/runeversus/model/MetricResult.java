package com.runeversus.model;

public class MetricResult
{
	private final MetricType type;
	private final String name;
	private final long leftValue;
	private final long rightValue;
	private final boolean lowerIsBetter;

	public MetricResult(MetricType type, String name, long leftValue, long rightValue)
	{
		this(type, name, leftValue, rightValue, false);
	}

	public MetricResult(MetricType type, String name, long leftValue, long rightValue, boolean lowerIsBetter)
	{
		this.type = type;
		this.name = name;
		this.leftValue = leftValue;
		this.rightValue = rightValue;
		this.lowerIsBetter = lowerIsBetter;
	}

	public MetricType getType()
	{
		return type;
	}

	public String getName()
	{
		return name;
	}

	public long getLeftValue()
	{
		return leftValue;
	}

	public long getRightValue()
	{
		return rightValue;
	}

	public long getGap()
	{
		return Math.abs(leftValue - rightValue);
	}

	public boolean isLowerIsBetter()
	{
		return lowerIsBetter;
	}

	public PlayerSide getWinner()
	{
		if (leftValue == rightValue)
		{
			return PlayerSide.TIE;
		}
		if (lowerIsBetter)
		{
			return leftValue < rightValue ? PlayerSide.LEFT : PlayerSide.RIGHT;
		}
		return leftValue > rightValue ? PlayerSide.LEFT : PlayerSide.RIGHT;
	}
}
