# RuneVersus

RuneVersus turns OSRS account comparison into shareable duel cards, clan recaps,
and quick chat results.

Compare yourself, two typed RSNs, Party members, clanmates, or players mentioned
through chat commands. RuneVersus scores public hiscore data, highlights close
categories, and exports polished 16:9 PNG cards for Discord, Twitter, clan channels,
and stream overlays.

## Features

- Compare any two OSRS players from the RuneVersus side panel.
- Use `!vs <player>` to compare the command sender against another player.
- Use `!vs <player1> <player2>` to compare two named players.
- Use commas or quotes for names with spaces, e.g. `!vs Zezima, Lynx Titan`.
- Display incoming `!vs` commands from public, private, friends chat, clan,
  guest clan, GIM, or Party chat as local text results.
- Compare skill XP, boss KC, clue and activity scores, and public collection-log
  count from official OSRS hiscores.
- Optionally add 24h, week, and month XP gains through Wise Old Man.
- Show the closest category, such as the KC or XP needed to take the lead.
- Show a biggest-flex highlight for the largest account gap.
- Pick players directly from your RuneLite Party.
- Pick online or full members from your clan channel.
- Right-click visible players to run `VS Compare`; optional clan-list shortcuts provide `VS Set A` and `VS Set B`.
- Export a duel-card PNG after comparisons.
- Rank the clan's XP, collection-log, and summed boss-KC values over 24 hours,
  one week, one month, one year, and all-time.
- Export the 15 clan progress leaders as one shareable 16:9 PNG card.
- Copy the saved PNG path to the clipboard after export.
- Choose card styles: Auto, PvM, Skilling, Ironman, Clan War, or Underdog.
- Choose verdict tone: Serious, Fun, or Savage.
- Optionally open the detailed two-player Versus view in the RuneLite side panel.
- Announce duel summaries to RuneLite Party members.
- Import optional local PB and detailed collection-log data when both players opt in.

## Social Modes

RuneVersus includes social tools from the side panel:

- Clan Member Comparison: opens a large, independent window containing every
  member of the configured Wise Old Man group. Switch between 24h, week, month,
  year, and all-time totals;
  rank by XP, CLogs, or total boss KC; search by name; and review clan totals,
  active-member counts, average active XP, and the three period leaders.
- Export Progress Card: creates one 16:9 card containing all 15 period/category
  leaders and exposes it through the clickable `Open saved card` link.

## Settings

The config is intentionally grouped into three simple sections:

- Player data: account type, optional recent XP source, and optional private data
  import. `Open comparison window` is on by default; disable it to keep the
  full Versus side-panel view with scores, highlights, filters, and every compared metric.
- Card export: PNG export, clipboard copy, card style, and verdict tone.
- Party & clan: right-click player menus, optional clan `VS Set` shortcuts, Party
  announcements, Wise Old Man group ID, and roster scan limit.

Card export and Party & clan settings are collapsed by default. The side panel
starts with only two player fields and one Compare button; Party, clan, and
social tools are available under More options.

The default settings are designed for quick testing: cards auto-save, PNG paths
are copied, right-click player menus are enabled, and Party announcements are on.

## Data And Privacy

- Boss KC, all-time skill XP, activities, clues, and public collection-log count
  come from the official OSRS hiscores.
- Recent 24h, week, and month XP requires Wise Old Man. Enabling this sends the
  compared RSNs to Wise Old Man.
- Clan Progress requires the numeric Wise Old Man group ID from the group's WOM
  page. RuneVersus makes five bulk read-only requests (day, week, month, year,
  and current bulk hiscores); rankings depend on the snapshots available in
  Wise Old Man.
- Full collection log and boss personal best times are not public for arbitrary
  players.
- Local opt-in sync reads Combat Achievement tiers, private PB, and collection
  data from your machine only.
  It does not upload that data.
- PB and detailed collection-log metrics are only scored when both compared
  players have provided local opt-in data.

Example local opt-in file:

```properties
# ~/.runelite/rune-versus/sync/My_RSN.properties
collection.items=1242
combatAchievements.tier=Master
pb.Vorkath=73
pb.Tombs_of_Amascut=1218
personalBest.Zulrah=48
```

Compact chat comparisons use three equally weighted categories: total skill XP,
Combat Achievement tier, and public collection-log count. Combat Achievement
tiers are not available from public hiscores, so that category displays `n/a`
and is not scored unless both players provide it through local opt-in data.

## Export Location

Cards are saved under:

```text
~/.runelite/rune-versus/cards
```

## Development

Build the plugin:

```bash
./gradlew build
```

Run the development client:

```bash
./gradlew run
```

If you use a Jagex Account, follow the RuneLite developer guide for Jagex
Accounts before logging in to the development client.

The project follows the RuneLite Plugin Hub standard build style and avoids
non-RuneLite third-party runtime dependencies.

For Plugin Hub submission steps, see [PLUGIN_HUB_SUBMISSION.md](PLUGIN_HUB_SUBMISSION.md).
