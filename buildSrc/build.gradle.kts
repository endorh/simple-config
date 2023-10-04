plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://files.minecraftforge.net/maven") {
        name = "Minecraft Forge"
    }
    // maven("https://maven.parchmentmc.org") {
    //     name = "Parchment MC"
    // }

    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("net.minecraftforge.gradle:ForgeGradle:6.0.+") {
        isChanging = true
    }

    // implementation("org.parchmentmc.librarian.forgegradle:org.parchmentmc.librarian.forgegradle.gradle.plugin:1.+")
}