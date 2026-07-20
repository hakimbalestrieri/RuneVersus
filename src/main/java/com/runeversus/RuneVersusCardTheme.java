package com.runeversus;

import java.awt.Color;

public enum RuneVersusCardTheme
{
	AUTO("Auto", new Color(73, 190, 176), new Color(194, 67, 76), new Color(246, 197, 92), new Color(20, 22, 28), new Color(8, 10, 15)),
	PVM("PvM", new Color(236, 91, 74), new Color(177, 43, 62), new Color(255, 193, 91), new Color(31, 20, 24), new Color(10, 8, 12)),
	SKILLING("Skilling", new Color(80, 188, 130), new Color(72, 140, 218), new Color(230, 209, 114), new Color(16, 28, 24), new Color(7, 13, 17)),
	IRONMAN("Ironman", new Color(196, 196, 196), new Color(166, 52, 57), new Color(238, 177, 68), new Color(24, 24, 27), new Color(7, 7, 9)),
	CLAN_WAR("Clan War", new Color(78, 132, 225), new Color(205, 67, 84), new Color(247, 204, 98), new Color(17, 20, 35), new Color(8, 8, 18)),
	UNDERDOG("Underdog", new Color(114, 214, 190), new Color(222, 133, 61), new Color(255, 223, 118), new Color(25, 21, 34), new Color(9, 8, 14));

	private final String displayName;
	private final Color leftAccent;
	private final Color rightAccent;
	private final Color gold;
	private final Color backgroundTop;
	private final Color backgroundBottom;

	RuneVersusCardTheme(String displayName, Color leftAccent, Color rightAccent, Color gold, Color backgroundTop, Color backgroundBottom)
	{
		this.displayName = displayName;
		this.leftAccent = leftAccent;
		this.rightAccent = rightAccent;
		this.gold = gold;
		this.backgroundTop = backgroundTop;
		this.backgroundBottom = backgroundBottom;
	}

	public Color getLeftAccent()
	{
		return leftAccent;
	}

	public Color getRightAccent()
	{
		return rightAccent;
	}

	public Color getGold()
	{
		return gold;
	}

	public Color getBackgroundTop()
	{
		return backgroundTop;
	}

	public Color getBackgroundBottom()
	{
		return backgroundBottom;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
