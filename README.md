# RuneVersus

RuneVersus turns OSRS account comparison into a shareable fight card.

Compare yourself, two typed RSNs, Party members, clanmates, or players from any
visible chat command across official hiscore categories, then export a polished
16:9 PNG for Discord, Twitter, clan recaps, and stream overlays.

## Current MVP

- Compare any two RSNs via official OSRS hiscores.
- `!vs <player>` compares the command sender against that player.
- `!vs <player1> <player2>` compares the supplied players.
- Incoming `!vs` commands from public, private, friends chat, clan, guest clan,
  GIM, or Party chat render as text-only results on your client.
- Score skill XP, boss KC, clue/minigame activity, and collection-log count.
- Show closest snipe callouts, e.g. the exact KC/XP needed to steal a category.
- Pick players from your RuneLite Party.
- Pick online or full members from your clan channel.
- Right-click any player to compare against yourself or set them as Player A/B.
- Generate a premium duel card PNG under `.runelite/rune-versus/cards`.
- Export themed cards: Auto, PvM, Skilling, Ironman, Clan War, or Underdog.
- Choose verdict tone: serious, fun, or savage.
- Optionally copy the exported card path to the clipboard.
- Optionally fetch XP gains from Wise Old Man for day/week/month form checks.

## Social Modes

- Party Leaderboard: local text recap of top Party members by boss KC, XP,
  collections, and current form.
- Clan Leaderboard: same recap for online clan members.
- Export Clan Recap: creates a shareable 16:9 clan recap card.
- Fight Night: picks two online clan or Party players at random, compares them,
  and exports a matchup card.
- Watchlist Snipes: compares you against comma-separated rivals from config and
  prints share-friendly snipe lines.

## Data Notes

- Boss KC and all-time XP come from the official OSRS hiscores.
- Day/week/month XP gains require the optional Wise Old Man integration.
- Local opt-in sync can add non-public PB and detailed collection-log totals
  from `.runelite/rune-versus/sync/<RSN>.properties`. Example:

```properties
collection.items=1242
pb.Vorkath=73
pb.Tombs_of_Amascut=1218
personalBest.Zulrah=48
```

- Full collection log and boss PBs are not public for arbitrary players. The
  plugin compares public `Collections Logged` by default and only scores local
  opt-in PB/collection data when both players have provided it.

## Development

```bash
./gradlew build
./gradlew run
```

The project follows the RuneLite Plugin Hub standard build style and avoids
non-RuneLite third-party dependencies.

For Plugin Hub submission steps, see [PLUGIN_HUB_SUBMISSION.md](PLUGIN_HUB_SUBMISSION.md).
