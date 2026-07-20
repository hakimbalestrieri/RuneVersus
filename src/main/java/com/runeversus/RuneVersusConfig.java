package com.runeversus;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.hiscore.HiscoreEndpoint;

@ConfigGroup("runeversus")
public interface RuneVersusConfig extends Config
{
	@ConfigSection(
		name = "Data",
		description = "Data sources used for comparisons.",
		position = 0
	)
	String dataSection = "data";

	@ConfigItem(
		keyName = "hiscoreEndpoint",
		name = "Hiscore type",
		description = "Official OSRS hiscore endpoint used for typed player lookups.",
		section = dataSection,
		position = 0
	)
	default HiscoreEndpoint hiscoreEndpoint()
	{
		return HiscoreEndpoint.NORMAL;
	}

	@ConfigItem(
		keyName = "wiseOldManGains",
		name = "Wise Old Man gains",
		description = "Fetch optional 24h/week/month XP gains from Wise Old Man. This sends compared RSNs to a third-party service.",
		section = dataSection,
		position = 1
	)
	default boolean wiseOldManGains()
	{
		return false;
	}

	@ConfigItem(
		keyName = "localOptInSync",
		name = "Local opt-in sync",
		description = "Read optional PB and detailed collection-log data from .runelite/rune-versus/sync/*.properties.",
		section = dataSection,
		position = 2
	)
	default boolean localOptInSync()
	{
		return false;
	}

	@ConfigSection(
		name = "Cards",
		description = "Duel card export settings.",
		position = 10
	)
	String cardSection = "cards";

	@ConfigItem(
		keyName = "autoExportCard",
		name = "Auto-export card",
		description = "Automatically export a PNG duel card after every successful comparison.",
		section = cardSection,
		position = 0
	)
	default boolean autoExportCard()
	{
		return true;
	}

	@ConfigItem(
		keyName = "copyPathToClipboard",
		name = "Copy card path",
		description = "Copy the generated card path to the clipboard after export.",
		section = cardSection,
		position = 1
	)
	default boolean copyPathToClipboard()
	{
		return true;
	}

	@ConfigItem(
		keyName = "partyAnnounceCards",
		name = "Party announce",
		description = "Announce generated duel-card summaries to RuneLite Party members.",
		section = cardSection,
		position = 2
	)
	default boolean partyAnnounceCards()
	{
		return true;
	}

	@ConfigItem(
		keyName = "cardTheme",
		name = "Card theme",
		description = "Visual theme used for exported duel cards.",
		section = cardSection,
		position = 3
	)
	default RuneVersusCardTheme cardTheme()
	{
		return RuneVersusCardTheme.AUTO;
	}

	@ConfigItem(
		keyName = "verdictStyle",
		name = "Verdict style",
		description = "Tone used for duel verdicts and share text.",
		section = cardSection,
		position = 4
	)
	default VerdictStyle verdictStyle()
	{
		return VerdictStyle.FUN;
	}

	@ConfigSection(
		name = "Social",
		description = "Party, clan, chat, and right-click comparison settings.",
		position = 20
	)
	String socialSection = "social";

	@ConfigItem(
		keyName = "playerMenuOptions",
		name = "Player menu options",
		description = "Add RuneVersus Compare/Set A/Set B options to player right-click menus.",
		section = socialSection,
		position = 0
	)
	default boolean playerMenuOptions()
	{
		return true;
	}

	@ConfigItem(
		keyName = "maxRosterPlayers",
		name = "Max roster players",
		description = "Maximum Party or clan players fetched for leaderboards, recaps, and fight night.",
		section = socialSection,
		position = 1
	)
	default int maxRosterPlayers()
	{
		return 12;
	}

	@ConfigItem(
		keyName = "watchlistRivals",
		name = "Watchlist rivals",
		description = "Comma-separated RSNs to compare against you for snipe callouts.",
		section = socialSection,
		position = 2
	)
	default String watchlistRivals()
	{
		return "";
	}
}
