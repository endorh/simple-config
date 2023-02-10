import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.common.tasks.ApplyRangeMap
import net.minecraftforge.gradle.common.tasks.ExtractExistingFiles
import net.minecraftforge.gradle.common.tasks.ExtractRangeMap
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.text.SimpleDateFormat
import java.util.*

// ForgeGradle is declared in buildSrc, so buildscript is not needed

// Plugins
plugins {
    `java-library`
    kotlin("jvm") version "1.5.31"
    id("net.minecraftforge.gradle")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("simpleconfig.antlr-conventions")
    id("simpleconfig.minecraft-conventions")
    `maven-publish`
}

// Mod info --------------------------------------------------------------------

val modId = "simpleconfig"
val simpleKonfig = "simplekonfig"
val modGroup = "endorh.simpleconfig"
val githubRepo = "endorh/simple-config"

val antlrVersion: String by extra
object V {
    val api = "1.0.0"
    val kotlinApi = api
    val mod = api
    val minecraft = "1.17.1"
    val forge = "37.1.1"
    val minecraftForge = "$minecraft-$forge"
    val minecraftMod = "$minecraft-$mod"
    object mappings {
        val channel = "official"
        val version = minecraft
    }
    
    // Dependencies
    val yaml = "1.31"
    val jei = "8.3.1.62"
    val kotlin = "1.5.31"
    val kotlinForForge = "2.0.1"
}

val vendor = "Endor H"
val credits = ""
val authors = "Endor H"
val issueTracker = "https://github.com/$githubRepo/issues"
val page = "https://www.curseforge.com/minecraft/mc-mods/simple-config"
val updateJson = "https://github.com/$githubRepo/raw/updates/updates.json"
val logoFile = "$modId.png"
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
version = V.minecraftMod
val groupSlashed = modGroup.replace(".", "/")
val classname = "SimpleConfigMod"
val baseArchiveName = "$modId-${V.minecraft}"

// Attributes
val displayName = "Simple Config"
val apiDisplayName = "Simple Config API"
val kotlinApiDisplayName = "Simple Konfig API"

// Jar manifest attributes
val jarAttributes = mapOf(
    "Specification-Title"      to modId,
    "Specification-Vendor"     to vendor,
    "Specification-Version"    to "1",
    "Implementation-Title"     to project.name,
    "Implementation-Version"   to version,
    "Implementation-Vendor"    to vendor,
    "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date()),
)

// Mod properties replaced across resource files
val modProperties = mapOf(
    "modid"         to modId,
    "display"       to displayName,
    "version"       to version,
    "mcversion"     to V.minecraft,
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

val mainSourceSet = sourceSets.main.get()
val apiSourceSet = sourceSets.create("api")
val kotlinApiSourceSet = sourceSets.create("kotlinApi")

mainSourceSet.resources {
    // Include resources generated by data generators.
    srcDir("src/generated/resources")
}

if (project.hasProperty("UPDATE_MAPPINGS")) {
    // Update mappings also in API source set
    tasks.getByName<ExtractRangeMap>("extractRangeMap") {
        sources.from(apiSourceSet.java.srcDirs)
    }
    tasks.getByName<ApplyRangeMap>("applyRangeMap") {
        sources.from(apiSourceSet.java.srcDirs)
    }
    tasks.getByName<ExtractExistingFiles>("extractMappedNew") {
        targets.from(apiSourceSet.java.srcDirs)
    }
}

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

// Console header --------------------------------------------------------------

fun sysProp(name: String) = System.getProperty(name)
println(
    "Mod: \"$displayName\" ($modId), version: ${V.minecraft}-${V.mod} (Forge: ${V.forge})")
println(
    "Java: ${sysProp("java.version")}, " +
    "JVM: ${sysProp("java.vm.version")}(${sysProp("java.vendor")}), " +
    "Arch: ${sysProp("os.arch")}")

// Minecraft options -----------------------------------------------------------

minecraft {
    mappings(V.mappings.channel, V.mappings.version)
    accessTransformer("src/main/resources/META-INF/accesstransformer.cfg")
    
    // Run configurations
    runs {
        val client = create("client") {
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
                    sources(
                        mainSourceSet,
                        apiSourceSet,
                    )
                }
            }
        }
        
        create("server") {
            // Separate client and server run configurations,
            //   to debug different common config files
            workingDirectory(file("run/server"))
    
            // JetBrains Runtime HotSwap (run with vanilla JBR 17 without fast-debug, see CONTRIBUTING.md)
            jvmArg("-XX:+AllowEnhancedClassRedefinition")
            
            // Allowed flags: SCAN, REGISTRIES, REGISTRYDUMP
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
    
            // Configure mixins for deobf environment
            property("mixin.env.disableRefMap", "true")
            
            // The integrated IDE console is enough
            arg("nogui")
            
            mods {
                create(modId) {
                    sources(
                        mainSourceSet,
                        apiSourceSet,
                    )
                }
            }
        }
        
        // Second client, for multiplayer tests
        create("client2") {
            parent(client)
            args("--username", "Dev2")
        }
    }
}

// Repositories ----------------------------------------------------------------

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
    mavenCentral {
        excludeExplicit()
    }
    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        name = "Kotlin for Forge"
        excludeExplicit()
    }
}

// Dependencies ----------------------------------------------------------------

