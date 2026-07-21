package com.runeversus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.util.Text;

@Singleton
public class RosterService
{
	private final Client client;
	private final PartyService partyService;

	@Inject
	public RosterService(Client client, PartyService partyService)
	{
		this.client = client;
		this.partyService = partyService;
	}

	public String getLocalPlayerName()
	{
		Player local = client.getLocalPlayer();
		return local == null || local.getName() == null ? "" : clean(local.getName());
	}

	public List<String> getPartyMembers()
	{
		Set<String> names = new LinkedHashSet<>();
		for (PartyMember member : partyService.getMembers())
		{
			if (member.isLoggedIn() && member.getDisplayName() != null && !"<unknown>".equals(member.getDisplayName()))
			{
				names.add(clean(member.getDisplayName()));
			}
		}
		return sorted(names);
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
					names.add(clean(member.getName()));
				}
			}
		}
		return sorted(names);
	}

	public List<String> getFriendsChatMembers()
	{
		Set<String> names = new LinkedHashSet<>();
		FriendsChatManager manager = client.getFriendsChatManager();
		if (manager != null && manager.getMembers() != null)
		{
			for (FriendsChatMember member : manager.getMembers())
			{
				if (member != null && member.getName() != null)
				{
					names.add(clean(member.getName()));
				}
			}
		}
		return sorted(names);
	}

	public String getFriendsChatName()
	{
		FriendsChatManager manager = client.getFriendsChatManager();
		return manager == null || manager.getName() == null ? "" : clean(manager.getName());
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
					names.add(clean(member.getName()));
				}
			}
		}
		return sorted(names);
	}

	private static List<String> sorted(Set<String> names)
	{
		List<String> out = new ArrayList<>(names);
		out.sort(Comparator.comparing(String::toLowerCase));
		return out;
	}

	private static String clean(String name)
	{
		return Text.removeTags(name).trim();
	}
}
