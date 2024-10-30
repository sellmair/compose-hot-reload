package org.jetbrains.compose.hotreload

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil.runTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.UserDataHolderBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.reload.orchestration.OrchestrationClientRole
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationServer
import org.jetbrains.compose.reload.orchestration.invokeWhenReceived
import org.jetbrains.compose.reload.orchestration.startOrchestrationServer
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.coroutines.suspendCoroutine

private val LOG = logger<HotReloadService>()

@Service(Service.Level.PROJECT)
class HotReloadService(
    private val project: Project,
    private val coroutineScope: CoroutineScope
) {
    private var _server: OrchestrationServer? = null
    private val server: OrchestrationServer get() = _server ?: startNewServer()

    val port: Int get() = server.port

    private var isApplicationConnected: Boolean = false

    private fun startNewServer(): OrchestrationServer {
        _server?.close()
        val newServer = startOrchestrationServer().apply {
            invokeWhenClosed { _server = null }
            invokeWhenReceived<OrchestrationMessage.UIRendered> {
                isApplicationConnected = true
            }
            invokeWhenReceived<OrchestrationMessage.ClientDisconnected> {
                if (it.clientRole == OrchestrationClientRole.Application) {
                    isApplicationConnected = false
                }
            }
        }
        _server = newServer
        return newServer
    }

    private suspend fun stopApplication() {
        val activeServer = _server ?: return
        if (!isApplicationConnected) return
        LOG.debug("Stopping application...")
        activeServer.close()
        while (_server != null) { delay(200) }
        LOG.debug("Application stopped")
    }

    private suspend fun askToStopApplication(): Boolean = suspendCoroutine { continuation ->
        continuation.resumeWith(
            Result.success(ConfirmStopAppDialog().showAndGet())
        )
    }

    fun runDevEntryPoint(entryPoint: DevEntryPoint) {
        LOG.debug("runDevEntryPoint: $entryPoint")
        coroutineScope.launch(EDT) {
            if (!isApplicationConnected || askToStopApplication()) {
                stopApplication()
                project.runDevEntryPoint(entryPoint, port)
            }
        }
    }
}

private fun Project.runDevEntryPoint(entryPoint: DevEntryPoint, port: Int) {
    LOG.debug("runDevEntryPoint: $entryPoint on port $port")
    val project = this
    val settings = ExternalSystemTaskExecutionSettings().apply {
        externalSystemIdString = GradleConstants.SYSTEM_ID.id
        vmOptions = GradleSettings.getInstance(project).gradleVmOptions
        executionName = "Run: ${entryPoint.functionName}"
        externalProjectPath = entryPoint.modulePath
        taskNames = listOf("devRun")
        scriptParameters = listOf(
            "-Dcompose.reload.orchestration.port=$port",
            "-DclassName=${entryPoint.className}",
            "-DfunName=${entryPoint.functionName}"
        ).joinToString(" ")
    }
    runTask(
        settings,
        DefaultRunExecutor.EXECUTOR_ID,
        project,
        GradleConstants.SYSTEM_ID,
        null,
        ProgressExecutionMode.IN_BACKGROUND_ASYNC,
        false,
        UserDataHolderBase()
    )
}

private class ConfirmStopAppDialog : DialogWrapper(true) {
    init {
        title = "Stop Application?"
        setOKButtonText("Stop")
        setCancelButtonText("Cancel")
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            val label = JLabel("One development application is already running. Stop it?").apply {
                preferredSize = Dimension(200, 80)
            }
            add(label, BorderLayout.CENTER)
        }
    }
}
