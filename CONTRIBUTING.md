# Contributing to GhostMask

Thanks for helping improve GhostMask. Bug reports, translations, and code changes are all welcome.
Everyone taking part is expected to follow the [code of conduct](CODE_OF_CONDUCT.md).

## Reporting a bug

Open an issue with:

- What you did, what you expected, and what happened instead
- Your phone model, Android version, and GhostMask version (Android Settings > Apps > GhostMask)
- Which screen you were on, the cover image size, and whether it was hide or reveal

## Suggesting a feature

Open an issue describing the problem you want solved before writing code, so we can agree on
the approach first.

## Translations

Strings live in `res/values/strings.xml` (English) and `res/values-<language>/strings.xml`.
To add a language, copy the English file into a new `values-<code>` folder and translate the
values, keeping the `name` attributes and any `%1$s`-style placeholders unchanged.

## Pull requests

1. Fork the repository and create a branch from `main`.
2. Keep each pull request to one change, and explain what it does and why.
3. Match the style of the surrounding code (Kotlin, Jetpack Compose).
4. Run the unit tests and make sure they pass:

   ```bash
   ./gradlew testDebugUnitTest
   ```

5. For changes to encryption or steganography, add or update unit tests; never weaken the crypto defaults.
6. Add a line under "Unreleased" in [CHANGELOG.md](CHANGELOG.md) for anything users will
   notice, and update the README or architecture docs if you change how the app is
   structured, its permissions, or its build steps.

CI runs the unit tests and a debug build on every pull request.

Do not commit build output, keystores, `local.properties`, `keystore.properties`, or IDE
settings; `.gitignore` already excludes them.

## License of contributions

By submitting a pull request, you agree that your contribution is licensed under the
[Apache License 2.0](LICENSE), the same license as the project.
