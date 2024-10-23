package org.jetbrains.compose.reload

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

private const val DEV_ENTRY_POINT_PROPERTY = "dev.entry.point"

abstract class RunDevEntryPointTask : DefaultTask() {
    @get:Input
    val devEntryPoint: Provider<String> = project.provider {
        if (project.hasProperty(DEV_ENTRY_POINT_PROPERTY)) {
            project.property(DEV_ENTRY_POINT_PROPERTY) as String
        } else {
            error("$DEV_ENTRY_POINT_PROPERTY property is not set")
        }
    }

    @TaskAction
    fun run() {
        logger.lifecycle("There will be a run of the entry function: ${devEntryPoint.get()}")
    }
}

fun Project.registerRunDevEntryPointTask() {
    tasks.register("runDevEntryPoint", RunDevEntryPointTask::class.java)
}