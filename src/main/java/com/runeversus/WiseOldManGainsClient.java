package com.runeversus;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
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
