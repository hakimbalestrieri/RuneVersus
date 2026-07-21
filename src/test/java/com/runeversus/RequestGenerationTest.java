package com.runeversus;

import org.junit.Assert;
import org.junit.Test;

public class RequestGenerationTest
{
	@Test
	public void onlyLatestRequestRemainsCurrent()
	{
		RequestGeneration requests = new RequestGeneration();
		long first = requests.next();
		long second = requests.next();

		Assert.assertFalse(requests.isCurrent(first));
		Assert.assertTrue(requests.isCurrent(second));

		requests.invalidate();
		Assert.assertFalse(requests.isCurrent(second));
	}
}
