import net.minecraftforge.gradle.common.tasks.ApplyRangeMap
import net.minecraftforge.gradle.common.tasks.ExtractExistingFiles
import net.minecraftforge.gradle.common.tasks.ExtractRangeMap
import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace
import java.text.SimpleDateFormat
import java.util.*

buildscript {
    repositories {
        maven("https://files.minecraftforge.net/maven")
        mavenCentral()
    }
    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:5.1.+") {
            isChanging = true
        }
    }
}

// Plugins
plugins {
    `java-library`
    id("net.minecraftforge.gradle")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("antlr.conventions")
    `maven-publish`
}

// Mod info --------------------------------------------------------------------

val modId = "simpleconfig"
val modGroup = "endorh.simpleconfig"
val githubRepo = "endorh/simpleconfig"
val apiVersion = "1.0.0"
val modVersion = "1.0.3"
val mcVersion = "1.18.2"
val forge = "40.1.0"
val forgeVersion = "$mcVersion-$forge"
val mappingsChannel = "official"
val mappingsVersion = "1.18.2"

val apiMavenArtifact = "$modGroup:$modId-api:$apiVersion"
val modMavenArtifact = "$modGroup:$modId:$modVersion"

val vendor = "Endor H"
val credits = ""
val authors = "Endor H"
val issueTracker = ""
val page = ""
val updateJson = ""
val logoFile = "${modId}.png"
val modDescription = """
    Provides a simple way for modders to define config files with autogenerated config menus and commands.
    Can also provide, or even replace config menus of other mods.
    
    Users can assign hotkeys to modify config values from within the game, and easily save and apply partial presets, which can be shared on the server.
    To edit server configs, players need to be authorized, or be top level operators.

    Optionally adds a button to the pause menu, which opens the mod list in order to access mod configs in-game . It's also possible to bind a hotkey to open the mod list, edit config hotkeys, or open a specific config menu.
""".trimIndent()

// License
val license = "LGPL"

group = modGroup
version = modVersion
val groupSlashed = modGroup.replace(".", "/")
val classname = "SimpleConfigMod"

// Attributes
val displayName = "Simple Config"
val apiDisplayName = "Simple Config API"

// Dependencies
val yamlVersion = "1.31"
val jeiVersion = "9.7.1.255"
val antlrVersion: String by extra

// Jar manifest attributes
val jarAttributes = mapOf(
    "Specification-Title"      to modId,
    "Specification-Vendor"     to vendor,
    "Specification-Version"    to "1",
    "Implementation-Title"     to project.name,
    "Implementation-Version"   to version,
    "Implementation-Vendor"    to vendor,
    "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date()),
    "Maven-Artifact"           to modMavenArtifact
)

// Mod properties replaced across resource files
val modProperties = mapOf(
    "modid"         to modId,
    "display"       to displayName,
    "version"       to version,
    "mcversion"     to mcVersion,
    "vendor"        to vendor,
    "authors"       to authors,
    "credits"       to credits,
    "license"       to license,
    "page"          to page,
    "issue_tracker" to issueTracker,
    "update_json"   to updateJson,
    "logo_file"     to logoFile,
    "description"   to modDescription,
    "group"         to group,
    "class_name"    to classname,
    "group_slashed" to groupSlashed,
)

// Source Sets -----------------------------------------------------------------

lateinit var apiSourceSet: SourceSet
sourceSets {
    apiSourceSet = create("api")
}

sourceSets.main.get().resources {
    // Include resources generated by data generators.
    srcDir("src/generated/resources")
}

if (project.hasProperty("UPDATE_MAPPINGS")) {
    val dirs = HashSet(apiSourceSet.java.srcDirs)
    dirs.removeIf { it.path.contains("antlr") }
    tasks.getByName<ExtractRangeMap>("extractRangeMap") {
        sources.from(dirs)
    }
    tasks.getByName<ApplyRangeMap>("applyRangeMap") {
        sources.from(dirs)
    }
    tasks.getByName<ExtractExistingFiles>("extractMappedNew") {
        targets.from(dirs)
    }
}

