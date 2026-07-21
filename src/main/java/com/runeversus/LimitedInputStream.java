package com.runeversus;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

final class LimitedInputStream extends FilterInputStream
{
	private final long limit;
	private long consumed;

	LimitedInputStream(InputStream input, long limit)
	{
		super(input);
		this.limit = limit;
	}

	@Override
	public int read() throws IOException
	{
		int value = super.read();
		if (value >= 0)
		{
			record(1L);
		}
		return value;
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException
	{
		int allowed = (int) Math.min(length, Math.max(1L, limit - consumed + 1L));
		int count = super.read(buffer, offset, allowed);
		if (count > 0)
		{
			record(count);
		}
		return count;
	}

	private void record(long count) throws IOException
	{
		consumed += count;
		if (consumed > limit)
		{
			throw new IOException("Input exceeds the configured size limit");
		}
	}
}
