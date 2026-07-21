package com.runeversus;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class SyncedPlayerData
{
	static final SyncedPlayerData EMPTY = new SyncedPlayerData(
		Collections.emptyMap(),
		0L,
		CombatAchievementTier.UNKNOWN);

	private final Map<String, Long> personalBests;
	private final long collectionItems;
	private final CombatAchievementTier combatAchievementTier;

	SyncedPlayerData(
		Map<String, Long> personalBests,
		long collectionItems,
		CombatAchievementTier combatAchievementTier)
	{
		TreeMap<String, Long> normalizedBests = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		normalizedBests.putAll(personalBests);
		this.personalBests = Collections.unmodifiableMap(normalizedBests);
		this.collectionItems = collectionItems;
		this.combatAchievementTier = combatAchievementTier == null
			? CombatAchievementTier.UNKNOWN
			: combatAchievementTier;
	}

	public Map<String, Long> getPersonalBests()
	{
		return personalBests;
	}

	public long getCollectionItems()
	{
		return collectionItems;
	}

	public CombatAchievementTier getCombatAchievementTier()
	{
		return combatAchievementTier;
	}

	public boolean isEmpty()
	{
		return personalBests.isEmpty() && collectionItems <= 0 && !combatAchievementTier.isKnown();
	}
}
