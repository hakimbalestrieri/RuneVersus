package com.runeversus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

public class LimitedInputStreamTest
{
	@Test
	public void rejectsInputBeyondTheLimit() throws Exception
	{
		LimitedInputStream input = new LimitedInputStream(
			new ByteArrayInputStream(new byte[] {1, 2, 3, 4}), 3L);
		byte[] buffer = new byte[8];

		try
		{
			input.read(buffer);
			Assert.fail("Expected the stream limit to be enforced");
		}
		catch (IOException expected)
		{
			Assert.assertTrue(expected.getMessage().contains("size limit"));
		}
	}
}
