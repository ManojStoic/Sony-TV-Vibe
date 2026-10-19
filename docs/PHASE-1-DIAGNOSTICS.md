# Phase 1: read-only diagnostics

Phase 1 establishes facts before launcher configuration or cleanup. Install official Android SDK Platform-Tools locally, enable debugging on the television yourself, authorize the computer on that television, and identify the exact ADB serial or endpoint.

```powershell
$adb=(Resolve-Path '.\tv-phase1\platform-tools\adb.exe').Path
& $adb devices -l
.\tv-phase1\Collect-TVReadOnly.ps1 -AdbPath $adb -Device 'SERIAL_OR_HOST:PORT'
```

The collector writes a timestamped report under `tv-phase1/reports/`. It reads device/build properties, installed packages, disabled packages, storage, memory, CPU, uptime, and Home resolution. It does not launch an app, install anything, alter settings, pair a device, or modify packages.

For a target shortcut, resolve its actual activity rather than inferring it from a package name:

```powershell
.\tv-phase2\Inspect-LaunchTargets.ps1 -Device 'SERIAL_OR_HOST:PORT' -Package 'com.example.app','com.example.player'
```

Do not commit the resulting reports. They can contain device identifiers, installed application names, and implementation details that belong to the television owner.
