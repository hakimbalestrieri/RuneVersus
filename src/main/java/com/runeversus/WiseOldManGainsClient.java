package com.runeversus;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.hiscore.HiscoreSkill;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

@Singleton
public class WiseOldManGainsClient
{
	private static final HttpUrl PLAYERS_URL = HttpUrl.parse("https://api.wiseoldman.net/v2/players/");
	private static final HttpUrl GROUPS_URL = HttpUrl.parse("https://api.wiseoldman.net/v2/groups/");
	private static final String USER_AGENT = "RuneVersus/0.1 (+https://github.com/hakimbalestrieri/RuneVersus)";
	private static final Duration PLAYER_CACHE_TTL = Duration.ofMinutes(5);
	private static final Duration GROUP_CACHE_TTL = Duration.ofMinutes(10);
	private static final Duration LEAGUE_CACHE_TTL = Duration.ofMinutes(30);
	private static final int MAX_CACHE_ENTRIES = 512;
	private static final int REQUEST_LOCK_STRIPES = 64;
	private static final int MAX_ARRAY_ENTRIES = 2_000;
	private static final long MAX_RESPONSE_BYTES = 16L * 1024L * 1024L;
	private static final Map<String, HiscoreSkill> SKILL_BY_WOM_METRIC = buildSkillMap();

