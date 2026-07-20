package com.runeversus;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.hiscore.HiscoreEndpoint;

@ConfigGroup("runeversus")
public interface RuneVersusConfig extends Config
{
	@ConfigSection(
		name = "Comparison",
		description = "How RuneVersus looks up and scores players.",
		position = 0
	)
	String comparisonSection = "comparison";

	@ConfigItem(
		keyName = "hiscoreEndpoint",
		name = "Account type",
		description = "Official hiscore table used for lookups, such as Normal or Ironman.",
		section = comparisonSection,
		position = 0
	)
	default HiscoreEndpoint hiscoreEndpoint()
	{
		return HiscoreEndpoint.NORMAL;
	}

	@ConfigItem(
		keyName = "wiseOldManGains",
		name = "Recent XP",
		description = "Adds 24h, week, and month XP using Wise Old Man. Compared RSNs are sent to Wise Old Man.",
		section = comparisonSection,
		position = 1
	)
	default boolean wiseOldManGains()
	{
		return false;
	}

	@ConfigItem(
		keyName = "localOptInSync",
		name = "Private PB/clog import",
		description = "Reads optional local PB and collection-log files from .runelite/rune-versus/sync.",
		section = comparisonSection,
		position = 2
	)
	default boolean localOptInSync()
	{
		return false;
	}

	@ConfigSection(
		name = "Cards",
		description = "PNG export and card style.",
		position = 10
	)
	String cardSection = "cards";

	@ConfigItem(
		keyName = "autoExportCard",
		name = "Auto-save PNG",
		description = "Saves a card after each comparison.",
		section = cardSection,
		position = 0
	)
	default boolean autoExportCard()
	{
		return true;
	}

	@ConfigItem(
		keyName = "copyPathToClipboard",
		name = "Copy PNG path",
		description = "Copies the saved card path after export.",
		section = cardSection,
		position = 1
	)
	default boolean copyPathToClipboard()
	{
		return true;
	}

	@ConfigItem(
		keyName = "cardTheme",
		name = "Card style",
		description = "Visual style for exported duel and recap cards.",
		section = cardSection,
		position = 2
	)
	default RuneVersusCardTheme cardTheme()
	{
		return RuneVersusCardTheme.AUTO;
	}

	@ConfigItem(
		keyName = "verdictStyle",
		name = "Verdict tone",
		description = "Tone used for card verdicts and share text.",
		section = cardSection,
		position = 3
	)
	default VerdictStyle verdictStyle()
	{
		return VerdictStyle.FUN;
	}

	@ConfigSection(
		name = "Social",
		description = "Party, clan, chat, and right-click features.",
		position = 20
	)
	String socialSection = "social";

	@ConfigItem(
		keyName = "playerMenuOptions",
		name = "Right-click players",
		description = "Adds VS Compare, VS Set A, and VS Set B to player right-click menus.",
		section = socialSection,
		position = 0
	)
	default boolean playerMenuOptions()
	{
		return true;
	}

	@ConfigItem(
		keyName = "partyAnnounceCards",
		name = "Party announce",
		description = "Shares duel-card summaries with RuneLite Party members.",
		section = socialSection,
		position = 1
	)
	default boolean partyAnnounceCards()
	{
		return true;
	}

	@Range(
		min = 2,
		max = 50
	)
	@ConfigItem(
		keyName = "maxRosterPlayers",
		name = "Roster limit",
		description = "Maximum Party or clan players scanned for leaderboards, recaps, and Fight Night.",
		section = socialSection,
		position = 2
	)
	default int maxRosterPlayers()
	{
		return 12;
	}

	@ConfigItem(
		keyName = "watchlistRivals",
		name = "Watchlist",
		description = "Comma-separated rivals used by Watchlist Snipes.",
		section = socialSection,
		position = 3
	)
	default String watchlistRivals()
	{
		return "";
	}
}
