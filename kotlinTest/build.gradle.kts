import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        maven("https://files.minecraftforge.net/maven")
        mavenCentral()
    }
    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:5.1.+") {
            isChanging = true
        }
        // Make sure this version matches the one included in Kotlin for Forge
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
        // OPTIONAL Gradle plugin for Kotlin Serialization
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.7.10")
    }
}

plugins {
    kotlin("jvm") version "1.7.10"
    id("net.minecraftforge.gradle")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    // `maven-publish`
}

apply(from = "https://raw.githubusercontent.com/thedarkcolour/KotlinForForge/site/thedarkcolour/kotlinforforge/gradle/kff-3.7.1.gradle")

// Mod info --------------------------------------------------------------------

val modId = "simplekonfig"
val modGroup = "endorh.simpleconfig"

object V {
    val api = "1.0.0"
    val mod = "1.0.3"
    val minecraft = "1.19.2"
    val forge = "43.1.1"
    val minecraftForge = "$minecraft-$forge"
    val minecraftMod = "$minecraft-$mod"
    object mappings {
        val channel = "official"
        val version = V.minecraft
    }
    
    // Dependencies
    val simpleKonfigApi = "1.0.0"
    val simpleConfig = "1.0.+"
    
    // Integration
    val jei = "11.2.0.256"
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

tasks.withType<JavaCompile> {
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

val explicitGroups: MutableSet<String> = mutableSetOf()
fun MavenArtifactRepository.includeOnly(vararg groups: String) {
    content {
        groups.forEach {
            // Include subgroups as well
            val regex = "${it.replace(".", "\\.")}(\\..*)?"
            includeGroupByRegex(regex)
            explicitGroups.add(regex)
        }
    }
}

fun MavenArtifactRepository.excludeExplicit() {
    content {
        explicitGroups.forEach {
            excludeGroupByRegex(it)
        }
    }
}

repositories {
    maven("https://www.cursemaven.com") {
        name = "Curse Maven" // Curse Maven
        includeOnly("curse.maven")
    }
    maven("https://modmaven.k-4u.nl") {
        name = "ModMaven" // JEI fallback
        includeOnly("mezz.jei")
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
    
    // Simple Config
    compileOnly("endorh.simpleconfig:simplekonfig-${V.minecraft}-api:${V.simpleKonfigApi}")
    runtimeOnly(fg.deobf("endorh.simpleconfig:simpleconfig-${V.minecraft}:${V.simpleConfig}"))
    
    // Testing dependencies
    // Catalogue
    runtimeOnly(fg.deobf("curse.maven:catalogue-459701:3873264"))
    
    // Configured
    runtimeOnly(fg.deobf("curse.maven:configured-457570:3947885"))
    
    // JEI
    runtimeOnly(fg.deobf("mezz.jei:jei-${V.minecraft}-forge:${V.jei}"))
}

// Tasks -----------------------------------------------------------------------

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.classes {
    dependsOn(tasks.extractNatives.get())
}

// lateinit var reobfShadowJar: RenameJarInPlace
lateinit var reobfJar: RenameJarInPlace
reobf {
    // reobfShadowJar = create("shadowJar")
    reobfJar = create("jar")
}

// Jar attributes
tasks.jar {
    finalizedBy(reobfJar)
}
