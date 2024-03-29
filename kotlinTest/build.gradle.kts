import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    // ForgeGradle is declared in buildSrc, only Kotlin needs to be declared here
    dependencies {
        // Kotlin For Forge Gradle plugin dependencies
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0") // Must match KFF version
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.8.0") // Optional
    }
}

plugins {
    kotlin("jvm") version "1.8.0"
    id("net.neoforged.gradle") version "6.0.18+"
    id("simpleconfig.minecraft-conventions")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

// Mod info --------------------------------------------------------------------

val modId = "simplekonfig"
val modGroup = "endorh.simpleconfig"

object V {
    val api = "1.0.2"
    val kotlinApi = api
    val mod = api
    val minecraft = "1.20.1"
    val parchment = "2023.09.03"
    val forge = "47.1.79"
    val minecraftForge = "$minecraft-$forge"
    val minecraftMod = "$minecraft-$mod"

    object mappings {
        val channel = "parchment"
        val version = "${parchment}-${minecraft}"
    }

    // Dependencies
    val yaml = "1.33"
    val jei = "15.2.0.27"
    val kotlin = "1.8.22"
    val kotlinForForge = "4.5.0"
}

// License
val license = "LGPL"

version = V.minecraftMod

// Attributes
val displayName = "Simple Config"
val apiDisplayName = "Simple Config API"

// Java options ----------------------------------------------------------------

val jvmTargetVersion = 17
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmTargetVersion))
    }
}

tasks.withType<JavaCompile>().all {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions {
        jvmTarget = jvmTargetVersion.toString()
    }
}

// Minecraft options -----------------------------------------------------------

minecraft {
    // Change to your preferred mappings
    mappings(V.mappings.channel, V.mappings.version)
    
    runs {
        // Run configurations
        runs {
            create("client") {
                taskName("runKotlinTestClient")
                ideaModule("${rootProject.name}.${project.name}.main")
                // Separate client and server run configurations,
                //   to debug different common config files
                workingDirectory(file("run/client"))
                
                // JetBrains Runtime HotSwap (run with vanilla JBR 17 without fast-debug, see CONTRIBUTING.md)
                jvmArg("-XX:+AllowEnhancedClassRedefinition")
                
                // Allowed flags: SCAN, REGISTRIES, REGISTRYDUMP
                property("forge.logging.markers", "REGISTRIES")
                property("forge.logging.console.level", "debug")
                
                // Configure mixins for deobf environment
                property("mixin.env.disableRefMap", "true")
                
                mods {
                    create(modId) {
                        source(sourceSets.main.get())
                    }
                }
            }
        }
    }
}



// Include assets and data from data generators
sourceSets {
    main.get().resources {
        srcDirs("src/generated/resources")
    }
}

// Dependencies ----------------------------------------------------------------

// The `exclusiveContent` Gradle API breaks with ForgeGradle dependency
// remapping, so instead we use an includeOnly/excludeExplicit approach
val explicitDependencyGroups = mutableSetOf<String>()
fun MavenArtifactRepository.includeOnly(vararg groups: String) = content {
    groups.map { """${Regex.escape(it)}(\..*)?""" } // Include subgroups as well
      .forEach { includeGroupByRegex(it.also(explicitDependencyGroups::add)) }
}

fun MavenArtifactRepository.excludeExplicit() =
  content { explicitDependencyGroups.forEach { excludeGroupByRegex(it) } }

repositories {
    maven("https://www.cursemaven.com") {
        name = "Curse Maven" // Curse Maven
        includeOnly("curse.maven")
    }
    maven("https://modmaven.k-4u.nl") {
        name = "ModMaven" // JEI fallback
        includeOnly("mezz.jei")
    }
    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        name = "Kotlin for Forge"
        includeOnly("thedarkcolour")
    }

    mavenCentral {
        excludeExplicit()
    }
}

dependencies {
    // Development tools
    implementation("org.jetbrains:annotations:23.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    
    // Use the latest version of NeoForged
    minecraft("net.neoforged:forge:${V.minecraftForge}")
    
    // Simple Config
    compileOnly(rootProject.sourceSets["api"].output)
    compileOnly(rootProject.sourceSets["kotlinApi"].output)
    runtimeOnly(project(rootProject.path, configuration = "deobfShadowJar"))

    // Kotlin For Forge
    implementation("thedarkcolour:kotlinforforge:${V.kotlinForForge}")

    // Testing dependencies
    // Catalogue
    runtimeOnly(fg.deobf("curse.maven:catalogue-459701:4766090"))
    
    // Configured
    // runtimeOnly(fg.deobf("curse.maven:configured-457570:4462894"))
    
    // JEI
    runtimeOnly(fg.deobf("mezz.jei:jei-${V.minecraft}-forge:${V.jei}"))
}

// Tasks -----------------------------------------------------------------------

tasks.withType<Test>().all {
    useJUnitPlatform()
}

tasks.classes {
    dependsOn(tasks.extractNatives.get())
    
    // It seems the IntelliJ run configuration doesn't understand
    //   inter-project dependencies' Gradle task dependencies, so we
    //   need to manually depend on the shadowJar task here, as
    //   we don't run Minecraft from Gradle
    // The `prepareRuns` and `prepareRunKotlinTestClient` tasks are
    //   dynamically created by the ForgeGradle plugin even after
    //   `afterEvaluate`, so the only way to make them depend on the
    //   `shadowJar` task is to either use `tasks.all` and manually
    //   filter by name (ugh), or create the dependency from one of
    //   the other tasks they depend on, like `classes`
    dependsOn(rootProject.tasks.shadowJar)
}

val reobfJar = reobf.create("jar")
tasks.jar { finalizedBy(reobfJar) }
