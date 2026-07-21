package com.runeversus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
		this.personalBests = Collections.unmodifiableMap(new LinkedHashMap<>(personalBests));
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
