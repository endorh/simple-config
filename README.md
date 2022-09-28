# Simple Config

Forge library mod to define config files with powerful config menus in a simple manner.

As a side effect, it can also provide menus (and commands) for other mods which
use Forge's config API, pretty much like
[Configured](https://github.com/MrCrayfish/Configured) does.

There are two supported ways to declare configs. Using entry builders is recommended,
but there's also support for reflection based config generation from static config classes.
Translation keys are automatically assigned to config entries by name.

## Examples

The mod includes example configs, declared using both ways.

Check out the `endorh.simpleconfig.demo` package

You may also check the [Aerobatic Elytra](https://github.com/endorh/aerobatic-elytra) and
[Aerobatic Elytra Jetpack](https://github.com/endorh/aerobatic-elytra-jetpack) mods,
which use this library.

## Help and Documentation

Whether you're a player or a mod developer, you can check out the
[wiki](https://github.com/endorh/simple-config/wiki) if you need any help using this mod.

## Usage
Add the following dependencies and repository to your buildscript:

<details><summary>Groovy Gradle</summary>

```Groovy
def mcVersion = "1.18.2"
def simpleConfigApiVersion = "1.0.0",
    simpleConfigVersion = "1.0.+"

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/endorh/simple-config")
        name = "SimpleConfig"
    }
}

dependencies {
    compileOnly "endorh.simpleconfig:simpleconfig-$mcVersion-api:$simpleConfigApiVersion"
    runtimeOnly fg.deobf("endorh.simpleconfig:simpleconfig-$mcVersion:$simpleConfigVersion")
}

```
</details>
<details><summary>Kotlin DSL</summary>

```Kotlin
val mcVersion = "1.18.2"
val simpleConfigApiVersion = "1.0.0"
val simpleConfigVersion = "1.0.+"

repositories {
    maven("https://maven.pkg.github.com/endorh/simple-config") {
        name = "SimpleConfig"
    }
}

dependencies {
    compileOnly("endorh.simpleconfig:simpleconfig-$mcVersion-api:$simpleConfigApiVersion")
    runtimeOnly(fg.deobf("endorh.simpleconfig:simpleconfig-$mcVersion:$simpleConfigVersion"))
}
```
</details>

## Kotlin API

There are plans to create a better Kotlin API based on property delegates,
and maybe redesign the Java reflection API to use non-static members.

## Hard dependency

Unfortunately, at least for now there's no way to shade the non-UI part of the library,
requiring mods using the library to have a non-optional dependency on the Simple Config
mod.

This is a known issue, but fixing it requires a major rewrite I'll tackle
when I have the time.
