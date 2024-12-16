@file:Suppress("UnstableApiUsage")

import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

plugins {
    `java-base` apply false
}

tasks.withType<AbstractTestTask>().configureEach {
    if (this is Test) {
        useJUnitPlatform()

        maxHeapSize = "4G"
        properties.filter { (key, _) -> key.startsWith("chr") }.forEach { (key, value) ->
            systemProperty(key, value.toString())
        }

        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
            vendor.set(JvmVendorSpec.JETBRAINS)
        })

        systemProperty(
            "firework.version", project.versionCatalogs.named("deps").findVersion("firework").get().requiredVersion
        )

        systemProperty(
            "repo.path", project.rootDir.absolutePath
        )

        /* We do not want to open actual windows */
        systemProperty("apple.awt.UIElement", true)

        if (!providers.environmentVariable("CI").isPresent) {
            systemProperty("junit.jupiter.execution.parallel.enabled", "true")
            systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
            systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
            systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "4")
        }

        maxParallelForks = 2
    }

    testLogging {
        showStandardStreams = true
        events = setOf(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
    }
}

plugins.withType<KotlinPluginWrapper> {
    dependencies {
        if (project.name != "hot-reload-core") {
            "testImplementation"(testFixtures(project(":hot-reload-core")))
        }

        "testImplementation"(kotlin("test"))
        "testImplementation"(project.versionCatalogs.named("deps").findLibrary("junit-jupiter").get())
        "testImplementation"(project.versionCatalogs.named("deps").findLibrary("junit-jupiter-engine").get())
    }
}
