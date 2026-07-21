package com.runeversus;

import java.io.File;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class ClanProgressCardPreview
{
	public static void main(String[] args) throws Exception
	{
		ClanProgressLeaderboard leaderboard = new ClanProgressLeaderboard(
			"Example Clan",
			139,
			Arrays.asList(
				player("Lynx Titan", 9_800_000, 38_400_000, 141_000_000, 812_000_000, 1, 7, 24, 101, 32, 184, 640, 4_280),
				player("Collectionist", 4_200_000, 21_700_000, 95_000_000, 504_000_000, 6, 19, 58, 244, 18, 226, 980, 5_760),
				player("Raid Captain", 6_100_000, 29_600_000, 172_000_000, 733_000_000, 3, 11, 41, 190, 94, 418, 1_740, 8_930)));

		File out = new File(args.length == 0
			? "build/previews/runeversus-clan-progress-preview.png"
			: args[0]);
		File parent = out.getParentFile();
		if (parent != null && !parent.exists())
		{
			parent.mkdirs();
		}
		ImageIO.write(new RuneVersusCardRenderer().renderClanProgress(
			leaderboard, RuneVersusCardTheme.CLAN_WAR), "png", out);
		System.out.println(out.getAbsolutePath());
	}

	private static ClanProgressPlayer player(
		String name,
		long dayXp, long weekXp, long monthXp, long yearXp,
		long dayClogs, long weekClogs, long monthClogs, long yearClogs,
		long dayKc, long weekKc, long monthKc, long yearKc)
	{
		Map<GainPeriod, ClanProgressGains> gains = new EnumMap<>(GainPeriod.class);
		gains.put(GainPeriod.DAY, new ClanProgressGains(dayXp, dayClogs, dayKc));
		gains.put(GainPeriod.WEEK, new ClanProgressGains(weekXp, weekClogs, weekKc));
		gains.put(GainPeriod.MONTH, new ClanProgressGains(monthXp, monthClogs, monthKc));
		gains.put(GainPeriod.YEAR, new ClanProgressGains(yearXp, yearClogs, yearKc));
		gains.put(GainPeriod.ALL_TIME, new ClanProgressGains(yearXp * 6, yearClogs * 6, yearKc * 6));
		return new ClanProgressPlayer(name, gains);
	}
}
