package org.jetbrains.compose.reload

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

@Suppress("unused")
class ComposeHotReloadPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create(composeHotReloadExtensionName, ComposeHotReloadExtension::class.java, target)

        target.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            target.onKotlinPluginApplied()
        }

        target.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            target.onKotlinPluginApplied()
        }
    }
}

private fun Project.onKotlinPluginApplied() {
    /* Test to find 'Isolated Classpath' issues */
    try {
        assert(project.extensions.getByName("kotlin") is KotlinProjectExtension)
    } catch (t: LinkageError) {
        throw IllegalStateException("'Inaccessible Kotlin Plugin'")
    }

    setupComposeHotReloadRuntimeDependency()
    setupComposeHotReloadVariant()
    setupComposeHotClasspathTasks()
    setupComposeHotReloadExecTasks()
    setupComposeHotRunConventions()
}
