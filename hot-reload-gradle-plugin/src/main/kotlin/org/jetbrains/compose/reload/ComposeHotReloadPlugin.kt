package org.jetbrains.compose.reload

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.DefaultKotlinBasePlugin

@Suppress("unused")
class ComposeHotReloadPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create(composeHotReloadExtensionName, ComposeHotReloadExtension::class.java, target)

        /*
        The Compose Hot Reload plugin is supposed to support Kotlin/JVM and Kotlin Multiplatform
         */
        target.plugins.withType<DefaultKotlinBasePlugin>().configureEach {
            target.setupComposeHotReloadRuntimeDependency()
            target.setupComposeHotReloadVariant()
            target.setupComposeHotClasspathTasks()
            target.setupComposeHotReloadExecTasks()
            target.setupComposeHotRunConventions()
        }

        target.registerRunDevEntryPointTask()
    }
}

