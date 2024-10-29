package org.jetbrains.compose.hotreload

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.compose.reload.orchestration.OrchestrationClient
import org.jetbrains.compose.reload.orchestration.OrchestrationClientRole
import org.jetbrains.compose.reload.orchestration.connectOrchestrationClient
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.ui.component.Text

private val LOG = logger<HotReloadToolWindowFactory>()

class HotReloadToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun isDumbAware() = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val service = project.service<HotReloadService>()
        toolWindow.addComposeTab { HotReloadPanel(service.port) }
    }
}

@Composable
private fun HotReloadPanel(port: Int) {
    val (client, setClient) = remember(port) { mutableStateOf<OrchestrationClient?>(null) }
    DisposableEffect(port) {
        setClient(connectOrchestrationClient(OrchestrationClientRole.Unknown, port))
        onDispose { client?.close() }
    }
    if (client == null) return

    Column {
        Text("Listen port: ${client.port}")

        val logMessages = remember { mutableStateListOf<String>() }
        LaunchedEffect(Unit) {
            client.invokeWhenMessageReceived { msg ->
                logMessages.add(msg.toString())
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(logMessages.size) { i -> Text(logMessages[i]) }
        }
    }
}
