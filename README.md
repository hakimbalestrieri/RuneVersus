# RuneVersus

RuneVersus turns OSRS account comparison into shareable duel cards, clan recaps,
and quick chat results.

Compare yourself, two typed RSNs, Party members, clanmates, or players mentioned
through chat commands. RuneVersus scores public hiscore data, highlights close
snipes, and exports polished 16:9 PNG cards for Discord, Twitter, clan channels,
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
- Show a closest-snipe callout, such as the KC or XP needed to steal a category.
- Show a biggest-flex highlight for the largest account gap.
- Pick players directly from your RuneLite Party.
- Pick online or full members from your clan channel.
- Right-click visible players to run `VS Compare`, `VS Set A`, or `VS Set B`.
- Export a duel-card PNG after comparisons.
- Export a clan recap PNG for social posts or Discord.
- Copy the saved PNG path to the clipboard after export.
- Choose card styles: Auto, PvM, Skilling, Ironman, Clan War, or Underdog.
- Choose verdict tone: Serious, Fun, or Savage.
- Announce duel summaries to RuneLite Party members.
- Keep a watchlist of rivals and print quick snipe summaries against your account.
- Import optional local PB and detailed collection-log data when both players opt in.

## Social Modes

RuneVersus includes social tools from the side panel:

- Party Leaderboard: scans Party members and prints top PvM, skilling,
  collections, and current-form results.
- Clan Leaderboard: scans online clan members and prints the same leaderboard
  summary.
- Export Clan Recap: creates a 16:9 recap card with PvM King, Skilling King,
  Collection Lord, and current-form tables.
- Fight Night: picks two online clan or Party players at random, compares them,
  and exports a matchup card.
- Watchlist Snipes: compares your account against configured rivals and prints
  share-friendly snipe lines.

## Settings

The config is intentionally grouped into three sections:

- Comparison: account type, optional recent XP source, and optional private data
  import.
- Cards: PNG export, clipboard copy, card style, and verdict tone.
- Social: right-click player menus, Party announcements, roster scan limit, and
  watchlist rivals.

The default settings are designed for quick testing: cards auto-save, PNG paths
are copied, right-click player menus are enabled, and Party announcements are on.

## Data And Privacy

- Boss KC, all-time skill XP, activities, clues, and public collection-log count
  come from the official OSRS hiscores.
- Recent 24h, week, and month XP requires Wise Old Man. Enabling this sends the
  compared RSNs to Wise Old Man.
- Full collection log and boss personal best times are not public for arbitrary
  players.
- Local opt-in sync reads private PB and collection data from your machine only.
  It does not upload that data.
- PB and detailed collection-log metrics are only scored when both compared
  players have provided local opt-in data.

Example local opt-in file:

```properties
# ~/.runelite/rune-versus/sync/My_RSN.properties
collection.items=1242
pb.Vorkath=73
pb.Tombs_of_Amascut=1218
personalBest.Zulrah=48
```

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
