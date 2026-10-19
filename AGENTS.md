# Project instructions

This is a reusable Android TV launcher template. Keep device-specific reports, screenshots, APKs, keystores, and SDK downloads local; they are excluded from Git.

Phase 1 is read-only diagnostics. Every ADB command must target an explicitly supplied, authorized device identifier. Never pair, install, uninstall, launch other apps, change settings, switch Home apps, disable packages, clear data, reboot, root, or alter network ADB unless the owner explicitly authorizes that exact action.

Phase 2 is local launcher development and build validation. Do not assume app packages or launch activities from a different television. Use saved reports or `Inspect-LaunchTargets.ps1` to verify a target before configuring a shortcut. Preserve access to the installed stock launcher.

Any cleanup is review-first: write a candidate report, explain the impact, and wait for an explicit list of packages approved by the TV owner. Do not classify system packages as bloatware from their names alone.
