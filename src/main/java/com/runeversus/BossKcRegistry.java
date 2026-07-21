package com.runeversus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;

final class BossKcRegistry
{
	private static final Map<String, String> NAME_BY_METRIC = buildNames();
	private static final List<String> ALL_NAMES = buildAllNames();

	private BossKcRegistry()
	{
	}

	static String knownDisplayName(String metric)
	{
		return NAME_BY_METRIC.get(normalize(metric));
	}

	static String displayName(String metric)
	{
		String known = knownDisplayName(metric);
		return known == null ? humanize(metric) : known;
	}

	static List<String> allNames()
	{
		return ALL_NAMES;
	}

	private static Map<String, String> buildNames()
	{
		Map<String, String> names = new LinkedHashMap<>();
		for (HiscoreSkill skill : HiscoreSkill.values())
		{
			if (skill.getType() == HiscoreSkillType.BOSS)
			{
				names.put(normalize(skill.getName()), skill.getName());
			}
		}
		return Collections.unmodifiableMap(names);
	}

	private static List<String> buildAllNames()
	{
		List<String> names = new ArrayList<>(NAME_BY_METRIC.values());
		names.sort(String.CASE_INSENSITIVE_ORDER);
		return Collections.unmodifiableList(names);
	}

	private static String normalize(String name)
	{
		return name == null ? "" : name.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9]+", "_")
			.replaceAll("^_+|_+$", "");
	}

	private static String humanize(String metric)
	{
		String normalized = normalize(metric);
		if (normalized.isEmpty())
		{
			return "Unknown boss";
		}
		StringBuilder result = new StringBuilder();
		for (String word : normalized.split("_"))
		{
			if (result.length() > 0)
			{
				result.append(' ');
			}
			result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return result.toString();
	}
}
