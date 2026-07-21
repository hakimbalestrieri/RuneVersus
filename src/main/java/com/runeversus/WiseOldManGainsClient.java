package com.runeversus;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
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

@Singleton
public class WiseOldManGainsClient
{
	private static final String BASE_URL = "https://api.wiseoldman.net/v2/players/";
	private static final String GROUPS_BASE_URL = "https://api.wiseoldman.net/v2/groups/";
	private static final Map<String, HiscoreSkill> SKILL_BY_WOM_METRIC = buildSkillMap();

	private final OkHttpClient okHttpClient;
	private final Gson gson;

	@Inject
	public WiseOldManGainsClient(OkHttpClient okHttpClient, Gson gson)
	{
		this.okHttpClient = okHttpClient;
		this.gson = gson;
	}

	public Map<HiscoreSkill, Long> getSkillExperienceGains(String username, String period) throws IOException
	{
		HttpUrl url = HttpUrl.parse(BASE_URL + urlName(username) + "/gained");
		if (url == null)
		{
			throw new IOException("Invalid Wise Old Man URL");
		}

		Request request = new Request.Builder()
			.url(url.newBuilder().addQueryParameter("period", period).build())
			.header("Accept", "application/json")
			.build();

		try (Response response = okHttpClient.newCall(request).execute())
		{
			if (!response.isSuccessful() || response.body() == null)
			{
				return new EnumMap<>(HiscoreSkill.class);
			}

			JsonObject root = gson.fromJson(response.body().charStream(), JsonObject.class);
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

				JsonObject experience = entry.getValue().getAsJsonObject().getAsJsonObject("experience");
				if (experience != null && experience.has("gained") && !experience.get("gained").isJsonNull())
				{
					gains.put(skill, experience.get("gained").getAsLong());
				}
			}
			return gains;
		}
	}

	public Map<String, ClanProgressGains> getGroupGains(int groupId, GainPeriod period) throws IOException
	{
		if (groupId <= 0)
		{
			throw new IOException("Wise Old Man group ID must be greater than zero");
		}

		if (period == GainPeriod.ALL_TIME)
		{
			return getGroupAllTime(groupId);
		}

		HttpUrl url = HttpUrl.parse(GROUPS_BASE_URL + groupId + "/bulk-gained");
		if (url == null)
		{
			throw new IOException("Invalid Wise Old Man group URL");
		}

		Request request = new Request.Builder()
			.url(url.newBuilder().addQueryParameter("period", period.getApiValue()).build())
			.header("Accept", "application/json")
			.build();

		try (Response response = okHttpClient.newCall(request).execute())
		{
			if (!response.isSuccessful() || response.body() == null)
			{
				throw new IOException("Wise Old Man group request failed (HTTP " + response.code() + ")");
			}

			JsonArray root = gson.fromJson(response.body().charStream(), JsonArray.class);
			return parseGroupGains(root);
		}
	}

	public java.util.List<MonthlyLeagueParticipant> getMonthlyLeagueGains(
		int groupId,
		Instant startDate,
		Instant endDate) throws IOException
	{
		if (groupId <= 0)
		{
			throw new IOException("Wise Old Man group ID must be greater than zero");
		}
		if (startDate == null || endDate == null || !endDate.isAfter(startDate))
		{
			throw new IOException("Monthly league date range is invalid");
		}

		HttpUrl url = HttpUrl.parse(GROUPS_BASE_URL + groupId + "/bulk-gained");
		if (url == null)
		{
			throw new IOException("Invalid Wise Old Man group URL");
		}

		Request request = new Request.Builder()
			.url(url.newBuilder()
				.addQueryParameter("startDate", startDate.toString())
				.addQueryParameter("endDate", endDate.toString())
				.build())
			.header("Accept", "application/json")
			.build();

		try (Response response = okHttpClient.newCall(request).execute())
		{
			if (!response.isSuccessful() || response.body() == null)
			{
				throw new IOException("Wise Old Man monthly league request failed (HTTP " + response.code() + ")");
			}
			JsonArray root = gson.fromJson(response.body().charStream(), JsonArray.class);
			return parseMonthlyLeagueGains(root);
		}
	}

	private Map<String, ClanProgressGains> getGroupAllTime(int groupId) throws IOException
	{
		HttpUrl url = HttpUrl.parse(GROUPS_BASE_URL + groupId + "/bulk-hiscores");
		if (url == null)
		{
			throw new IOException("Invalid Wise Old Man group URL");
		}

		Request request = new Request.Builder()
			.url(url)
			.header("Accept", "application/json")
			.build();
		try (Response response = okHttpClient.newCall(request).execute())
		{
			if (!response.isSuccessful() || response.body() == null)
			{
				throw new IOException("Wise Old Man group hiscores request failed (HTTP " + response.code() + ")");
			}
			JsonArray root = gson.fromJson(response.body().charStream(), JsonArray.class);
			return parseGroupAllTime(root);
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
						bossKc.merge(bossName, gained, Long::sum);
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

	private static String urlName(String username)
	{
		return username == null ? "" : username.trim().replace(" ", "%20");
	}

	private static String playerName(JsonObject player)
	{
		String displayName = stringValue(player, "displayName");
		return displayName.isEmpty() ? stringValue(player, "username") : displayName;
	}

	private static String stringValue(JsonObject object, String key)
	{
		if (object == null || !object.has(key) || object.get(key).isJsonNull())
		{
			return "";
		}
		return object.get(key).getAsString().trim();
	}

	private static long longValue(JsonObject object, String key)
	{
		if (object == null || !object.has(key) || object.get(key).isJsonNull())
		{
			return 0L;
		}
		try
		{
			return object.get(key).getAsLong();
		}
		catch (NumberFormatException ex)
		{
			return 0L;
		}
	}

	private static double doubleValue(JsonObject object, String key)
	{
		if (object == null || !object.has(key) || object.get(key).isJsonNull())
		{
			return 0.0;
		}
		try
		{
			return object.get(key).getAsDouble();
		}
		catch (NumberFormatException ex)
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

}
