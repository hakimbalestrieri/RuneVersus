package com.runeversus;

import com.runeversus.model.DuelResult;
import com.runeversus.model.MetricResult;
import com.runeversus.model.MetricType;
import com.runeversus.model.PlayerProfile;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class RuneVersusChatFormatterTest
{
	@Test
	public void formatsXpAndCollectionComparisonOnOneLine()
	{
		DuelResult duel = duel(
			Arrays.asList(
				new MetricResult(MetricType.SKILL, "Attack", 100_000_000L, 20_000_000L),
				new MetricResult(MetricType.SKILL, "Magic", 20_000_000L, 15_800_000L)),
			Arrays.asList(
				new MetricResult(MetricType.COLLECTION_LOG, "Collections Logged", 1_242, 1_087),
				new MetricResult(MetricType.COMBAT_ACHIEVEMENTS, "Combat Achievements", 5, 6)));

		Assert.assertEquals(
			"[VS] Hakim 2-0 Rival | XP: +84.2M | CLog: 1,242 vs 1,087",
			RuneVersusChatFormatter.format(duel));
	}

	@Test
	public void scoresOnlyXpAndCollectionLog()
	{
		DuelResult duel = duel(
			Collections.singletonList(
				new MetricResult(MetricType.SKILL, "Attack", 15_000_000L, 10_000_000L)),
			Collections.singletonList(
				new MetricResult(MetricType.COLLECTION_LOG, "Collections Logged", 800, 900)));

		Assert.assertEquals(
			"[VS] Hakim 1-1 Rival | XP: +5.0M | CLog: 800 vs 900",
			RuneVersusChatFormatter.format(duel));
	}

	@Test
	public void parsesCombatAchievementTierAliases()
	{
		Assert.assertEquals(CombatAchievementTier.MASTER, CombatAchievementTier.parse("master"));
		Assert.assertEquals(CombatAchievementTier.GRANDMASTER, CombatAchievementTier.parse("Grand Master"));
		Assert.assertEquals(CombatAchievementTier.UNRANKED, CombatAchievementTier.parse("none"));
		Assert.assertEquals(CombatAchievementTier.UNKNOWN, CombatAchievementTier.parse("invalid"));
	}

	private static DuelResult duel(
		java.util.List<MetricResult> skills,
		java.util.List<MetricResult> activities)
	{
		return new DuelResult(
			new PlayerProfile("Hakim", null),
			new PlayerProfile("Rival", null),
			skills,
			Collections.emptyList(),
			activities,
			Collections.emptyList(),
			Collections.emptyList(),
			Collections.emptyList());
	}
}