// Java options ----------------------------------------------------------------

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

println(
    "Java: " + System.getProperty("java.version")
    + " JVM: " + System.getProperty("java.vm.version") + "(" + System.getProperty("java.vendor")
    + ") Arch: " + System.getProperty("os.arch"))

// Minecraft options -----------------------------------------------------------

minecraft {
    mappings(mappingsChannel, mappingsVersion)
    
    // Run configurations
    runs {
        val client = create("client") {
            workingDirectory(file("run/client"))
    
            jvmArg("-XX:+AllowEnhancedClassRedefinition")
            
            // Allowed flags: SCAN, REGISTRIES, REGISTRYDUMP
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            property("mixin.env.disableRefMap", "true")
            
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                    source(apiSourceSet)
                }
            }
        }
        
        create("server") {
            workingDirectory(file("run/server"))
    
            jvmArg("-XX:+AllowEnhancedClassRedefinition")
            
            // Allowed flags: SCAN, REGISTRIES, REGISTRYDUMP
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            property("mixin.env.disableRefMap", "true")
            
            arg("nogui")
            
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                    source(apiSourceSet)
                }
            }
        }
        
        create("client2") {
            parent(client)
            args("--username", "Dev2")
        }
    }
}

// Repositories ----------------------------------------------------------------

repositories {
    maven("https://repo.maven.apache.org/maven2") {
        name = "Maven Central"
    }
    maven("https://www.cursemaven.com") {
        name = "Curse Maven" // Curse Maven
        content {
            includeGroup("curse.maven")
        }
    }
    maven("https://modmaven.k-4u.nl") {
        name = "ModMaven" // JEI fallback
    }
}

// Dependencies ----------------------------------------------------------------

lateinit var apiImplementation: Configuration
lateinit var copiedForRuntime: Configuration
configurations {
    copiedForRuntime = create("copiedForRuntime")
    apiImplementation = getByName(apiSourceSet.implementationConfigurationName).apply {
        extendsFrom(minecraft.get())
    }
    implementation.get().apply {
        extendsFrom(apiImplementation)
        extendsFrom(copiedForRuntime)
    }
}

