package com.runeversus;

import com.runeversus.model.DuelResult;
import com.runeversus.model.MetricResult;
import com.runeversus.model.MetricType;
import com.runeversus.model.PlayerProfile;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import javax.imageio.ImageIO;

public class DuelCardPreview
{
	public static void main(String[] args) throws Exception
	{
		DuelResult duel = new DuelResult(
			new PlayerProfile("Raid Captain", null),
			new PlayerProfile("Clog Hunter", null),
			Arrays.asList(
				new MetricResult(MetricType.SKILL, "Attack", 20_000_000, 13_000_000),
				new MetricResult(MetricType.SKILL, "Magic", 15_000_000, 32_000_000),
				new MetricResult(MetricType.SKILL, "Slayer", 24_000_000, 18_000_000)),
			Arrays.asList(
				new MetricResult(MetricType.BOSS, "Vorkath", 1_284, 927),
				new MetricResult(MetricType.BOSS, "Zulrah", 61, 2_212)),
			Collections.singletonList(new MetricResult(
				MetricType.COLLECTION_LOG, "Collections Logged", 914, 872)),
			Collections.singletonList(new MetricResult(MetricType.FORM_DAY, "Overall", 2_840_000, 1_120_000)),
			Collections.singletonList(new MetricResult(MetricType.FORM_WEEK, "Overall", 8_120_000, 11_480_000)),
			Collections.singletonList(new MetricResult(MetricType.FORM_MONTH, "Overall", 44_800_000, 39_250_000)));

		File out = new File(args.length == 0
			? "build/previews/runeversus-duel-card-preview.png"
			: args[0]);
		File parent = out.getParentFile();
		if (parent != null && !parent.exists())
		{
			parent.mkdirs();
		}
		ImageIO.write(new RuneVersusCardRenderer().render(
			duel, RuneVersusCardTheme.AUTO, "Raid Captain edges the all-around account clash."), "png", out);
		System.out.println(out.getAbsolutePath());
	}
}
