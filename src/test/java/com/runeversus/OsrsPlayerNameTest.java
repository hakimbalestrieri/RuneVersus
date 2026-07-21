package com.runeversus;

import org.junit.Assert;
import org.junit.Test;

public class OsrsPlayerNameTest
{
	@Test
	public void normalizesValidRuneScapeNames()
	{
		Assert.assertEquals("1 JOUR MAXED", OsrsPlayerName.requireValid(" 1\u00a0JOUR MAXED "));
		Assert.assertEquals("player_99-x", OsrsPlayerName.requireValid("player_99-x"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNamesThatCouldAlterARequestPath()
	{
		OsrsPlayerName.requireValid("Alice/Bob");
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNamesLongerThanTheGameLimit()
	{
		OsrsPlayerName.requireValid("thirteenchars");
	}
}
