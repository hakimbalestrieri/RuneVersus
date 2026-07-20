# Plugin Hub Submission

RuneVersus follows the RuneLite Plugin Hub `Creating new plugins` checklist:

- Public plugin repository based on the official external-plugin layout.
- Java 11 Gradle project with `runeLiteVersion = 'latest.release'`.
- `runelite-plugin.properties` includes `displayName`, `author`, `description`,
  `tags`, `plugins`, empty `version`, and `build=standard`.
- No non-RuneLite third-party runtime dependency is required.
- README describes the plugin features and data-source limitations.
- BSD 2-Clause license is present.

## Local Validation

```bash
./gradlew build
./gradlew run
```

## Plugin Hub Manifest

After this repository is pushed publicly, add exactly one file to your fork of
`runelite/plugin-hub`:

```properties
# plugin-hub/plugins/rune-versus
repository=https://github.com/<github-user>/runelite-versus.git
commit=<40-character commit hash from the public plugin repository>
```

Do not submit placeholder values. The Plugin Hub CI fetches the repository and
commit from this file.

## Refreshing The Commit Hash

Every plugin update needs a new Plugin Hub manifest commit hash:

```bash
git -C runelite-versus rev-parse HEAD
```

Paste that 40-character hash into `plugin-hub/plugins/rune-versus`, then open
or update the Plugin Hub pull request.