dependencies {
    // Development tools
    apiImplementation("org.jetbrains:annotations:23.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    
    // Minecraft
    "minecraft"("net.minecraftforge:forge:${forgeVersion}")
    
    // API source set
    implementation(apiSourceSet.output)
    
    // Snake yaml
    // Run configurations don't load `snakeyaml` classes when using
    // `implementation` since the update to Java 16
    copiedForRuntime("org.yaml:snakeyaml:$yamlVersion")
    
    // Testing dependencies
    // Catalogue
    runtimeOnly(fg.deobf("curse.maven:catalogue-459701:3803098"))
    
    // Configured
    runtimeOnly(fg.deobf("curse.maven:configured-457570:3946495"))
    
    // JEI
    runtimeOnly(fg.deobf("mezz.jei:jei-$mcVersion:$jeiVersion"))
}

// Tasks -----------------------------------------------------------------------

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.classes {
    dependsOn(tasks.extractNatives.get())
}

lateinit var reobfShadowJar: RenameJarInPlace
lateinit var reobfJar: RenameJarInPlace
reobf {
    reobfShadowJar = create("shadowJar")
    reobfJar = create("jar")
}

// Workaround issue of `snakeyaml` classes not being added to run configurations
//   We copy its classes files into our classpath
val extractMissingRuntimeClassesTask = tasks.register<Copy>("extractMissingRuntimeClasses") {
    group = "build"
    copiedForRuntime.asFileTree.forEach {
        from(zipTree(it).matching {
            include("**/*.class")
        })
    }
    
    includeEmptyDirs = false
    into("$buildDir/classes/java/main")
}

tasks.classes {
    dependsOn(extractMissingRuntimeClassesTask)
}

// Jars ------------------------------------------------------------------------

tasks.shadowJar {
    archiveBaseName.set("$modId-$mcVersion")
    archiveClassifier.set("") // Replace default jar
    
    from(apiSourceSet.output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    dependencies {
        include(dependency("org.antlr:antlr4:$antlrVersion"))
        include(dependency("org.antlr:antlr4-runtime:$antlrVersion"))
        include(dependency("org.yaml:snakeyaml:$yamlVersion"))
    }
    
    val shadowRoot = "$group.shadowed"
    val relocatedPackages = listOf(
        "org.antlr",
        "org.yaml.snakeyaml",
    )
    relocatedPackages.forEach { relocate(it, "$shadowRoot.$it") }
    
    manifest {
        attributes(jarAttributes)
    }
    
    finalizedBy(reobfShadowJar)
}

val apiJarTask = tasks.register<Jar>("apiJar") {
    group = "build"
    archiveBaseName.set("$modId-$mcVersion-api")
    archiveClassifier.set("")
    
    from(apiSourceSet.output)
    
    manifest {
        attributes(jarAttributes)
        attributes(mapOf("Maven-Artifact" to apiMavenArtifact))
    }
}

val apiSourcesJarTask = tasks.register<Jar>("apiSourcesJar") {
    group = "build"
    archiveBaseName.set("$modId-$mcVersion-api")
    archiveClassifier.set("src")
    
    from(apiSourceSet.allJava)
    
    manifest {
        attributes(jarAttributes)
        attributes(mapOf("Maven-Artifact" to "$apiMavenArtifact:${archiveClassifier.get()}"))
    }
}

val sourcesJarTask = tasks.register<Jar>("sourcesJar") {
    group = "build"
    archiveClassifier.set("sources")
    
    from(sourceSets.main.get().allJava)
    from(apiSourceSet.allJava)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes(jarAttributes)
        attributes(mapOf("Maven-Artifact" to "$modMavenArtifact:${archiveClassifier.get()}"))
    }
}

// Jar attributes
tasks.jar {
    archiveClassifier.set("flat")
    
    manifest {
        attributes(jarAttributes)
        attributes(mapOf("Maven-Artifact" to "$modMavenArtifact:${archiveClassifier.get()}"))
    }
    
    finalizedBy(reobfJar)
}

// Process resources
tasks.processResources {
    inputs.properties(modProperties)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    
    // Exclude development files
    exclude("**/.dev/**")
    
    from(sourceSets.main.get().resources.srcDirs) {
        // Expand properties in manifest files
        filesMatching(listOf("**/*.toml", "**/*.mcmeta")) {
            expand(modProperties)
        }
        // Expand properties in JSON resources except for translations
        filesMatching("**/*.json") {
            if (!path.contains("/lang/"))
                expand(modProperties)
        }
    }
}

// Publishing -----------------------------------------------------------------

artifacts {
    archives(tasks.shadowJar.get())
    archives(apiJarTask.get())
    archives(apiSourcesJarTask.get())
    archives(sourcesJarTask.get())
}

publishing {
    repositories {
        maven("https://maven.pkg.github.com/$githubRepo") {
            name = "GitHubPackages"
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
        
        maven(rootProject.projectDir.parentFile.resolve("maven")) {
            name = "LocalMods"
        }
    }
    
    publications {
        register<MavenPublication>("mod") {
            artifactId = "$modId-$mcVersion"
            version = modVersion
            
            artifact(tasks.shadowJar.get())
            artifact(sourcesJarTask.get())
            
            pom {
                name.set(displayName)
                url.set(page)
                description.set(modDescription)
            }
        }
        
        register<MavenPublication>("api") {
            artifactId = "$modId-$mcVersion-api"
            version = apiVersion
            
            artifact(apiJarTask.get())
            artifact(apiSourcesJarTask.get())
            
            pom {
                name.set(apiDisplayName)
                url.set(page)
                description.set(modDescription)
            }
        }
    }
}
