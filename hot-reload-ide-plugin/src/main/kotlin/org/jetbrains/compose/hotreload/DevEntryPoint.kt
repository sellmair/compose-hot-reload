package org.jetbrains.compose.hotreload

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil.runTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.compose.reload.orchestration.startOrchestrationServer
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

internal data class DevEntryPoint(
    val fqName: String,
    val modulePath: String
)

internal val HOT_RELOAD_SERVER = startOrchestrationServer() //todo move to service

internal class RunDevEntryPointAction(
    private val entryPoint: DevEntryPoint
) : AnAction({ "Run development app" }, AllIcons.Actions.RerunAutomatically) {
    override fun actionPerformed(event: AnActionEvent) {
        event.project?.runDevEntryPoint(entryPoint)
    }
}

private fun Project.runDevEntryPoint(entryPoint: DevEntryPoint) {
    val project = this
    val settings = ExternalSystemTaskExecutionSettings().apply {
        externalSystemIdString = GradleConstants.SYSTEM_ID.id
        vmOptions = GradleSettings.getInstance(project).gradleVmOptions
        executionName = "Run: ${entryPoint.fqName}"
        externalProjectPath = entryPoint.modulePath
        taskNames = listOf("jvmRun")
        scriptParameters = "-Dcompose.reload.orchestration.port=${HOT_RELOAD_SERVER.port} -PmainClass=MainKt"
    }
    runTask(
        settings,
        DefaultRunExecutor.EXECUTOR_ID,
        project,
        GradleConstants.SYSTEM_ID,
        null,
        ProgressExecutionMode.IN_BACKGROUND_ASYNC,
        true,
        UserDataHolderBase()
    )
}