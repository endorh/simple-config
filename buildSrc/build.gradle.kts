plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://files.minecraftforge.net/maven") {
        name = "Minecraft Forge"
    }
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("net.minecraftforge.gradle:ForgeGradle:6.0.+") {
        isChanging = true
    }
}