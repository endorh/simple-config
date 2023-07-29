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
which use Forge's config API, without any effort from their
part, pretty much like how
[Configured](https://github.com/MrCrayfish/Configured) does.

In multiplayer servers, it can allow certain players
(by default, all operators) to remotely edit the server
configuration for all mods, as well as share config presets
for all players to use.

As a bonus, it also wraps Minecraft's options, gamerules
and server properties as if Minecraft was just another mod,
which allows Minecraft options to benefit from the config
hotkey, preset and command features.

- [**Features for Players**](#features-for-players)
  - [**Menu Features**](#menu-features)
  - [**Example Use-cases**](#example-use-cases)
- [**Features for Mod Developers**](#features-for-mod-developers)
- [**Help and Documentation**](#help-and-documentation)
  - [**Examples**](#examples-for-mod-developers)
  - [**Usage**](#usage-for-mod-developers)
  - [**Usage as Soft Dependency**](#usage-as-soft-dependency-for-mod-developers)

## Features for players
- **Powerful** config menus for **every** mod
- Edit server configuration **remotely** *(if authorized)*
- Create and easily apply config **presets** or **patches** (partial presets)
- Create **hotkeys** to *modify* multiple config entries with a single key press
- **Share** presets/hotkeys within servers
- Benefit from all these features while you edit the usual **Minecraft options**

### Menu features
- All options for a mod are organized in a single tabbed screen, for easier spatial orientation
- **Search**/**Filter** by text in entries' *names*, *descriptions* or *values* (supports Regex)
- **Reset** entries or groups of entries to their default or **restore** them to their last saved value
- **Undo**/**Redo** changes before you save
- **Select** multiple entries/groups of entries to reset/restore/save them as a preset with a single click
- Create **hotkeys** which can **modify** a config entry, without leaving the menu
- **Organize** your config hotkeys in arbitrarily deep groups, which can be enabled/disabled with their own hotkey
- Detect **conflicts** in case you modify a menu at the same time as you edit its config file, or if you modify
  a server config at the same time as another player
- Easily **navigate** to config errors, warnings or conflicts to address them
- **Drag and drop** elements in lists/dictionaries to arrange them
- Use a middle click drag to easily **glance** at all options in multioption buttons
- **Switch** between sliders or keyboard input for numeric entries, in case you need a precise value, which in
  some cases can allow you to specify values **outside** the recommended range suggested by the slider
- **Color picker** with an user-customizable **palette**
- **Syntax highlighting** for some types of entries (NBT and Regex for now)
- **Smart autocompletion** in multioption entries
- Full **keyboard accesibility**, no need to swap your hand between your mouse and your keyboard all the time

### Example use-cases
- Create a hotkey to increase/decrease your FOV/mouse sensitivity by a certain amount
- Test config changes in your server knowing you can easily rollback to your last saved preset at any moment
- Create a datapack which changes server options in response to any game event
- Provide presets so new players of your server can easily setup their local configuration in few steps

## Features for mod developers
- Simple yet powerful Java API built around **reusable builders** to define config files
- Even simpler **declarative Java API** based in annotations, which can be extended with your own **custom annotations**
- **Dependency injection** for use as soft dependency (with natural [*limitations*](#usage-as-soft-dependency-for-mod-developers) if users don't have Simple Config installed)
- Simpler and safer **declarative Kotlin API** based on **property delegates**, if you have the luck of using Kotlin
- Organize entries in **groups** of arbitrary depth
- Automatically generated **menus** and **commands**, you only need to define the file
- Automatic **baking** into fields (easily pluggable with custom transformations/checks), for **faster** and **convenient** config **access** from your mod's logic
- Automatically mapped **translation keys** for entry **names** and **tooltips/comments**, no need to write entry descriptions in two separate locations
- Create **composite entry types** with **generic collections** (lists, sets or maps) (arbitrarily nested), custom **Java beans/Kotlin data classes**, or **arbitrary serializable types**
- Provide multiple **default presets/hotkeys** for players to easily set up their config when they setup your mod for the first time, choosing between different provided config suggestions (e.g., you could provide broken/easy/hard/hardcore presets to easily setup all options according to a certain difficulty level)
- Provide **insightful error messages** for invalid config values, which may depend on multiple entries
- **Disable** some entries **conditionally** (such as depending on the value of a feature toggle) to convey contextual irrelevance
- Group related entries in **pairs/triples** to reduce error predicates that depend on more than one *entry*
- Use entries as the **caption** of a group/collection of entries (where it makes sense, e.g., a feature toggle, master volume slider, ...)
- Apply multiple **tags** to config entries, to **organize** them better, or warn players about **experimental/advanced** options

***

## Help and Documentation
This mod has a [wiki](https://github.com/endorh/simple-config/wiki) which contains:
- A [Guide for Players](https://github.com/endorh/simple-config/wiki/Guide-for-Players)
- A [Guide for Mod Developers](https://github.com/endorh/simple-config/wiki/Guide-for-Mod-Developers)
- A [Guide to Translate Config Menus/Files](https://github.com/endorh/simple-config/wiki/Translating-Config-Menus), which can be done with a simple resource pack

You may also drop by our [Discord Server](https://discord.gg/gqYVjBq65U) if you have
any issues, questions or feedback of any kind, or if you just want to say hi.

### Examples (for mod developers)
This mod includes a few demo configs:
- [`DemoConfigCategory`](https://github.com/endorh/simple-config/blob/1.19/src/main/java/endorh/simpleconfig/demo/DemoConfigCategory.java#L55)
  which showcases the Java API
- [`ClientKonfig`](https://github.com/endorh/simple-config/blob/1.19/kotlinTest/src/main/kotlin/endorh/simplekonfig/konfig/ClientKonfig.kt#L12)
  which showcases the Kotlin API
- [`DemoDeclarativeConfigCategory`](https://github.com/endorh/simple-config/blob/1.19/src/main/java/endorh/simpleconfig/demo/DemoDeclarativeConfigCategory.java#L43)
  which showcases the declarative Java API
- [`DeclarativeJavaConfig`](https://github.com/endorh/simple-config/blob/1.19/declarativeTest/src/main/java/endorh/simpleconfig/test/DeclarativeJavaConfig.java)  which showcases the declarative Java API using dependency injection in a minimal test mod

You may also check the [Aerobatic Elytra](https://github.com/endorh/aerobatic-elytra) and
[Aerobatic Elytra Jetpack](https://github.com/endorh/aerobatic-elytra-jetpack) mods,
which use this library.

### Usage (for mod developers)
See the wiki on [how to add Simple Config as a dependency](https://github.com/endorh/simple-config/wiki/Adding-as-a-Dependency).

### Usage as soft dependency (for mod developers)
It's possible to use Simple Config as a soft dependency through [dependency injection](https://github.com/endorh/simple-config/wiki/Using-as-a-Soft-Dependency).

This way, players will be able to use your mod *without needing to install Simple Config*.
However, the price that they'll pay is **not being able to edit** your mod's configuration,
neither through **menus** nor config **files**. They'll be forced to use your **default values**.

At the moment, dependency injection is **not supported** for the **Kotlin API**.
