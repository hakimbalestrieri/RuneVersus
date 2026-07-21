package com.runeversus;

import com.runeversus.model.DuelResult;
import com.runeversus.model.MetricResult;
import com.runeversus.model.PlayerSide;
import java.util.Locale;

public final class RuneVersusChatFormatter
{
	private RuneVersusChatFormatter()
	{
	}

	public static String format(DuelResult result)
	{
		MetricResult collectionLog = result.getCollectionLogMetric();
		long xpDifference = signedDifference(result.getLeftTotalXp(), result.getRightTotalXp());

		int leftScore = score(PlayerSide.LEFT, xpDifference, collectionLog);
		int rightScore = score(PlayerSide.RIGHT, xpDifference, collectionLog);

		return new StringBuilder("[VS] ")
			.append(result.getLeft().getName())
			.append(' ')
			.append(leftScore)
			.append('-')
			.append(rightScore)
			.append(' ')
			.append(result.getRight().getName())
			.append(" | XP: ")
			.append(formatSignedXp(xpDifference))
			.append(" | CLog: ")
			.append(formatCollectionLog(collectionLog))
			.toString();
	}

	private static int score(
		PlayerSide side,
		long xpDifference,
		MetricResult collectionLog)
	{
		int score = 0;
		if ((side == PlayerSide.LEFT && xpDifference > 0)
			|| (side == PlayerSide.RIGHT && xpDifference < 0))
		{
			score++;
		}
		if (collectionLog != null && collectionLog.getWinner() == side)
		{
			score++;
		}
		return score;
	}

	private static String formatSignedXp(long difference)
	{
		if (difference == 0)
		{
			return "0";
		}

		long absolute = difference == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(difference);
		String sign = difference > 0 ? "+" : "-";
		if (absolute >= 1_000_000_000L)
		{
			return sign + String.format(Locale.ROOT, "%.1fB", absolute / 1_000_000_000.0);
		}
		if (absolute >= 1_000_000L)
		{
			return sign + String.format(Locale.ROOT, "%.1fM", absolute / 1_000_000.0);
		}
		if (absolute >= 1_000L)
		{
			return sign + String.format(Locale.ROOT, "%.1fK", absolute / 1_000.0);
		}
		return sign + absolute;
	}

	private static long signedDifference(long left, long right)
	{
		try
		{
			return Math.subtractExact(left, right);
		}
		catch (ArithmeticException ex)
		{
			return left > right ? Long.MAX_VALUE : -Long.MAX_VALUE;
		}
	}

	private static String formatCollectionLog(MetricResult metric)
	{
		if (metric == null)
		{
			return "n/a";
		}
		return String.format(Locale.US, "%,d vs %,d", metric.getLeftValue(), metric.getRightValue());
	}
}
