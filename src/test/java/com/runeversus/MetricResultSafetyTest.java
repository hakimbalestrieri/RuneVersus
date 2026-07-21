package com.runeversus;

import com.runeversus.model.MetricResult;
import com.runeversus.model.MetricType;
import org.junit.Assert;
import org.junit.Test;

public class MetricResultSafetyTest
{
	@Test
	public void saturatesAnUnrepresentableGap()
	{
		MetricResult metric = new MetricResult(
			MetricType.SKILL, "Overall", Long.MAX_VALUE, Long.MIN_VALUE);

		Assert.assertEquals(Long.MAX_VALUE, metric.getGap());
	}
}
