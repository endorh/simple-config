![Simple Config menu](https://github.com/endorh/simple-config/raw/docs/wiki/images/cover.png)

![Minecraft: 1.16 - 1.19](https://img.shields.io/static/v1?label=&message=1.16%20-%201.19&color=2d2d2d&labelColor=4e4e4e&style=flat-square&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAZdEVYdFNvZnR3YXJlAHBhaW50Lm5ldCA0LjAuMjCGJ1kDAAACoElEQVQ4T22SeU8aURTF/ULGtNRWWVQY9lXABWldIDPIMgVbNgEVtaa0damiqGBdipXaJcY2ofEf4ycbTt97pVAabzK5b27u+Z377kwXgK77QthRy7OfXbeJM+ttqKSXN8sdwbT/A0L7elmsYqrPHZmROLPh5YkV4oEBwaKuHj+yyJptLDoAhbq3O1V1XCVObY3FL24mfn5oRPrcwSCRfQOyNWcjVjZdCbtcdwcgXrXUspdOKbDN/XE9tiBJMhXHT60gUIT2dMhcDLMc3NVKQklz0QIkf5qlyEcO6Qs7yPhMJB4amDMFimQSmqNlE8SKAZFzDfxHfVILIIZ10sJ3OwIbcqSuiOjchkzNCboHev9o2YhgiUP8mxnLN24I6/3ghYdtQG5iUMpFBuCP9iKwLsfiLyeCp2rMnZgwX3NArGoxW1Ridl+BzLEVKa8KSxOqNmDdz0kFnxaLHhWEgAyZigWhHXL+pEDy2ozsDxv8vAzTnh7w5kcghqCaFmCT10of4iPIT2mRdPUh4HoCcVwBH/8Ac2kzUkEV5r3EfVSOvbAJa5NDyI0r2oDtWb1EClh+OoC3Pg7v/Bw7p939yI4rsRW2Y3lKh01eh7WpIRyKZqzyjjYgPdIvlaMWRqYuG7wWryYHsRM0sFolZiPvQ3jheIwSmSBPdkByG/B6Wi3RYiVmRX7GiAPiUCRisii8D+jZNKvPBrHCW1GY0bAz6WkDCtOaSyKQFsi4K5NqNiZtehN2Y5uAShETqolhBqJXpfdPuPsuWwAaRdHSkxdc11mPqkGnyY4pyKbpl1GyJ0Pel7yqBoFcF3zqno5f+d8ohYy9Sx7lzQpxo1eirluCDgt++00p6uxttrG4F/A39sJGZWZMfrcp6O6+5kaVzXJHAOj6DeSs8qw5o8oxAAAAAElFTkSuQmCC)
[<img alt="Mod Loader: Forge" src="https://img.shields.io/badge/loader-forge-1976d2?style=flat-square"/>](https://files.minecraftforge.net/)
![GitHub](https://img.shields.io/github/license/endorh/simple-config?style=flat-square)
[<img alt="Curse Forge" src="https://cf.way2muchnoise.eu/short_670630_downloads(4E4E4E-E04E14-E0E0E0-2D2D2D-E0E0E0).svg?badge_style=flat"/>](https://www.curseforge.com/minecraft/mc-mods/simple-config)
[<img alt="Join the Discord" src="https://img.shields.io/discord/1017484317636710520?color=%235865F2&label=&labelColor=4e4e4e&logo=discord&logoColor=white&style=flat-square"/>](https://discord.gg/gqYVjBq65U)

[//]: # (![Forge versions]&#40;https://cf.way2muchnoise.eu/versions/Forge_670630_all.svg?badge_style=flat&#41;)

# Simple Config

Helps other mods provide powerful config menus.

Initially developed as an alternative to Forge's config API,
now it can also provide menus (and commands) for other mods
(and Minecraft itself) which use Forge's config API, pretty
much like how [Configured](https://github.com/MrCrayfish/Configured) does.

## Features for players
- Powerful config menus for every mod
- Edit Minecraft Options as if it was just another mod
- Edit server config remotely (if authorized)
- Create config presets or patches (partial presets)
- Create hotkeys to modify any config entry in-game
- Full config access through commands
- Share presets/hotkeys within servers

## Features for developers
- Simple Java API to define config files
- Declarative and extensible Java API based in annotations
- Dependency injection for use as soft dependency
- Declarative Kotlin API based on property delegates
- Automatically generated menus and commands
- Automatic baking into fields (with custom transformations)
- Automatically mapped translation keys for entries
- Create composite entry types with generic types (lists/maps/beans/...) or custom serializable types
- Provide multiple default presets/hotkeys for players to easily set up their config

***

## Help and Documentation
This mod has a [wiki](https://github.com/endorh/simple-config/wiki) which contains:
- A [Guide for Players](https://github.com/endorh/simple-config/wiki/Guide-for-Players)
- A [Guide for Mod Developers](https://github.com/endorh/simple-config/wiki/Guide-for-Mod-Developers)

You may also drop by our [Discord Server](https://discord.gg/gqYVjBq65U) if you have
any issues, questions or feedback of any kind, or if you just want to say hi.

## Examples
This mod includes a few demo configs:
- [`DemoConfigCategory`](https://github.com/endorh/simple-config/blob/1.19/src/main/java/endorh/simpleconfig/demo/DemoConfigCategory.java#L55)
  which showcases the Java API
- [`ClientKonfig`](https://github.com/endorh/simple-config/blob/1.19/kotlinTest/src/main/kotlin/endorh/simplekonfig/konfig/ClientKonfig.kt#L12)
  which showcases the Kotlin API
- [`DemoDeclarativeConfigCategory`](https://github.com/endorh/simple-config/blob/1.19/src/main/java/endorh/simpleconfig/demo/DemoDeclarativeConfigCategory.java#L43)
  which showcases the declarative Java API

You may also check the [Aerobatic Elytra](https://github.com/endorh/aerobatic-elytra) and
[Aerobatic Elytra Jetpack](https://github.com/endorh/aerobatic-elytra-jetpack) mods,
which use this library.

## Usage
See the wiki on [how to add Simple Config as a dependency](https://github.com/endorh/simple-config/wiki/Adding-as-a-Dependency).

***

## Soft dependency
It's possible to use Simple Config as a soft dependency through [dependency injection](https://github.com/endorh/simple-config/wiki/Using-as-a-Soft-Dependency).

This way, players will be able to use your mod without needing to install Simple Config.
However, the price that they'll pay is not being able to edit your mod's configuration,
neither through menus nor config files. They'll be forced to use your default values.

At the moment, dependency injection is not supported for the Kotlin API.
