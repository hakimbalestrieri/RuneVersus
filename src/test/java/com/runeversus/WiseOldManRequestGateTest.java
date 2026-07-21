package com.runeversus;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.Assert;
import org.junit.Test;

public class WiseOldManRequestGateTest
{
	@Test
	public void enforcesRollingWindowAndRecoversAfterIt()
		throws Exception
	{
		AtomicLong now = new AtomicLong(10_000L);
		WiseOldManRequestGate gate = new WiseOldManRequestGate(
			2, 1_000L, 0L, now::get, millis -> now.addAndGet(millis));

		gate.acquire();
		gate.acquire();
		try
		{
			gate.acquire();
			Assert.fail("Expected a local WOM rate-limit exception");
		}
		catch (WiseOldManRateLimitException ex)
		{
			Assert.assertEquals(1L, ex.getRetryAfterSeconds());
		}

		now.addAndGet(1_000L);
		gate.acquire();
	}

	@Test
	public void honorsServerRetryAfterCooldown()
		throws Exception
	{
		AtomicLong now = new AtomicLong(20_000L);
		WiseOldManRequestGate gate = new WiseOldManRequestGate(
			10, 60_000L, 0L, now::get, millis -> now.addAndGet(millis));

		WiseOldManRateLimitException response = gate.backOff("12");
		Assert.assertEquals(12L, response.getRetryAfterSeconds());
		try
		{
			gate.acquire();
			Assert.fail("Expected the server cooldown to remain active");
		}
		catch (WiseOldManRateLimitException ex)
		{
			Assert.assertEquals(12L, ex.getRetryAfterSeconds());
		}

		now.addAndGet(12_000L);
		gate.acquire();
	}
}
