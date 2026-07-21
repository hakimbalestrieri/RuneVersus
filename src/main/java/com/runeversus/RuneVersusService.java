package com.runeversus;

import com.runeversus.model.DuelResult;
import com.runeversus.model.MetricResult;
import com.runeversus.model.MetricType;
import com.runeversus.model.PlayerProfile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;
import net.runelite.client.hiscore.Skill;

@Singleton
public class RuneVersusService
{
	private static final HiscoreSkill[] PUBLIC_COLLECTION_METRICS = {
		HiscoreSkill.COLLECTIONS_LOGGED,
		HiscoreSkill.CLUE_SCROLL_ALL,
		HiscoreSkill.CLUE_SCROLL_MASTER,
		HiscoreSkill.CLUE_SCROLL_ELITE,
		HiscoreSkill.CLUE_SCROLL_HARD
	};

	private final HiscoreClient hiscoreClient;
	private final WiseOldManGainsClient wiseOldManGainsClient;
	private final OptInSyncService optInSyncService;
	private final RuneVersusConfig config;
	private final ScheduledExecutorService executor;

	@Inject
	public RuneVersusService(
		HiscoreClient hiscoreClient,
		WiseOldManGainsClient wiseOldManGainsClient,
		OptInSyncService optInSyncService,
		RuneVersusConfig config,
		ScheduledExecutorService executor)
	{
		this.hiscoreClient = hiscoreClient;
		this.wiseOldManGainsClient = wiseOldManGainsClient;
		this.optInSyncService = optInSyncService;
		this.config = config;
		this.executor = executor;
	}

	public CompletableFuture<DuelResult> compare(String leftName, String rightName)
	{
		String left = cleanName(leftName);
		String right = cleanName(rightName);
		return CompletableFuture.supplyAsync(() ->
		{
			try
			{
				PlayerProfile leftProfile = loadProfile(left);
				PlayerProfile rightProfile = loadProfile(right);
				return buildDuel(leftProfile, rightProfile);
			}
			catch (IOException ex)
			{
				throw new IllegalStateException(ex);
			}
		}, executor);
	}

	public CompletableFuture<ClanProgressLeaderboard> analyzeClanProgress(String label, int groupId)
	{
		return CompletableFuture.supplyAsync(() ->
		{
			Map<String, String> displayNames = new LinkedHashMap<>();
			Map<String, EnumMap<GainPeriod, ClanProgressGains>> gainsByPlayer = new LinkedHashMap<>();
			try
			{
				for (GainPeriod period : GainPeriod.values())
				{
					Map<String, ClanProgressGains> periodGains = wiseOldManGainsClient.getGroupGains(groupId, period);
					for (Map.Entry<String, ClanProgressGains> entry : periodGains.entrySet())
					{
						String key = normalizedName(entry.getKey());
						displayNames.putIfAbsent(key, entry.getKey());
						gainsByPlayer.computeIfAbsent(key, ignored -> new EnumMap<>(GainPeriod.class))
							.put(period, entry.getValue());
					}
				}
			}
			catch (IOException ex)
			{
				throw new IllegalStateException(ex);
			}

			List<ClanProgressPlayer> players = new ArrayList<>();
			for (Map.Entry<String, EnumMap<GainPeriod, ClanProgressGains>> entry : gainsByPlayer.entrySet())
			{
				players.add(new ClanProgressPlayer(displayNames.get(entry.getKey()), entry.getValue()));
			}
			players.sort(java.util.Comparator.comparing(ClanProgressPlayer::getName, String.CASE_INSENSITIVE_ORDER));
			return new ClanProgressLeaderboard(label, groupId, players);
		}, executor);
	}

