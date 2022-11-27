# Simple Config

![Simple Config menu](https://github.com/endorh/simple-config/raw/docs/wiki/images/cover.png)

Forge library mod to define config files with powerful config menus in a simple manner.

As a side effect, it can also provide menus (and commands) for other mods which
use Forge's config API, pretty much like
[Configured](https://github.com/MrCrayfish/Configured) does.

## Features for players

- Powerful config menus for every mod
- Edit Minecraft Options as if it was just another mod
- Edit server config remotely (if authorized)
- Create config presets or patches (partial presets)
- Create hotkeys to modify any config entry
- Full config access through commands
- Share presets/hotkeys within servers

## Features for developers
- Simple API to define config files
- Automatically generated menus and commands
- Automatic baking into fields (even with custom transformations)
- Automatically mapped translation keys for entries
- Create composite entry types with generic types (lists/maps/beans/...)
- Create custom serializable entry types

***

## Help and Documentation

Whether you're a player or a mod developer, you can check out the
[wiki](https://github.com/endorh/simple-config/wiki) if you need any help using this mod.

## Examples

The mod includes example configs, declared using both ways.

Check out the `endorh.simpleconfig.demo` package

You may also check the [Aerobatic Elytra](https://github.com/endorh/aerobatic-elytra) and
[Aerobatic Elytra Jetpack](https://github.com/endorh/aerobatic-elytra-jetpack) mods,
which use this library.

## Usage
See the wiki on [how to add Simple Config as a dependency](https://github.com/endorh/simple-config/wiki/Adding-as-a-Dependency).

## Kotlin API
If you're using Kotlin in your mod, there's a far better declarative
[API for Kotlin](https://github.com/endorh/simple-config/wiki/Kotlin-API)
based on property delegates.

***

## Hard dependency

Unfortunately, at least for now there's no way to shade the non-UI part of the library,
requiring mods using the library to have a non-optional dependency on the Simple Config
mod.
