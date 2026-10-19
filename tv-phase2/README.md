# Launcher

This framework-only Java launcher targets Android TV API 26+ and builds with JDK 17 plus Android SDK Build-Tools/Platform 35. It has no Gradle, Maven, analytics, network client, background service, or runtime permission dependency.

## Customization

The six favourites in `MainActivity.java` are examples. Replace their labels and package/activity targets only after collecting a report from the target TV. A missing package produces an in-app availability message; the launcher does not install or substitute applications.

`DeviceTargets.java` intentionally has empty media-player fields. Populate them only with a package and exported activity verified by `Inspect-LaunchTargets.ps1`. Keep them empty if the TV's built-in media player cannot be confirmed.

## Build

Install JDK 17 and extract these official Android SDK archives into the matching locations:

| Archive | Destination |
| --- | --- |
| `build-tools_r35_windows.zip` | `tools/sdk/build-tools` |
| `platform-35_r02.zip` | `tools/sdk/platform` |

Run:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tv-phase2\Build-Debug.ps1
```

The APK is written to `tv-phase2/artifacts/tv-vibe-debug.apk`. Build output, downloaded SDK files, APKs, and generated debug signing material are ignored by Git.

## Test

Use an authorized device identifier explicitly:

```powershell
python .\tv-phase2\Native-Test.py --device 'SERIAL_OR_HOST:PORT' install
python .\tv-phase2\Native-Test.py --device 'SERIAL_OR_HOST:PORT' launch
python .\tv-phase2\Native-Test.py --device 'SERIAL_OR_HOST:PORT' capture home
```

Read [testing guidance](../docs/TV-TESTING.md) before deciding to select this launcher as Home.