// Snake YAML isn't loaded by Forge in dev environment for unknown reasons
val copiedForRuntime by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val apiImplementation = configurations.getByName(apiSourceSet.implementationConfigurationName).apply {
    extendsFrom(configurations.minecraft.get())
}
val apiApi = configurations.getByName(apiSourceSet.apiConfigurationName)
val apiAnnotationProcessor = configurations.getByName(apiSourceSet.annotationProcessorConfigurationName)
val kotlinApiImplementation = configurations.getByName(kotlinApiSourceSet.implementationConfigurationName).apply {
    extendsFrom(configurations.minecraft.get())
    extendsFrom(apiImplementation)
}
configurations {
    implementation.get().apply {
        extendsFrom(apiImplementation)
        extendsFrom(copiedForRuntime)
    }
    annotationProcessor.get().apply {
        extendsFrom(apiAnnotationProcessor)
    }
}

dependencies {
    // Development tools
    apiImplementation("org.jetbrains:annotations:23.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    
    // Minecraft
    minecraft("net.minecraftforge:forge:${V.minecraftForge}")
    
    // API source set
    implementation(apiSourceSet.output)
    
    // Kotlin API
    kotlinApiImplementation(apiSourceSet.output)
    kotlinApiImplementation(mainSourceSet.output)
    kotlinApiImplementation("thedarkcolour:kotlinforforge:${V.kotlinForForge}")
    
    // Snake yaml
    // Run configurations don't load `snakeyaml` classes when using
    // `implementation` since the update to Java 16
    copiedForRuntime("org.yaml:snakeyaml:${V.yaml}")
    
    // Testing dependencies
    // Catalogue
    runtimeOnly(fg.deobf("curse.maven:catalogue-459701:3529459"))
    
    // Configured
    runtimeOnly(fg.deobf("curse.maven:configured-457570:3537614"))
    
    // Zombie Awareness (multi-file support)
    // runtimeOnly(fg.deobf("curse.maven:coroutil-237749:4331325"))
    // runtimeOnly(fg.deobf("curse.maven:zombie-awareness-237754:3603741"))
    
    // JEI
    runtimeOnly(fg.deobf("mezz.jei:jei-${V.minecraft}:${V.jei}"))
}

// Tasks -----------------------------------------------------------------------

tasks.withType<Test>().all {
    useJUnitPlatform()
}

tasks.classes {
    dependsOn(tasks.extractNatives.get())
}

val deobfShadowJar by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}
val apiJar by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
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

tasks.withType<Jar>().all {
    group = "build"
    manifest { attributes(jarAttributes) }
}

fun Jar.setArchive(classifier: String = "", version: String = V.mod) {
    archiveBaseName.set(baseArchiveName)
    archiveClassifier.set(classifier)
    archiveVersion.set(version)
}

fun ShadowJar.configureShadowJar() {
    from(mainSourceSet.output)
    from(apiSourceSet.output)
    from(kotlinApiSourceSet.output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    configurations.addAll(listOf(
        project.configurations.antlr
    ).map { it.get() })
    dependencies {
        include(dependency("org.antlr:antlr4:$antlrVersion"))
        include(dependency("org.antlr:antlr4-runtime:$antlrVersion"))
        include(dependency("org.yaml:snakeyaml:${V.yaml}"))
    }
    
    val shadowRoot = "${project.group}.shadowed"
    val relocatedPackages = listOf(
        "org.antlr",
        "org.yaml.snakeyaml",
        "com.impetus",
    )
    relocatedPackages.forEach { relocate(it, "$shadowRoot.$it") }
}

tasks.shadowJar {
    setArchive("deobf")
    
    configureShadowJar()
}

val reobfShadowJarTask = tasks.register<ShadowJar>("reobfShadowJar") {
    setArchive("") // Replace default jar
    
    configureShadowJar()
}

val sourcesJarTask = tasks.register<Jar>("sourcesJar") {
    setArchive("sources")
    
    from(mainSourceSet.allSource)
    from(apiSourceSet.allSource)
    from(kotlinApiSourceSet.allSource)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val apiJarTask = tasks.register<Jar>("apiJar") {
    setArchive("api", V.api)
    
    from(apiSourceSet.output)
}

val kotlinApiJarTask = tasks.register<Jar>("kotlinApiJar") {
    setArchive("kotlin-api", V.kotlinApi)
    
    // Includes the Java API as part of the Kotlin API
    from(kotlinApiSourceSet.output)
    from(apiSourceSet.output)
}

tasks.jar {
    setArchive("flat")
}

// Process resources
tasks.processResources {
    inputs.properties(modProperties)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    
    // Exclude development files
    exclude("**/.dev/**")
    
    from(mainSourceSet.resources.srcDirs) {
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

val reobfShadowJar by reobf.creating
reobfShadowJarTask.configure { finalizedBy(reobfShadowJar) }

val reobfJar = reobf.create("jar")
tasks.jar { finalizedBy(reobfJar) }

// Publishing -----------------------------------------------------------------

artifacts {
    operator fun Configuration.invoke(
      artifact: Any, action: Action<in ConfigurablePublishArtifact> = Action {}
    ) = add(name, artifact, action)
    
    deobfShadowJar(tasks.shadowJar.get())
    apiJar(apiJarTask.get())
    archives(reobfShadowJarTask.get())
    archives(apiJarTask.get())
    archives(kotlinApiJarTask.get())
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
            artifactId = "$modId-${V.minecraft}"
            version = V.mod
    
            artifact(reobfShadowJarTask.get())
            artifact(apiJarTask.get())
            artifact(kotlinApiJarTask.get())
            artifact(sourcesJarTask.get())
    
            pom {
                name.set(displayName)
                url.set(page)
                description.set(modDescription)
            }
        }
    }
}