	public CompletableFuture<ClanProgressLeaderboard> analyzeRosterProgress(
		String label,
		String sourceDescription,
		List<String> rosterNames)
	{
		List<String> names = rosterNames == null
			? Collections.emptyList() : new ArrayList<>(rosterNames);
		return CompletableFuture.supplyAsync(() ->
		{
			List<ClanProgressPlayer> players = new ArrayList<>();
			for (String rawName : names)
			{
				String name = cleanName(rawName);
				if (name.isEmpty())
				{
					continue;
				}

				HiscoreResult hiscores;
				try
				{
					hiscores = hiscoreClient.lookup(name, config.hiscoreEndpoint());
				}
				catch (IOException ex)
				{
					continue;
				}
				if (hiscores == null)
				{
					continue;
				}

				EnumMap<GainPeriod, ClanProgressGains> gains = new EnumMap<>(GainPeriod.class);
				gains.put(GainPeriod.ALL_TIME, progressFromHiscores(hiscores));
				for (GainPeriod period : GainPeriod.values())
				{
					if (period.isAllTime())
					{
						continue;
					}
					try
					{
						gains.put(period, wiseOldManGainsClient.getPlayerProgress(name, period));
					}
					catch (IOException ex)
					{
						gains.put(period, new ClanProgressGains(0L, 0L, 0L));
					}
				}
				players.add(new ClanProgressPlayer(name, gains));
			}
			players.sort(java.util.Comparator.comparing(ClanProgressPlayer::getName, String.CASE_INSENSITIVE_ORDER));
			return new ClanProgressLeaderboard(
				label,
				ProgressGroupType.FRIENDS_CHAT,
				sourceDescription,
				players);
		}, executor);
	}

	private PlayerProfile loadProfile(String name) throws IOException
	{
		HiscoreResult result = hiscoreClient.lookup(name, config.hiscoreEndpoint());
		if (result == null)
		{
			throw new IOException("No hiscore data for " + name);
		}

		if (!config.wiseOldManGains())
		{
			return new PlayerProfile(name, result);
		}

		Map<HiscoreSkill, Long> day = safeGains(name, "day");
		Map<HiscoreSkill, Long> week = safeGains(name, "week");
		Map<HiscoreSkill, Long> month = safeGains(name, "month");
		return new PlayerProfile(name, result, day, week, month, !day.isEmpty() || !week.isEmpty() || !month.isEmpty());
	}

	private Map<HiscoreSkill, Long> safeGains(String name, String period)
	{
		try
		{
			return wiseOldManGainsClient.getSkillExperienceGains(name, period);
		}
		catch (IOException ex)
		{
			return Collections.emptyMap();
		}
	}

	private DuelResult buildDuel(PlayerProfile left, PlayerProfile right)
	{
		List<MetricResult> skills = new ArrayList<>();
		List<MetricResult> bosses = new ArrayList<>();
		List<MetricResult> activities = new ArrayList<>();

		for (HiscoreSkill hiscoreSkill : HiscoreSkill.values())
		{
			if (hiscoreSkill.getType() == HiscoreSkillType.SKILL || hiscoreSkill == HiscoreSkill.OVERALL)
			{
				long leftValue = skillExperience(left.getHiscores(), hiscoreSkill);
				long rightValue = skillExperience(right.getHiscores(), hiscoreSkill);
				if (leftValue > 0 || rightValue > 0)
				{
					skills.add(new MetricResult(MetricType.SKILL, hiscoreSkill.getName(), leftValue, rightValue));
				}
			}
			else if (hiscoreSkill.getType() == HiscoreSkillType.BOSS)
			{
				long leftValue = score(left.getHiscores(), hiscoreSkill);
				long rightValue = score(right.getHiscores(), hiscoreSkill);
				if (leftValue > 0 || rightValue > 0)
				{
					bosses.add(new MetricResult(MetricType.BOSS, hiscoreSkill.getName(), leftValue, rightValue));
				}
			}
		}

		for (HiscoreSkill metric : PUBLIC_COLLECTION_METRICS)
		{
			long leftValue = score(left.getHiscores(), metric);
			long rightValue = score(right.getHiscores(), metric);
			if (leftValue > 0 || rightValue > 0)
			{
				MetricType type = metric == HiscoreSkill.COLLECTIONS_LOGGED
					? MetricType.COLLECTION_LOG
					: MetricType.ACTIVITY;
				activities.add(new MetricResult(type, metric.getName(), leftValue, rightValue));
			}
		}
		appendOptInMetrics(activities, optInSyncService.load(left.getName()), optInSyncService.load(right.getName()));

		return new DuelResult(
			left,
			right,
			skills,
			bosses,
			activities,
			formMetrics(MetricType.FORM_DAY, left.getDayGains(), right.getDayGains()),
			formMetrics(MetricType.FORM_WEEK, left.getWeekGains(), right.getWeekGains()),
			formMetrics(MetricType.FORM_MONTH, left.getMonthGains(), right.getMonthGains()));
	}

