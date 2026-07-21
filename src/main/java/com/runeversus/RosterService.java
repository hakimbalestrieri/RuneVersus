package com.runeversus;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.client.util.Text;

@Singleton
public class RosterService
{
	private final Client client;

	@Inject
	public RosterService(Client client)
	{
		this.client = client;
	}

	public String getLocalPlayerName()
	{
		Player local = client.getLocalPlayer();
		return local == null || local.getName() == null ? "" : clean(local.getName());
	}

	public List<String> getOnlineClanMembers()
	{
		Set<String> names = new LinkedHashSet<>();
		ClanChannel channel = client.getClanChannel();
		if (channel != null)
		{
			for (ClanChannelMember member : channel.getMembers())
			{
				if (member.getName() != null)
				{
					addCleanName(names, member.getName());
				}
			}
		}
		return sorted(names);
	}

	public List<String> getAllClanMembers()
	{
		Set<String> names = new LinkedHashSet<>();
		ClanSettings settings = client.getClanSettings();
		if (settings != null)
		{
			for (ClanMember member : settings.getMembers())
			{
				if (member.getName() != null)
				{
					addCleanName(names, member.getName());
				}
			}
		}
		return sorted(names);
	}

	private static List<String> sorted(Set<String> names)
	{
		List<String> out = new ArrayList<>(names);
		out.sort(String.CASE_INSENSITIVE_ORDER);
		return out;
	}

	private static void addCleanName(Set<String> names, String name)
	{
		String cleaned = clean(name);
		if (!cleaned.isEmpty())
		{
			names.add(cleaned);
		}
	}

	private static String clean(String name)
	{
		return Text.removeTags(name).trim();
	}
}
