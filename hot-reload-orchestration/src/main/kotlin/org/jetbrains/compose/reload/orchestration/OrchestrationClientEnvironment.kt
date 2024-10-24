package org.jetbrains.compose.reload.orchestration

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.jetbrains.compose.reload.orchestration.launchSocketHandler

object OrchestrationClientEnvironment {
    val orchestrationServerPort: Int? =
        System.getenv("COMPOSE_RELOAD_ORCHESTRATION_PORT")?.toIntOrNull()
            ?: System.getProperty("compose.reload.orchestration.port")?.toIntOrNull()
}