	private final OkHttpClient okHttpClient;
	private final Gson gson;
	private final WiseOldManRequestGate requestGate;
	private final Object[] requestLocks = createRequestLocks();
	private final Map<String, CachedJson> responseCache = Collections.synchronizedMap(
		new LinkedHashMap<String, CachedJson>(64, 0.75f, true)
		{
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, CachedJson> eldest)
			{
				return size() > MAX_CACHE_ENTRIES;
			}
		});

	@Inject
	public WiseOldManGainsClient(OkHttpClient okHttpClient, Gson gson)
	{
		this(okHttpClient, gson, new WiseOldManRequestGate());
	}

	WiseOldManGainsClient(OkHttpClient okHttpClient, Gson gson, WiseOldManRequestGate requestGate)
	{
		this.okHttpClient = okHttpClient;
		this.gson = gson;
		this.requestGate = requestGate;
	}

	public Map<HiscoreSkill, Long> getSkillExperienceGains(String username, String period) throws IOException
	{
		final String playerName;
		try
		{
			playerName = OsrsPlayerName.requireValid(username);
		}
		catch (IllegalArgumentException ex)
		{
			throw new IOException(ex.getMessage(), ex);
		}
		HttpUrl url = PLAYERS_URL.newBuilder()
			.addPathSegment(playerName)
			.addPathSegment("gained")
			.build();

		JsonObject root = fetchJsonObject(
			url.newBuilder().addQueryParameter("period", period).build(), PLAYER_CACHE_TTL, false);
		JsonObject skills = objectAt(root, "data", "skills");
		Map<HiscoreSkill, Long> gains = new EnumMap<>(HiscoreSkill.class);
		if (skills == null)
		{
			return gains;
		}

		for (Map.Entry<String, JsonElement> entry : skills.entrySet())
		{
			HiscoreSkill skill = SKILL_BY_WOM_METRIC.get(entry.getKey());
			if (skill == null || !entry.getValue().isJsonObject())
			{
				continue;
			}

			JsonObject experience = object(entry.getValue().getAsJsonObject(), "experience");
			Long gained = optionalLong(experience, "gained");
			if (gained != null)
			{
				gains.put(skill, gained);
			}
		}
		return gains;
	}

	public Map<String, ClanProgressGains> getGroupGains(int groupId, GainPeriod period) throws IOException
	{
		return getGroupGains(groupId, period, false);
	}

	public Map<String, ClanProgressGains> getGroupGains(
		int groupId,
		GainPeriod period,
		boolean forceRefresh) throws IOException
	{
		if (groupId <= 0)
		{
			throw new IOException("Wise Old Man group ID must be greater than zero");
		}

		if (period == GainPeriod.ALL_TIME)
		{
			return getGroupAllTime(groupId, forceRefresh);
		}

		HttpUrl url = groupUrl(groupId, "bulk-gained");

		HttpUrl requestUrl = url.newBuilder().addQueryParameter("period", period.getApiValue()).build();
		return parseGroupGains(fetchJsonArray(requestUrl, GROUP_CACHE_TTL, forceRefresh));
	}

	public java.util.List<MonthlyLeagueParticipant> getMonthlyLeagueGains(
		int groupId,
		Instant startDate,
		Instant endDate) throws IOException
	{
		return getMonthlyLeagueGains(groupId, startDate, endDate, false);
	}

	public java.util.List<MonthlyLeagueParticipant> getMonthlyLeagueGains(
		int groupId,
		Instant startDate,
		Instant endDate,
		boolean forceRefresh) throws IOException
	{
		if (groupId <= 0)
		{
			throw new IOException("Wise Old Man group ID must be greater than zero");
		}
		if (startDate == null || endDate == null || !endDate.isAfter(startDate))
		{
			throw new IOException("Monthly league date range is invalid");
		}

		HttpUrl url = groupUrl(groupId, "bulk-gained");

		HttpUrl requestUrl = url.newBuilder()
			.addQueryParameter("startDate", startDate.toString())
			.addQueryParameter("endDate", endDate.toString())
			.build();
		return parseMonthlyLeagueGains(fetchJsonArray(requestUrl, LEAGUE_CACHE_TTL, forceRefresh));
	}

	public java.util.List<MonthlyLeagueMembership> getGroupMemberships(
		int groupId,
		boolean forceRefresh) throws IOException
	{
		if (groupId <= 0)
		{
			throw new IOException("Wise Old Man group ID must be greater than zero");
		}

		HttpUrl url = groupUrl(groupId);
		JsonObject root = fetchJsonObject(url, GROUP_CACHE_TTL, forceRefresh);
		JsonArray memberships = root.has("memberships") && root.get("memberships").isJsonArray()
			? root.getAsJsonArray("memberships") : null;
		if (memberships != null && memberships.size() > MAX_ARRAY_ENTRIES)
		{
			throw new IOException("Wise Old Man returned too many group members");
		}
		return parseGroupMemberships(root);
	}

	private Map<String, ClanProgressGains> getGroupAllTime(int groupId, boolean forceRefresh) throws IOException
	{
		HttpUrl url = groupUrl(groupId, "bulk-hiscores");

		return parseGroupAllTime(fetchJsonArray(url, GROUP_CACHE_TTL, forceRefresh));
	}

	private JsonArray fetchJsonArray(HttpUrl url, Duration ttl, boolean forceRefresh) throws IOException
	{
		JsonElement json = fetchJson(url, ttl, forceRefresh);
		if (!json.isJsonArray())
		{
			throw new IOException("Wise Old Man returned an unexpected response");
		}
		JsonArray array = json.getAsJsonArray();
		if (array.size() > MAX_ARRAY_ENTRIES)
		{
			throw new IOException("Wise Old Man returned too many group members");
		}
		return array;
	}

	private JsonObject fetchJsonObject(HttpUrl url, Duration ttl, boolean forceRefresh) throws IOException
	{
		JsonElement json = fetchJson(url, ttl, forceRefresh);
		if (!json.isJsonObject())
		{
			throw new IOException("Wise Old Man returned an unexpected response");
		}
		return json.getAsJsonObject();
	}

	private JsonElement fetchJson(HttpUrl url, Duration ttl, boolean forceRefresh) throws IOException
	{
		String cacheKey = url.toString();
		if (!forceRefresh)
		{
			JsonElement cached = cached(cacheKey, ttl);
			if (cached != null)
			{
				return cached;
			}
		}

		Object requestLock = requestLocks[(cacheKey.hashCode() & Integer.MAX_VALUE) % requestLocks.length];
		synchronized (requestLock)
		{
			if (!forceRefresh)
			{
				JsonElement cached = cached(cacheKey, ttl);
				if (cached != null)
				{
					return cached;
				}
			}

			requestGate.acquire();
			Request request = new Request.Builder()
				.url(url)
				.header("Accept", "application/json")
				.header("User-Agent", USER_AGENT)
				.build();
			try (Response response = okHttpClient.newCall(request).execute())
			{
				if (response.code() == 429)
				{
					throw requestGate.backOff(response.header("Retry-After"));
				}
				if (!response.isSuccessful() || response.body() == null)
				{
					throw new IOException("Wise Old Man request failed (HTTP " + response.code() + ")");
				}

				ResponseBody body = response.body();
				long declaredLength = body.contentLength();
				if (declaredLength > MAX_RESPONSE_BYTES)
				{
					throw new IOException("Wise Old Man response is too large");
				}
				BufferedSource source = body.source();
				source.request(MAX_RESPONSE_BYTES + 1L);
				if (source.getBuffer().size() > MAX_RESPONSE_BYTES)
				{
					throw new IOException("Wise Old Man response is too large");
				}

				final JsonElement json;
				try
				{
					json = gson.fromJson(source.readUtf8(), JsonElement.class);
				}
				catch (JsonParseException ex)
				{
					throw new IOException("Wise Old Man returned invalid JSON", ex);
				}
				if (json == null || json.isJsonNull())
				{
					throw new IOException("Wise Old Man returned an empty response");
				}
				responseCache.put(cacheKey, new CachedJson(json, Instant.now()));
				return json;
			}
		}
	}

	private static Object[] createRequestLocks()
	{
		Object[] locks = new Object[REQUEST_LOCK_STRIPES];
		for (int index = 0; index < locks.length; index++)
		{
			locks[index] = new Object();
		}
		return locks;
	}

	private JsonElement cached(String cacheKey, Duration ttl)
	{
		synchronized (responseCache)
		{
			CachedJson cached = responseCache.get(cacheKey);
			if (cached == null)
			{
				return null;
			}
			if (Duration.between(cached.storedAt, Instant.now()).compareTo(ttl) >= 0)
			{
				responseCache.remove(cacheKey);
				return null;
			}
			return cached.json;
		}
	}

	static Map<String, ClanProgressGains> parseGroupGains(JsonArray root)
	{
		if (root == null)
		{
			return Collections.emptyMap();
		}

		Map<String, ClanProgressGains> gainsByPlayer = new LinkedHashMap<>();
		for (JsonElement element : root)
		{
			if (!element.isJsonObject())
			{
				continue;
			}

			JsonObject entry = element.getAsJsonObject();
			JsonObject player = entry.has("player") && entry.get("player").isJsonObject()
				? entry.getAsJsonObject("player") : null;
			JsonArray data = entry.has("data") && entry.get("data").isJsonArray()
				? entry.getAsJsonArray("data") : null;
			String name = playerName(player);
			if (name.isEmpty() || data == null)
			{
				continue;
			}

			long xp = 0L;
			long collections = 0L;
			Map<String, Long> bossKc = new LinkedHashMap<>();
			for (JsonElement metricElement : data)
			{
				if (!metricElement.isJsonObject())
				{
					continue;
				}
				JsonObject metricData = metricElement.getAsJsonObject();
				String metric = stringValue(metricData, "metric");
				long gained = Math.max(0L, longValue(metricData, "gained"));
				if ("overall".equals(metric))
				{
					xp = gained;
				}
				else if ("collections_logged".equals(metric))
				{
					collections = gained;
				}
				else
				{
					String bossName = BossKcRegistry.knownDisplayName(metric);
					if (bossName != null && gained > 0L)
					{
						bossKc.merge(bossName, gained, ClanProgressGains::saturatedAdd);
					}
				}
			}
			gainsByPlayer.put(name, new ClanProgressGains(xp, collections, bossKc));
		}
		return gainsByPlayer;
	}

	static Map<String, ClanProgressGains> parseGroupAllTime(JsonArray root)
	{
		if (root == null)
		{
			return Collections.emptyMap();
		}

		Map<String, ClanProgressGains> totalsByPlayer = new LinkedHashMap<>();
		for (JsonElement element : root)
		{
			if (!element.isJsonObject())
			{
				continue;
			}

			JsonObject entry = element.getAsJsonObject();
			JsonObject player = object(entry, "player");
			String name = playerName(player);
			JsonObject snapshot = object(entry, "data");
			JsonObject snapshotData = object(snapshot, "data");
			JsonObject skills = object(snapshotData, "skills");
			JsonObject bosses = object(snapshotData, "bosses");
			JsonObject activities = object(snapshotData, "activities");
			if (name.isEmpty() || snapshotData == null)
			{
				continue;
			}

			JsonObject overall = object(skills, "overall");
			long xp = Math.max(0L, longValue(overall, "experience"));
			if (xp == 0L)
			{
				xp = Math.max(0L, longValue(player, "exp"));
			}

			JsonObject collections = object(activities, "collections_logged");
			long collectionCount = Math.max(0L, longValue(collections, "score"));
			Map<String, Long> bossKc = new LinkedHashMap<>();
			if (bosses != null)
			{
				for (Map.Entry<String, JsonElement> boss : bosses.entrySet())
				{
					if (boss.getValue().isJsonObject())
					{
						long kills = Math.max(0L, longValue(boss.getValue().getAsJsonObject(), "kills"));
						if (kills > 0L)
						{
							bossKc.put(BossKcRegistry.displayName(boss.getKey()), kills);
						}
					}
				}
			}
			totalsByPlayer.put(name, new ClanProgressGains(xp, collectionCount, bossKc));
		}
		return totalsByPlayer;
	}

	static java.util.List<MonthlyLeagueParticipant> parseMonthlyLeagueGains(JsonArray root)
	{
		if (root == null)
		{
			return Collections.emptyList();
		}

		java.util.List<MonthlyLeagueParticipant> participants = new ArrayList<>();
		for (JsonElement element : root)
		{
			if (!element.isJsonObject())
			{
				continue;
			}
			JsonObject entry = element.getAsJsonObject();
			JsonObject player = object(entry, "player");
			JsonArray data = entry.has("data") && entry.get("data").isJsonArray()
				? entry.getAsJsonArray("data") : null;
			String name = playerName(player);
			if (name.isEmpty() || data == null)
			{
				continue;
			}

			double ehp = 0.0;
			double ehb = 0.0;
			long collections = 0L;
			for (JsonElement metricElement : data)
			{
				if (!metricElement.isJsonObject())
				{
					continue;
				}
				JsonObject metricData = metricElement.getAsJsonObject();
				String metric = stringValue(metricData, "metric");
				if ("ehp".equals(metric))
				{
					ehp = Math.max(0.0, doubleValue(metricData, "gained"));
				}
				else if ("ehb".equals(metric))
				{
					ehb = Math.max(0.0, doubleValue(metricData, "gained"));
				}
				else if ("collections_logged".equals(metric))
				{
					collections = Math.max(0L, longValue(metricData, "gained"));
				}
			}

			participants.add(new MonthlyLeagueParticipant(
				longValue(player, "id"),
				name,
				stringValue(player, "type"),
				ehp,
				ehb,
				collections,
				instantValue(entry, "startDate"),
				instantValue(entry, "endDate")));
		}
		return participants;
	}

	static java.util.List<MonthlyLeagueMembership> parseGroupMemberships(JsonObject root)
	{
		if (root == null || !root.has("memberships") || !root.get("memberships").isJsonArray())
		{
			return Collections.emptyList();
		}

		java.util.List<MonthlyLeagueMembership> memberships = new ArrayList<>();
		for (JsonElement element : root.getAsJsonArray("memberships"))
		{
			if (!element.isJsonObject())
			{
				continue;
			}
			JsonObject membership = element.getAsJsonObject();
			JsonObject player = object(membership, "player");
			String name = playerName(player);
			long playerId = Math.max(0L, longValue(membership, "playerId"));
			if (playerId == 0L)
			{
				playerId = Math.max(0L, longValue(player, "id"));
			}
			if (name.isEmpty())
			{
				continue;
			}
			Instant clientJoinedAt = instantValue(membership, "clientSyncJoinedAt");
			Instant joinedAt = clientJoinedAt == null
				? instantValue(membership, "createdAt") : clientJoinedAt;
			memberships.add(new MonthlyLeagueMembership(
				playerId,
				name,
				stringValue(player, "type"),
				joinedAt));
		}
		return memberships;
	}

	private static JsonObject object(JsonObject parent, String key)
	{
		return parent != null && parent.has(key) && parent.get(key).isJsonObject()
			? parent.getAsJsonObject(key) : null;
	}

	private static JsonObject objectAt(JsonObject root, String first, String second)
	{
		if (root == null || !root.has(first) || !root.get(first).isJsonObject())
		{
			return null;
		}
		JsonObject child = root.getAsJsonObject(first);
		if (!child.has(second) || !child.get(second).isJsonObject())
		{
			return null;
		}
		return child.getAsJsonObject(second);
	}

	private static HttpUrl groupUrl(int groupId, String... segments)
	{
		HttpUrl.Builder builder = GROUPS_URL.newBuilder().addPathSegment(String.valueOf(groupId));
		for (String segment : segments)
		{
			builder.addPathSegment(segment);
		}
		return builder.build();
	}

	private static String playerName(JsonObject player)
	{
		String displayName = stringValue(player, "displayName");
		String candidate = displayName.isEmpty() ? stringValue(player, "username") : displayName;
		try
		{
			return OsrsPlayerName.requireValid(candidate);
		}
		catch (IllegalArgumentException ex)
		{
			return "";
		}
	}

	private static String stringValue(JsonObject object, String key)
	{
		if (object == null || !object.has(key) || object.get(key).isJsonNull())
		{
			return "";
		}
		JsonElement value = object.get(key);
		if (!value.isJsonPrimitive())
		{
			return "";
		}
		try
		{
			return value.getAsString().trim();
		}
		catch (RuntimeException ex)
		{
			return "";
		}
	}

	private static long longValue(JsonObject object, String key)
	{
		Long value = optionalLong(object, key);
		return value == null ? 0L : value;
	}

	private static Long optionalLong(JsonObject object, String key)
	{
		if (object == null || !object.has(key) || object.get(key).isJsonNull()
			|| !object.get(key).isJsonPrimitive())
		{
			return null;
		}
		try
		{
			return object.get(key).getAsLong();
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	private static double doubleValue(JsonObject object, String key)
	{
		if (object == null || !object.has(key) || object.get(key).isJsonNull())
		{
			return 0.0;
		}
		JsonElement value = object.get(key);
		if (!value.isJsonPrimitive())
		{
			return 0.0;
		}
		try
		{
			double parsed = value.getAsDouble();
			return Double.isFinite(parsed) ? parsed : 0.0;
		}
		catch (RuntimeException ex)
		{
			return 0.0;
		}
	}

	private static Instant instantValue(JsonObject object, String key)
	{
		String value = stringValue(object, key);
		if (value.isEmpty())
		{
			return null;
		}
		try
		{
			return Instant.parse(value);
		}
		catch (DateTimeParseException ex)
		{
			return null;
		}
	}

	private static Map<String, HiscoreSkill> buildSkillMap()
	{
		Map<String, HiscoreSkill> map = new HashMap<>();
		map.put("overall", HiscoreSkill.OVERALL);
		map.put("attack", HiscoreSkill.ATTACK);
		map.put("defence", HiscoreSkill.DEFENCE);
		map.put("strength", HiscoreSkill.STRENGTH);
		map.put("hitpoints", HiscoreSkill.HITPOINTS);
		map.put("ranged", HiscoreSkill.RANGED);
		map.put("prayer", HiscoreSkill.PRAYER);
		map.put("magic", HiscoreSkill.MAGIC);
		map.put("cooking", HiscoreSkill.COOKING);
		map.put("woodcutting", HiscoreSkill.WOODCUTTING);
		map.put("fletching", HiscoreSkill.FLETCHING);
		map.put("fishing", HiscoreSkill.FISHING);
		map.put("firemaking", HiscoreSkill.FIREMAKING);
		map.put("crafting", HiscoreSkill.CRAFTING);
		map.put("smithing", HiscoreSkill.SMITHING);
		map.put("mining", HiscoreSkill.MINING);
		map.put("herblore", HiscoreSkill.HERBLORE);
		map.put("agility", HiscoreSkill.AGILITY);
		map.put("thieving", HiscoreSkill.THIEVING);
		map.put("slayer", HiscoreSkill.SLAYER);
		map.put("farming", HiscoreSkill.FARMING);
		map.put("runecrafting", HiscoreSkill.RUNECRAFT);
		map.put("runecraft", HiscoreSkill.RUNECRAFT);
		map.put("hunter", HiscoreSkill.HUNTER);
		map.put("construction", HiscoreSkill.CONSTRUCTION);
		map.put("sailing", HiscoreSkill.SAILING);
		return map;
	}

	private static final class CachedJson
	{
		private final JsonElement json;
		private final Instant storedAt;

		private CachedJson(JsonElement json, Instant storedAt)
		{
			this.json = json;
			this.storedAt = storedAt;
		}
	}

}
