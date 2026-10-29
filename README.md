# Katachi

Yugen's personalization surface: Quick Settings panels, Liquid Glass and the power menu.

This repository is **not a standalone APK**. It is checked out at `packages/apps/Settings/Katachi`
and compiled into the Settings APK, the same way `Atelier` is compiled into crDroid's Settings.
That buys us Settings' fragment base classes, preference widgets, theme, back stack and search
without a new manifest, permission or product package.

## Layout

```
src/org/yugen/katachi/   fragments and controllers, package org.yugen.katachi
res/                     Katachi resources, merged into the Settings resource set
```

Resources are addressed through `com.android.settings.R` because both modules share one APK.
Anything added here must therefore use a unique resource name (`katachi_*` for new resources).

## Wiring (lives in the Settings repo)

- `Android.bp`: `srcs` gains `Katachi/src/**/*.kt` and `Katachi/src/**/*.java`,
  `resource_dirs` gains `Katachi/res`.
- `res/xml/top_level_settings*.xml`: the `top_level_katachi` card points at
  `org.yugen.katachi.KatachiDashboard`.
- `.gitignore`: ignores `Katachi/` so the parent repo stays clean.
- `android/snippets/yugen.xml`: nested project
  `path="packages/apps/Settings/Katachi" name="android_packages_apps_Katachi"`.

## Storage

All values are `Settings.Secure` keys so SystemUI can observe them without a broadcast:

| Key | Values |
|---|---|
| `qs_panel_style` | `0` default, `1` Yugen, `2` MyUI, `3` Harmony |
| `qs_connectivity_folder` | `0`/`1`, defaults to on for the Yugen panel |
| `qs_oneui_experimental` | `0`/`1` |
| `penguin_liquid_glass` | `0`/`1` |
| `penguin_liquid_glass_style` | `0` rimmed, `1` frosted |
| `penguin_glass_tab_bars` | `0`/`1` |
| `power_menu_style` | `0` Yugen grid, `1` iOS slide to act |
