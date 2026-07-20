package com.runeversus;

public enum VerdictStyle
{
	SERIOUS("Serious"),
	FUN("Fun"),
	SAVAGE("Savage");

	private final String displayName;

	VerdictStyle(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
