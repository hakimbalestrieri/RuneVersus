# RuneVersus

RuneVersus turns OSRS account comparison into shareable duel cards, clan recaps,
and quick chat results.

![RuneVersus player comparison](docs/images/player-comparison.png)

Compare yourself, two typed RSNs, clanmates, or players mentioned
through chat commands. RuneVersus scores public hiscore data, highlights close
categories, and exports polished 16:9 PNG cards for Discord, Twitter, clan channels,
and stream overlays.

## Features

- Compare any two OSRS players from the RuneVersus side panel.
- Use `!vs <player>` to compare the command sender against another player.
- Use `!vs <player1> <player2>` to compare two named players.
- Use commas or quotes for names with spaces, e.g. `!vs Zezima, Lynx Titan`.
- Display incoming `!vs` commands from public, private, friends chat, clan,
  guest clan, or GIM as local text results.
- Compare skill XP, boss KC, clue and activity scores, and public collection-log
  count from official OSRS hiscores.
- Optionally add 24h, week, and month XP gains through Wise Old Man.
- Show the closest category, such as the KC or XP needed to take the lead.
- Show a biggest-flex highlight for the largest account gap.
- Pick online or full members from your clan channel.
- Right-click visible players to run `VS Compare`; optional clan-list shortcuts provide `VS Set A` and `VS Set B`.
- Export a duel-card PNG after comparisons.
- Rank the clan's XP, collection-log, and summed boss-KC values over 24 hours,
  one week, one month, one year, and all-time.
- Run an automatic calendar-month clan league using fair EHP and EHB rankings,
  live podiums, twelve browsable seasons, and a separate Collection spotlight.
- Exclude late-tracked members from competitive ranks while keeping their gains
  visible as provisional data.
- Export the 15 clan progress leaders as one shareable 16:9 PNG card.
- Export the live or final monthly podium as an OSRS-style 16:9 PNG card.
- Copy the saved PNG path to the clipboard after export.
- Choose card styles: Auto, PvM, Skilling, Ironman, Clan War, or Underdog.
- Choose verdict tone: Serious, Fun, or Savage.
- Optionally open the detailed two-player Versus view in the RuneLite side panel.
- Import optional local PB and detailed collection-log data when both players opt in.

![RuneVersus duel card](docs/images/duel-card.png)

## Social Modes

RuneVersus includes social tools from the side panel:

- Monthly League: opens a large competitive window for the configured WOM group.
  The overall score is 50% clan-relative EHP rank and 50% clan-relative EHB rank.
  CLogs have their own podium and never affect the overall result. Seasons reset
  automatically at 00:00 UTC on the first day of each month; the previous eleven
  months remain selectable.
- Clan Member Comparison: opens a large, independent window containing every
  member of the configured Wise Old Man group. Switch between 24h, week, month,
  year, and all-time totals;
  rank by XP, CLogs, or total boss KC; search by name; and review clan totals,
  active-member counts, average active XP, and the three period leaders.
- Export Progress Card: creates one 16:9 card containing all 15 period/category
  leaders and exposes it through the clickable `Open saved card` link.

![RuneVersus monthly league](docs/images/monthly-league.png)

## Monthly League Integrity

- The competitive roster is frozen on the first successful load of a season.
- A player is eligible only when both their clan join date and first usable WOM
  snapshot fall within the first 72 hours of the month. Missing dates fail closed:
  the player remains visible as Provisional but cannot enter the podium.
- Later clan additions cannot enter the frozen roster. Previously frozen members
  remain listed even if they leave the WOM group during the month.
- After month end, a player needs a snapshot from the final 48 hours to remain
  eligible. The first successful post-season refresh seals an immutable local result.
- Archives are stored under `~/.runelite/rune-versus/leagues` and are not synced
  between computers. For one authoritative clan podium, designate one organizer
  to load the season during its first 72 hours and refresh it after month end.

## Settings

The config is intentionally grouped into three simple sections:

- Player data: account type, optional recent XP source, and optional private data
  import. `Open comparison window` is on by default; disable it to keep the
  full Versus side-panel view with scores, highlights, filters, and every compared metric.
- Card export: PNG export, clipboard copy, card style, and verdict tone.
- Clan: right-click player menus, optional clan `VS Set` shortcuts, and the Wise
  Old Man group ID.

Card export and Clan settings are collapsed by default. The side panel starts
with only two player fields and one Compare button; clan tools remain directly
available below the comparison controls.

The default settings are designed for quick testing: cards auto-save, PNG paths
are copied, and right-click player menus are enabled.

## Data And Privacy

- Boss KC, all-time skill XP, activities, clues, and public collection-log count
  come from the official OSRS hiscores.
- Recent 24h, week, and month XP requires Wise Old Man. Enabling this sends the
  compared RSNs to Wise Old Man.
- WOM responses are cached for 5 to 30 minutes depending on the view. RuneVersus
  identifies itself with a dedicated User-Agent, spaces requests, limits request
  bursts, and honors server `Retry-After` cooldowns.
- Clan Progress requires the numeric Wise Old Man group ID from the group's WOM
  page. RuneVersus makes five bulk read-only requests (day, week, month, year,
  and current bulk hiscores); rankings depend on the snapshots available in
  Wise Old Man.
- Monthly League uses one WOM bulk-gains request with exact calendar dates plus
  one group-membership request to verify clan join dates. Results are cached for
  30 minutes and archived locally for roster integrity.
- EHP normalizes skill XP and EHB normalizes boss KC using Wise Old Man's
  account- and activity-specific efficiency rates. Collection gains remain a
  separate recognition category because public totals cannot distinguish easy
  early slots from rare late-game unlocks.
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

Compact `!vs` chat comparisons use two equally weighted categories: total skill
XP and public collection-log count. Combat Achievement tiers remain available
only inside detailed comparisons when both players provide local opt-in data.

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

Generate the checked-in release screenshots and Plugin Hub icon:

```bash
./gradlew releaseAssets
```

Report bugs through the [RuneVersus issue tracker](https://github.com/hakimbalestrieri/RuneVersus/issues).

For Plugin Hub submission steps, see [PLUGIN_HUB_SUBMISSION.md](PLUGIN_HUB_SUBMISSION.md).
