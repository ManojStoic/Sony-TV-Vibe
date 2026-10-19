# Device cleanup: review before action

There is no universal safe debloat list for Android TV. Vendor, Google, and media packages can provide casting, Bluetooth, HDMI input, updates, accessibility, remote control, voice, DRM, parental controls, or the installed launcher.

1. Run Phase 1 and inspect `packages-third-party.txt`, `packages-system.txt`, and `packages-disabled.txt`.
2. Make an owner-approved list of third-party packages that are known to be unused or duplicated.
3. Record each candidate's package name, visible app name, why it is unused, and whether its data should be retained.
4. Review the exact command and its rollback path before making any change.

Do not remove or disable system packages solely because they sound promotional, diagnostic, or unfamiliar. Keep the stock launcher and its supporting packages. Removing an app does not by itself guarantee better performance; storage pressure, startup services, and firmware behavior should be measured first.

This template intentionally provides no debloat script. That prevents a clone from applying one TV's assumptions to another TV.
