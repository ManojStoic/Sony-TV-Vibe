# Launcher customization

The Home row is intentionally small: edit the six `tile(...)` calls in `MainActivity.java` to choose your favourites. For ordinary Android TV applications, use `launchPackage("package.name")`; it resolves the installed Leanback activity first and shows an availability message if the app is absent.

Use `launchVerified("package.name","fully.qualified.Activity")` only for a component found by Phase 1 inspection. This is appropriate for vendor-specific apps whose generic launch intent is unreliable.

App banners and icons are read from applications installed on the target TV. No brand artwork is bundled into this template. Verify white or transparent artwork, focus scaling, and text on the physical panel after changing favourites.

## Built-in media player

Some televisions expose a built-in USB/media player, while others do not. After verifying an exported activity, set these fields in `DeviceTargets.java`:

```java
static final String MEDIA_PLAYER_PACKAGE="package.name";
static final String MEDIA_PLAYER_ACTIVITY="package.name.PlayerActivity";
```

Leave both empty when unverified. The Media submenu will explain that no USB player has been configured instead of guessing a target.

## Package identity

`com.sonytvvibe.launcher` is the template application ID. Change it before distributing your own variant. A changed application ID is a separate Android app; it cannot update an existing installation using the old ID. Preserve the same signing key when publishing an update to an existing package.
