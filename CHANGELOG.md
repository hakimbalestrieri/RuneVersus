# Changelog

## Unreleased

- Keep the Monthly League roster open for the full 72-hour entry window before
  freezing it, while preserving the sealed post-season result.
- Reject malformed RSNs before network access and encode player names as a
  single WOM URL path segment.
- Bound WOM response sizes, group-array sizes, archive files, and optional local
  sync files; tolerate malformed external fields without crashing comparisons.
- Prevent stale asynchronous player, clan, or league requests from replacing a
  newer result or updating UI after plugin shutdown.
- Preserve existing card exports on filename collisions and keep successful PNG
  exports valid when the system clipboard is unavailable.
- Harden arithmetic against overflow and make number formatting locale-stable.
- Pin GitHub Actions to audited commit SHAs and run CI for every branch push.
- Redesign the README around player matchups, clan tools, league fairness,
  privacy, configuration, and release readiness.

## 0.1.0-beta.1 - 2026-07-21

- Add detailed player comparison windows with proportional red/blue metric bars.
- Add complete clan comparison for XP, collection logs, and individual boss KC.
- Add monthly clan leagues scored by relative EHP and EHB ranks, with a separate
  collection-log spotlight and shareable podium cards.
- Freeze eligible monthly participants, preserve departed members, and seal final
  standings in a local archive.
- Add Wise Old Man request identification, caching, pacing, and rate-limit backoff.
- Add clan-list context actions, OSRS-style icons, card exports, and release previews.
