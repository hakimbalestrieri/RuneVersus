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
		name = "Player data",
		description = "Choose the account type and optional data sources.",
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
		name = "Recent XP details",
		description = "Adds 24h, week, and month XP to cards. Compared names are sent to Wise Old Man.",
		section = comparisonSection,
		position = 1
	)
	default boolean wiseOldManGains()
	{
		return false;
	}

	@ConfigItem(
		keyName = "localOptInSync",
		name = "CA tier & private data",
		description = "Reads optional CA tier, PB, and collection-log files from .runelite/rune-versus/sync.",
		section = comparisonSection,
		position = 2
	)
	default boolean localOptInSync()
	{
		return false;
	}

	@ConfigItem(
		keyName = "openInterfaceOnComparison",
		name = "Open comparison window",
		description = "Opens Player comparison in a separate window. Disable it to use the RuneLite side panel.",
		section = comparisonSection,
		position = 3
	)
	default boolean openInterfaceOnComparison()
	{
		return true;
	}

	@ConfigSection(
		name = "Card export",
		description = "Optional PNG export settings.",
		position = 10,
		closedByDefault = true
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
		name = "Party & clan",
		description = "Optional social and right-click features.",
		position = 20,
		closedByDefault = true
	)
	String socialSection = "social";

	@ConfigItem(
		keyName = "playerMenuOptions",
		name = "Right-click players",
		description = "Adds VS Compare to visible players and clan members.",
		section = socialSection,
		position = 0
	)
	default boolean playerMenuOptions()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clanSetMenuOptions",
		name = "Clan VS Set options",
		description = "Adds VS Set A and VS Set B when right-clicking a clan member.",
		section = socialSection,
		position = 1
	)
	default boolean clanSetMenuOptions()
	{
		return true;
	}

	@ConfigItem(
		keyName = "partyAnnounceCards",
		name = "Party announce",
		description = "Shares duel-card summaries with RuneLite Party members.",
		section = socialSection,
		position = 2
	)
	default boolean partyAnnounceCards()
	{
		return true;
	}

	@ConfigItem(
		keyName = "wiseOldManGroupId",
		name = "WOM group ID",
		description = "Wise Old Man group used for the clan progress leaders (XP, CLogs, and total boss KC).",
		section = socialSection,
		position = 3
	)
	default String wiseOldManGroupId()
	{
		return "";
	}

	@Range(
		min = 2,
		max = 50
	)
	@ConfigItem(
		keyName = "maxRosterPlayers",
		name = "Roster limit",
		description = "Maximum Party, Friend Chat, or clan players scanned for comparisons and leaderboards.",
		section = socialSection,
		position = 4
	)
	default int maxRosterPlayers()
	{
		return 12;
	}
}
