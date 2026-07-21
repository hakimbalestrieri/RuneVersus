package com.runeversus;

import org.junit.Assert;
import org.junit.Test;

public class RuneVersusConfigTest
{
	@Test
	public void opensDetailedComparisonWindowByDefault()
	{
		RuneVersusConfig config = new RuneVersusConfig() { };
		Assert.assertTrue(config.openInterfaceOnComparison());
		Assert.assertTrue(config.clanSetMenuOptions());
	}
}
