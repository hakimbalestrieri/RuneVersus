package com.runeversus;

import java.util.concurrent.atomic.AtomicLong;

final class RequestGeneration
{
	private final AtomicLong generation = new AtomicLong();

	long next()
	{
		return generation.incrementAndGet();
	}

	void invalidate()
	{
		generation.incrementAndGet();
	}

	boolean isCurrent(long request)
	{
		return generation.get() == request;
	}
}
