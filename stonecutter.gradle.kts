allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.fabricmc.net/")
    }
}
plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.20.1"

