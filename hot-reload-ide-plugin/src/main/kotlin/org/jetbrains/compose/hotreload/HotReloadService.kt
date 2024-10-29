package org.jetbrains.compose.hotreload

import com.intellij.openapi.components.Service
import org.jetbrains.compose.reload.orchestration.OrchestrationServer
import org.jetbrains.compose.reload.orchestration.startOrchestrationServer

@Service(Service.Level.PROJECT)
class HotReloadService {
    private var _server: OrchestrationServer? = null
    private val server: OrchestrationServer get() = _server ?: startNewServer()

    val port: Int get() = server.port

    private fun startNewServer(): OrchestrationServer {
        _server?.close()
        val newServer = startOrchestrationServer().apply {
            invokeWhenClosed { _server = null }
        }
        _server = newServer
        return newServer
    }
}
