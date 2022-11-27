import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    // ForgeGradle is declared in buildSrc, only Kotlin needs to be declared here
    dependencies {
        // Kotlin For Forge Gradle plugin dependencies
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31") // Must match KFF version
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.5.31") // Optional
    }
}

plugins {
    kotlin("jvm") version "1.5.31"
    id("net.minecraftforge.gradle")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

// Mod info --------------------------------------------------------------------

val modId = "simplekonfig"
val modGroup = "endorh.simpleconfig"

object V {
    val api = "1.0.0"
    val mod = "1.0.0"
    val minecraft = "1.17.1"
    val forge = "37.1.1"
    val minecraftForge = "$minecraft-$forge"
    val minecraftMod = "$minecraft-$mod"
    object mappings {
        val channel = "official"
        val version = V.minecraft
    }
    
    // Dependencies
    val simpleKonfigApi = "1.0.0"
    val simpleConfig = "1.0.+"
    val kotlinForForge = "2.0.1"
    
    // Integration
    val jei = "8.3.1.62"
}

// License
val license = "LGPL"

version = V.minecraftMod

// Attributes
val displayName = "Simple Config"
val apiDisplayName = "Simple Config API"

// Java options ----------------------------------------------------------------

val jvmTargetVersion = 16
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
    
    // Local repository for faster multi-mod development
    maven(rootProject.projectDir.parentFile.resolve("maven")) {
        name = "LocalMods"
        includeOnly("endorh")
    }
    
    // GitHub Packages
    val gitHubRepos = mapOf(
        "endorh/simple-config" to "endorh.simpleconfig",
    )
    for (repo in gitHubRepos.entries) maven("https://maven.pkg.github.com/${repo.key}") {
        name = "GitHub/${repo.key}"
        includeOnly(repo.value)
        credentials {
            // read:packages only GitHub token published by Endor H
            // You may as well use your own GitHub PAT with read:packages scope, until GitHub
            //   supports unauthenticated read access to public packages, see:
            //   https://github.com/orgs/community/discussions/26634#discussioncomment-3252637
            password = "\u0067hp_SjEzHOWgAWIKVczipKZzLPPJcCMHHd1LILfK"
        }
    }
    
    mavenCentral {
        excludeExplicit()
    }
}

dependencies {
    // Development tools
    implementation("org.jetbrains:annotations:23.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    
    // Use the latest version of Minecraft Forge
    minecraft("net.minecraftforge:forge:${V.minecraftForge}")
    
    // Kotlin for Forge
    implementation("thedarkcolour:kotlinforforge:${V.kotlinForForge}")
    
    // Simple Config
    compileOnly(rootProject.sourceSets["api"].output)
    compileOnly(rootProject.sourceSets["kotlinApi"].output)
    runtimeOnly(project(rootProject.path, configuration = "deobfShadowJar"))
    
    // Testing dependencies
    // Catalogue
    runtimeOnly(fg.deobf("curse.maven:catalogue-459701:3529459"))
    
    // Configured
    runtimeOnly(fg.deobf("curse.maven:configured-457570:3537614"))
    
    // JEI
    runtimeOnly(fg.deobf("mezz.jei:jei-${V.minecraft}:${V.jei}"))
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

