@file:Suppress("UnstableApiUsage")

import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings").version("2.1.0")
}

/*
Configure Repositories / Dependencies
*/
dependencyResolutionManagement {
    versionCatalogs {
        create("deps") {
            from(files("dependencies.toml"))
        }
    }

    repositories {
        repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

        mavenCentral()
        google {
            mavenContent {
                includeGroupByRegex(".*android.*")
                includeGroupByRegex(".*androidx.*")
                includeGroupByRegex(".*google.*")
            }
        }
        intellijPlatform { defaultRepositories() }
    }
}

include(":hot-reload-ide-plugin")
include(":hot-reload-agent")
include(":hot-reload-gradle-plugin")
include(":hot-reload-runtime-api")
include(":hot-reload-runtime-jvm")

gradle.lifecycle.beforeProject {
    group = "org.jetbrains.compose"
    version = project.providers.gradleProperty("version").get()
}