package com.runeversus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;

@Singleton
public class OptInSyncService
{
	private static final String PB_PREFIX = "pb.";
	private static final String PERSONAL_BEST_PREFIX = "personalBest.";

	private final RuneVersusConfig config;

	@Inject
	public OptInSyncService(RuneVersusConfig config)
	{
		this.config = config;
	}

	public SyncedPlayerData load(String playerName)
	{
		if (!config.localOptInSync())
		{
			return SyncedPlayerData.EMPTY;
		}

		File file = new File(new File(RuneLite.RUNELITE_DIR, "rune-versus/sync"), sanitize(playerName) + ".properties");
		if (!file.isFile())
		{
			return SyncedPlayerData.EMPTY;
		}

		Properties properties = new Properties();
		try (FileInputStream in = new FileInputStream(file))
		{
			properties.load(in);
		}
		catch (IOException ex)
		{
			return SyncedPlayerData.EMPTY;
		}

		Map<String, Long> personalBests = new LinkedHashMap<>();
		for (String key : properties.stringPropertyNames())
		{
			if (key.startsWith(PB_PREFIX))
			{
				addPersonalBest(personalBests, key.substring(PB_PREFIX.length()), properties.getProperty(key));
			}
			else if (key.startsWith(PERSONAL_BEST_PREFIX))
			{
				addPersonalBest(personalBests, key.substring(PERSONAL_BEST_PREFIX.length()), properties.getProperty(key));
			}
		}

		long collectionItems = longProperty(properties, "collection.items");
		if (collectionItems <= 0)
		{
			collectionItems = longProperty(properties, "collectionLog.items");
		}

		CombatAchievementTier combatAchievementTier = CombatAchievementTier.parse(
			firstProperty(properties,
				"combatAchievements.tier",
				"combatAchievement.tier",
				"ca.tier"));
		return new SyncedPlayerData(personalBests, collectionItems, combatAchievementTier);
	}

	private static String firstProperty(Properties properties, String... keys)
	{
		for (String key : keys)
		{
			String value = properties.getProperty(key);
			if (value != null && !value.trim().isEmpty())
			{
				return value;
			}
		}
		return null;
	}

	private static void addPersonalBest(Map<String, Long> personalBests, String bossName, String value)
	{
		String boss = bossName == null ? "" : bossName.trim().replace('_', ' ');
		long seconds = parseLong(value);
		if (!boss.isEmpty() && seconds > 0)
		{
			personalBests.put(boss, seconds);
		}
	}

	private static long longProperty(Properties properties, String key)
	{
		return parseLong(properties.getProperty(key));
	}

	private static long parseLong(String value)
	{
		if (value == null)
		{
			return 0L;
		}
		try
		{
			return Long.parseLong(value.trim());
		}
		catch (NumberFormatException ex)
		{
			return 0L;
		}
	}

	private static String sanitize(String name)
	{
		if (name == null || name.trim().isEmpty())
		{
			return "player";
		}
		return name.trim().replaceAll("[^a-zA-Z0-9_-]+", "_");
	}
}
