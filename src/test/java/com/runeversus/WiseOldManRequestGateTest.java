package com.runeversus;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

	@Test
	public void honorsHttpDateRetryAfterAndCapsExcessiveDelays()
	{
		AtomicLong now = new AtomicLong(Instant.parse("2026-07-21T10:00:00Z").toEpochMilli());
		WiseOldManRequestGate gate = new WiseOldManRequestGate(
			10, 60_000L, 0L, now::get, millis -> now.addAndGet(millis));
		String retryAt = DateTimeFormatter.RFC_1123_DATE_TIME.format(
			Instant.ofEpochMilli(now.get() + 12_000L).atZone(ZoneOffset.UTC));

		Assert.assertEquals(12L, gate.backOff(retryAt).getRetryAfterSeconds());

		WiseOldManRequestGate capped = new WiseOldManRequestGate(
			10, 60_000L, 0L, now::get, millis -> now.addAndGet(millis));
		Assert.assertEquals(3_600L, capped.backOff("999999999999").getRetryAfterSeconds());
	}
}
