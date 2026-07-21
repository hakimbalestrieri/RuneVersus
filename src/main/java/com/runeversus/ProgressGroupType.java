package com.runeversus;

public enum ProgressGroupType
{
	CLAN("CLAN PERFORMANCE", "CLAN PROGRESS", "Five periods. Three crowns. One clan."),
	FRIENDS_CHAT("FRIEND CHAT PERFORMANCE", "FRIEND CHAT PROGRESS", "Five periods. Three crowns. One Friend Chat.");

	private final String windowTitle;
	private final String cardTitle;
	private final String cardTagline;

	ProgressGroupType(String windowTitle, String cardTitle, String cardTagline)
	{
		this.windowTitle = windowTitle;
		this.cardTitle = cardTitle;
		this.cardTagline = cardTagline;
	}

	public String getWindowTitle()
	{
		return windowTitle;
	}

	public String getCardTitle()
	{
		return cardTitle;
	}

	public String getCardTagline()
	{
		return cardTagline;
	}
}