	private static void appendOptInMetrics(
		List<MetricResult> activities,
		SyncedPlayerData leftSync,
		SyncedPlayerData rightSync)
	{
		if ((leftSync == null || leftSync.isEmpty()) && (rightSync == null || rightSync.isEmpty()))
		{
			return;
		}

		SyncedPlayerData left = leftSync == null ? SyncedPlayerData.EMPTY : leftSync;
		SyncedPlayerData right = rightSync == null ? SyncedPlayerData.EMPTY : rightSync;
		if (left.getCollectionItems() > 0 && right.getCollectionItems() > 0)
		{
			activities.add(new MetricResult(
				MetricType.COLLECTION_LOG,
				"Opt-in Collection Items",
				left.getCollectionItems(),
				right.getCollectionItems()));
		}

		CombatAchievementTier leftTier = left.getCombatAchievementTier();
		CombatAchievementTier rightTier = right.getCombatAchievementTier();
		if (leftTier.isKnown() && rightTier.isKnown())
		{
			activities.add(new MetricResult(
				MetricType.COMBAT_ACHIEVEMENTS,
				"Combat Achievements",
				leftTier.getScore(),
				rightTier.getScore()));
		}

		Set<String> bosses = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		bosses.addAll(left.getPersonalBests().keySet());
		bosses.addAll(right.getPersonalBests().keySet());
		for (String boss : bosses)
		{
			long leftSeconds = left.getPersonalBests().getOrDefault(boss, 0L);
			long rightSeconds = right.getPersonalBests().getOrDefault(boss, 0L);
			if (leftSeconds > 0 && rightSeconds > 0)
			{
				activities.add(new MetricResult(
					MetricType.PERSONAL_BEST,
					"PB " + boss + " (s)",
					leftSeconds,
					rightSeconds,
					true));
			}
		}
	}

	private static List<MetricResult> formMetrics(MetricType type, Map<HiscoreSkill, Long> left, Map<HiscoreSkill, Long> right)
	{
		if ((left == null || left.isEmpty()) && (right == null || right.isEmpty()))
		{
			return Collections.emptyList();
		}

		Map<HiscoreSkill, Long> leftValues = left == null ? Collections.emptyMap() : left;
		Map<HiscoreSkill, Long> rightValues = right == null ? Collections.emptyMap() : right;
		List<MetricResult> out = new ArrayList<>();
		for (HiscoreSkill skill : HiscoreSkill.values())
		{
			if (skill == HiscoreSkill.OVERALL || skill.getType() == HiscoreSkillType.SKILL)
			{
				long leftValue = leftValues.getOrDefault(skill, 0L);
				long rightValue = rightValues.getOrDefault(skill, 0L);
				if (leftValue != 0 || rightValue != 0)
				{
					out.add(new MetricResult(type, skill.getName(), leftValue, rightValue));
				}
			}
		}
		return out;
	}

	private static long skillExperience(HiscoreResult result, HiscoreSkill skill)
	{
		Skill s = result.getSkill(skill);
		if (s == null)
		{
			return 0L;
		}
		if (s.getExperience() >= 0)
		{
			return s.getExperience();
		}
		return Math.max(0, s.getLevel());
	}

	private static long score(HiscoreResult result, HiscoreSkill skill)
	{
		Skill s = result.getSkill(skill);
		if (s == null)
		{
			return 0L;
		}
		return Math.max(0, s.getLevel());
	}

	private static String cleanName(String name)
	{
		return name == null ? "" : name.trim();
	}

	private static ClanProgressGains progressFromHiscores(HiscoreResult result)
	{
		Map<String, Long> bossKc = new LinkedHashMap<>();
		for (HiscoreSkill metric : HiscoreSkill.values())
		{
			if (metric.getType() == HiscoreSkillType.BOSS)
			{
				long kills = score(result, metric);
				if (kills > 0L)
				{
					bossKc.put(BossKcRegistry.displayName(metric.getName()), kills);
				}
			}
		}
		return new ClanProgressGains(
			skillExperience(result, HiscoreSkill.OVERALL),
			score(result, HiscoreSkill.COLLECTIONS_LOGGED),
			bossKc);
	}

	private static String normalizedName(String name)
	{
		return cleanName(name).replace('\u00a0', ' ').toLowerCase(Locale.ROOT);
	}
}
