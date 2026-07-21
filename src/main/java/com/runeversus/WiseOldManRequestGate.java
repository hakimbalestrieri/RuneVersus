package com.runeversus;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongSupplier;

final class WiseOldManRequestGate
{
	private static final int DEFAULT_MAX_REQUESTS = 18;
	private static final long DEFAULT_WINDOW_MILLIS = 60_000L;
	private static final long DEFAULT_MIN_SPACING_MILLIS = 250L;
	private static final long DEFAULT_BACKOFF_MILLIS = 60_000L;

	private final int maxRequests;
	private final long windowMillis;
	private final long minimumSpacingMillis;
	private final LongSupplier clock;
	private final Sleeper sleeper;
	private final Deque<Long> recentRequests = new ArrayDeque<>();
	private long lastRequestAt = Long.MIN_VALUE;
	private long blockedUntil;

	WiseOldManRequestGate()
	{
		this(DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_MILLIS, DEFAULT_MIN_SPACING_MILLIS,
			System::currentTimeMillis, Thread::sleep);
	}

	WiseOldManRequestGate(
		int maxRequests,
		long windowMillis,
		long minimumSpacingMillis,
		LongSupplier clock,
		Sleeper sleeper)
	{
		this.maxRequests = maxRequests;
		this.windowMillis = windowMillis;
		this.minimumSpacingMillis = minimumSpacingMillis;
		this.clock = clock;
		this.sleeper = sleeper;
	}

	synchronized void acquire() throws IOException
	{
		long now = clock.getAsLong();
		if (now < blockedUntil)
		{
			throw new WiseOldManRateLimitException(secondsUntil(blockedUntil, now));
		}

		prune(now);
		if (recentRequests.size() >= maxRequests)
		{
			long retryAt = recentRequests.peekFirst() + windowMillis;
			throw new WiseOldManRateLimitException(secondsUntil(retryAt, now));
		}

		long waitMillis = lastRequestAt == Long.MIN_VALUE
			? 0L : minimumSpacingMillis - (now - lastRequestAt);
		if (waitMillis > 0L)
		{
			try
			{
				sleeper.sleep(waitMillis);
			}
			catch (InterruptedException ex)
			{
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while waiting for Wise Old Man", ex);
			}
			now = clock.getAsLong();
			prune(now);
		}

		recentRequests.addLast(now);
		lastRequestAt = now;
	}

	synchronized WiseOldManRateLimitException backOff(String retryAfterHeader)
	{
		long retrySeconds = parseRetryAfter(retryAfterHeader);
		long now = clock.getAsLong();
		blockedUntil = Math.max(blockedUntil, now + retrySeconds * 1_000L);
		return new WiseOldManRateLimitException(retrySeconds);
	}

	private void prune(long now)
	{
		while (!recentRequests.isEmpty() && now - recentRequests.peekFirst() >= windowMillis)
		{
			recentRequests.removeFirst();
		}
	}

	private static long parseRetryAfter(String header)
	{
		if (header != null)
		{
			try
			{
				return Math.max(1L, Long.parseLong(header.trim()));
			}
			catch (NumberFormatException ignored)
			{
				// HTTP-date values are uncommon on WOM; use a conservative fallback.
			}
		}
		return DEFAULT_BACKOFF_MILLIS / 1_000L;
	}

	private static long secondsUntil(long target, long now)
	{
		return Math.max(1L, (target - now + 999L) / 1_000L);
	}

	@FunctionalInterface
	interface Sleeper
	{
		void sleep(long millis) throws InterruptedException;
	}
}
