package com.runeversus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SyncedPlayerData
{
	static final SyncedPlayerData EMPTY = new SyncedPlayerData(Collections.emptyMap(), 0L);

	private final Map<String, Long> personalBests;
	private final long collectionItems;

	SyncedPlayerData(Map<String, Long> personalBests, long collectionItems)
	{
		this.personalBests = Collections.unmodifiableMap(new LinkedHashMap<>(personalBests));
		this.collectionItems = collectionItems;
	}

	public Map<String, Long> getPersonalBests()
	{
		return personalBests;
	}

	public long getCollectionItems()
	{
		return collectionItems;
	}

	public boolean isEmpty()
	{
		return personalBests.isEmpty() && collectionItems <= 0;
	}
}
