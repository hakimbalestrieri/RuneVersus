package com.runeversus;

import com.runeversus.model.DuelResult;
import com.runeversus.model.MetricResult;
import com.runeversus.model.MetricType;
import com.runeversus.model.PlayerSide;

public final class RuneVersusFlavor
{
	private RuneVersusFlavor()
	{
	}

	public static RuneVersusCardTheme resolveTheme(DuelResult duel, RuneVersusCardTheme configured)
	{
		if (configured != RuneVersusCardTheme.AUTO)
		{
			return configured;
		}

		int pvmGap = Math.abs(duel.getLeftBossWins() - duel.getRightBossWins());
		int skillGap = Math.abs(duel.getLeftSkillWins() - duel.getRightSkillWins());
		int totalGap = Math.abs(duel.getLeftTotalWins() - duel.getRightTotalWins());
		if (totalGap <= 2 && Math.max(duel.getLeftTotalWins(), duel.getRightTotalWins()) > 5)
		{
			return RuneVersusCardTheme.UNDERDOG;
		}
		if (pvmGap >= skillGap + 3)
		{
			return RuneVersusCardTheme.PVM;
		}
		if (skillGap >= pvmGap + 3)
		{
			return RuneVersusCardTheme.SKILLING;
		}
		return RuneVersusCardTheme.CLAN_WAR;
	}

	public static String verdict(DuelResult duel, VerdictStyle style)
	{
		if (style == VerdictStyle.SERIOUS)
		{
			return duel.getVerdict();
		}

		String winner = duel.getWinnerName();
		if ("Tie".equals(winner))
		{
			return style == VerdictStyle.SAVAGE
				? "Nobody gets to talk. It is tied."
				: "Dead even. One more trip decides the pub argument.";
		}

		int pvmGap = Math.abs(duel.getLeftBossWins() - duel.getRightBossWins());
		int skillGap = Math.abs(duel.getLeftSkillWins() - duel.getRightSkillWins());
		if (style == VerdictStyle.SAVAGE)
		{
			if (pvmGap >= skillGap + 4)
			{
				return winner + " brought boss receipts. This is a PvM diff.";
			}
			if (skillGap >= pvmGap + 4)
			{
				return winner + " wins the XP spreadsheet war.";
			}
			return winner + " wins. The other account has homework.";
		}

		if (pvmGap >= skillGap + 4)
		{
			return winner + " is wearing the PvM crown tonight.";
		}
		if (skillGap >= pvmGap + 4)
		{
			return winner + " wins with skilling depth.";
		}
		return winner + " edges it. Certified rivalry fuel.";
	}

	public static String snipeCallout(DuelResult duel)
	{
		MetricResult closest = duel.getClosestSteal();
		if (closest == null)
		{
			return "No obvious snipe yet.";
		}

		PlayerSide winner = closest.getWinner();
		String chaser = winner == PlayerSide.LEFT ? duel.getRight().getName() : duel.getLeft().getName();
		if (closest.isLowerIsBetter())
		{
			String unit = closest.getType() == MetricType.PERSONAL_BEST ? "seconds faster" : "lower";
			return chaser + " needs " + format(closest.getGap() + 1) + " " + unit + " to steal " + closest.getName() + ".";
		}

		String action = closest.getType().getLabel().equals("Skills") ? "XP" : "score";
		if (closest.getType().getLabel().contains("Boss"))
		{
			action = "KC";
		}
		return chaser + " needs " + format(closest.getGap() + 1) + " " + action + " to steal " + closest.getName() + ".";
	}

	public static String format(long value)
	{
		long absolute = Math.abs(value);
		if (absolute >= 1_000_000_000L)
		{
			return String.format("%.1fb", value / 1_000_000_000.0);
		}
		if (absolute >= 1_000_000L)
		{
			return String.format("%.1fm", value / 1_000_000.0);
		}
		if (absolute >= 10_000L)
		{
			return String.format("%.1fk", value / 1_000.0);
		}
		return String.format("%,d", value);
	}
}
