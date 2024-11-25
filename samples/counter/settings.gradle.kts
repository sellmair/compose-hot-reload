pluginManagement {
    plugins {
        id("org.jetbrains.compose-hot-reload") version "1.0.0-dev.28.4"
    }

    repositories {
        maven(file("../..//build/repo"))
        maven("https://repo.sellmair.io")
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        kotlin("multiplatform") version "2.0.21-firework.28"
        kotlin("plugin.compose") version "2.0.21-firework.28"
        id("org.jetbrains.compose") version "1.7.1"
    }
}

dependencyResolutionManagement {
    repositories {
        maven(file("../..//build/repo"))
        maven("https://repo.sellmair.io")
        mavenCentral()
        google()
    }
}

include(":app")
include(":widgets")