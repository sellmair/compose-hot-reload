package org.jetbrains.compose.reload.utils

import org.gradle.api.Project
import org.gradle.internal.impldep.org.junit.platform.commons.support.AnnotationSupport
import org.jetbrains.compose.reload.HOT_RELOAD_VERSION
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File
import kotlin.io.path.createFile
import kotlin.io.path.writeText

annotation class DefaultSettingsGradleKts

internal class DefaultSettingsGradleKtsExtension(
    private val testedVersions: TestedVersions,
) : BeforeTestExecutionCallback {

    override fun beforeTestExecution(context: ExtensionContext) {
        if (!AnnotationSupport.isAnnotated(context.requiredTestMethod, DefaultSettingsGradleKts::class.java)) {
            return
        }


        val projectDir = context.getHotReloadTestFixtureOrThrow().projectDir
        projectDir.settingsGradleKts.createFile()
        projectDir.settingsGradleKts.writeText(
            """
            pluginManagement {
                plugins {
                    kotlin("multiplatform") version "${testedVersions.kotlin}"
                    kotlin("jvm") version "${testedVersions.kotlin}"
                    kotlin("plugin.compose") version "${testedVersions.kotlin}"
                    id("org.jetbrains.compose") version "${testedVersions.compose}"
                    id("org.jetbrains.compose-hot-reload") version "$HOT_RELOAD_VERSION"
                }
                
                repositories {
                    maven(file("${localTestRepoDirectory.absolutePath}"))
                    mavenCentral()
                    google()
                }
            }

            dependencyResolutionManagement {
                repositories {
                    maven(file("${localTestRepoDirectory.absolutePath}"))
                    mavenCentral()
                    google()
                }
            }
        """.trimIndent()
        )
    }
}

private val localTestRepoDirectory: File
    get() {
        val localRepoPath = System.getProperty("local.test.repo") ?: error("Missing 'local.test.repo' property")
        val localRepo = File(localRepoPath)
        if (!localRepo.exists()) error("Local repository does not exist: $localRepo")
        return localRepo
    }