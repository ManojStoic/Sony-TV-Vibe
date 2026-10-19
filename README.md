# TV Vibe Launcher Template

TV Vibe is a small native Android TV launcher template for people who want a focused, remote-first home screen they can tailor for their own television. It is designed for safe, repeatable vibe coding: inspect a TV first, customize the launcher locally, build a debug APK, test it as a preview, and only then decide whether to make it the default Home app.

The repository contains no device reports, network endpoints, pairing codes, account data, or per-TV package mappings. Runtime output is ignored by Git.

## What is included

- A Java/framework-only Android TV launcher with D-pad focus, app discovery, Search, All Apps, a media submenu, static local artwork, and a stock-launcher fallback option.
- A parameterized, read-only Phase 1 ADB collector in `tv-phase1/Collect-TVReadOnly.ps1`.
- A parameterized launcher-target inspector in `tv-phase2/Inspect-LaunchTargets.ps1`.
- A bounded prototype test helper in `tv-phase2/Native-Test.py`.
- A manual build script with no Gradle, Android Studio, or Maven dependency.

## Start here

1. Read [diagnostics](docs/PHASE-1-DIAGNOSTICS.md) and collect a report from the exact TV you have authorized.
2. Review the report. Do not disable, uninstall, or change settings as part of Phase 1.
3. Follow [launcher customization](docs/LAUNCHER-CUSTOMIZATION.md) to choose favourites and configure an optional built-in media player only after its activity has been verified.
4. Build the APK using [the launcher guide](tv-phase2/README.md).
5. Use [TV testing and launcher switching](docs/TV-TESTING.md) before selecting it as the default Home app.

## Scope and safety

The scripts always require an explicit ADB device serial or endpoint. They do not pair a device, turn on debugging, change the default launcher, disable packages, uninstall apps, reboot the TV, or alter TV settings.

Device cleanup is intentionally review-first. See [cleanup guidance](docs/DEVICE-CLEANUP.md): package lists cannot tell whether an app is useful to its owner, and system components may support casting, Bluetooth, input, updates, or the stock launcher.

## Reuse on another TV

Clone the project into a new folder, run Phase 1 against the new TV, customize the launch targets for that TV, and build a separate APK. Keep a distinct report folder per television and never copy another TV's package component names without resolving them again.

## License

The source code is available under the [MIT License](LICENSE). Third-party application icons, names, and installed banners are not bundled: the launcher reads artwork supplied by apps installed on the user’s own TV.
