plugins {
    id("simpleconfig.conventions")
    `maven-publish`
}

val modGroup: String by extra
val apiVersion: String by extra

group = modGroup
version = apiVersion

val modId: String by extra
val apiArtifactId = "$modId-api"

val displayName = "Simple Config API"
val vendor: String by extra
val credits: String by extra
val authors: String by extra
val issueTracker: String by extra
val page: String by extra
val updateJson: String by extra
val logoFile: String by extra
val modDescription: String by extra

val apiMavenArtifact: String by extra
val apiArchiveBaseName: String by extra

val jarAttributes = mapOf(
    "FMLModType"            to "LIBRARY",
    "Specification-Title"   to "Simple Config API",
    "Specification-Vendor"  to vendor,
    "Specification-Version" to version,
    "Maven-Artifact"        to apiMavenArtifact
)

tasks.jar {
    manifest {
        attributes(jarAttributes)
    }
    
    archiveBaseName.set(apiArchiveBaseName)

    // finalizedBy 'reobfJar'
    finalizedBy("copyJar")
}

val sourcesJarTask = tasks.register<Jar>("sourcesJar") {
    group = "build"
    from(sourceSets.main.get().allJava)
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier.set("sources")
}

val copyJar = tasks.register("copyJar") {
    copy {
        from(tasks.jar.get().archiveFile)
        into("$buildDir")
        rename { "api.jar" }
    }
}

artifacts {
    archives(tasks.jar.get())
    archives(sourcesJarTask.get())
}

publishing {
    publications {
        register<MavenPublication>("api") {
            artifact(tasks.jar.get())
            artifact(sourcesJarTask.get())
            artifactId = apiArtifactId
    
            pom {
                name.set(displayName)
                url.set(page)
                description.set(modDescription)
            }
        }
    }
    
    repositories {
        maven(rootProject.projectDir.parentFile.resolve("maven")) {
            name = "LocalMods"
        }
    }
}