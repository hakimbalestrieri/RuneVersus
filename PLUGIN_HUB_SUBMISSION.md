# Plugin Hub Submission

RuneVersus follows the RuneLite Plugin Hub `Creating new plugins` checklist:

- Public plugin repository based on the official external-plugin layout.
- Java 11 Gradle project with `runeLiteVersion = 'latest.release'`.
- `runelite-plugin.properties` includes `displayName`, `author`, `description`,
  `tags`, `plugins`, empty `version`, and `build=standard`.
- No non-RuneLite third-party runtime dependency is required.
- README describes the plugin features and data-source limitations.
- BSD 2-Clause license is present.
- Root `icon.png` is 48x48 pixels, within the Plugin Hub limit.
- GitHub Actions validates the Java 11 build on every push and pull request;
  third-party actions are pinned to full commit SHAs.
- External WOM calls are HTTPS-only, identified, paced, cached, bounded by
  response size and member count, and honor both forms of `Retry-After`.
- The plugin uses no reflection, JNI, subprocess execution, runtime code
  downloads, credential storage, or automatic outgoing chat modification.

## Local Validation

```bash
./gradlew build
./gradlew run
```

The pre-release suite also checks malformed external responses, RSN validation,
request supersession, export collisions, clipboard failure, archive limits, and
monthly-roster integrity.

## Reviewer Note: Optional Private Metrics

`Private comparison data` is disabled by default and reads user-supplied local
properties only. It can display PB times, detailed collection totals, and a CA
tier when both compared players provide the relevant values. It never affects
the compact `!vs` result or Monthly League scoring and is explicitly described
as unverified in the README.

RuneLite's rejected-features guidance calls out PB hiscores and other spoofable
non-hiscore information. Confirm this narrow local-only presentation with the
Plugin Hub reviewer; if they consider it in scope, remove the optional private
metrics before approval rather than weakening the public-data comparisons.

## Plugin Hub Manifest

After this repository is pushed publicly, add exactly one file to your fork of
`runelite/plugin-hub`:

```properties
# plugin-hub/plugins/rune-versus
repository=https://github.com/hakimbalestrieri/RuneVersus.git
commit=<40-character commit hash from the public plugin repository>
```

Do not submit placeholder values. The Plugin Hub CI fetches the repository and
commit from this file.

## Refreshing The Commit Hash

Every plugin update needs a new Plugin Hub manifest commit hash:

```bash
git -C RuneVersus rev-parse HEAD
```

Paste that 40-character hash into `plugin-hub/plugins/rune-versus`, then open
or update the Plugin Hub pull request.
