package com.runeversus;

import java.io.IOException;

final class WiseOldManRateLimitException extends IOException
{
	private final long retryAfterSeconds;

	WiseOldManRateLimitException(long retryAfterSeconds)
	{
		super("Wise Old Man rate limit reached. Retry in " + Math.max(1L, retryAfterSeconds) + "s");
		this.retryAfterSeconds = Math.max(1L, retryAfterSeconds);
	}

	long getRetryAfterSeconds()
	{
		return retryAfterSeconds;
	}
}
