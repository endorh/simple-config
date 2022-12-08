// ForgeGradle is declared in buildSrc, so buildscript is not needed

plugins {
    java
    id("net.minecraftforge.gradle")
    id("simpleconfig.minecraft-conventions")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

// Mod info --------------------------------------------------------------------

val modId = "simpleconfigtest"
val modGroup = "endorh.simpleconfig"

object V {
    val api = "1.0.0"
    val mod = "1.0.0"
    val minecraft = "1.19.2"
    val forge = "43.1.1"
    val minecraftForge = "$minecraft-$forge"
    val minecraftMod = "$minecraft-$mod"
    object mappings {
        val channel = "official"
        val version = V.minecraft
    }
    
    // Dependencies
    val simpleConfig = "1.0.+"
    
    // Integration
    val jei = "11.2.0.256"
}

// License
val license = "LGPL"

version = V.minecraftMod

// Attributes
val displayName = "Simple Config Test"
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

// Minecraft options -----------------------------------------------------------

minecraft {
    // Change to your preferred mappings
    mappings(V.mappings.channel, V.mappings.version)
    
    runs {
        // Run configurations
        runs {
            create("client") {
                taskName("runDeclarativeTestClient")
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
    
    maven(rootProject.projectDir.parentFile.resolve("maven")) {
        name = "LocalMods"
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
    compileOnly(rootProject.sourceSets["api"].output)
    // compileOnly("endorh.simpleconfig:simpleconfig-${V.minecraft}-api:${V.api}")
    // compileOnly(project(rootProject.path, configuration = "apiJar"))
    runtimeOnly(project(rootProject.path, configuration = "deobfShadowJar"))
    // runtimeOnly(fg.deobf("endorh.simpleconfig:simpleconfig-${V.minecraft}:${V.mod}"))
    
    // Testing dependencies
    // Catalogue
    runtimeOnly(fg.deobf("curse.maven:catalogue-459701:3873264"))
    
    // Configured
    runtimeOnly(fg.deobf("curse.maven:configured-457570:3947885"))
    
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
    // The `prepareRuns` and `prepareRunDeclarativeTestClient` tasks are
    //   dynamically created by the ForgeGradle plugin even after
    //   `afterEvaluate`, so the only way to make them depend on the
    //   `shadowJar` task is to either use `tasks.all` and manually
    //   filter by name (ugh), or create the dependency from one of
    //   the other tasks they depend on, like `classes`
    dependsOn(rootProject.tasks.shadowJar)
}

val reobfJar = reobf.create("jar")
tasks.jar { finalizedBy(reobfJar) }
