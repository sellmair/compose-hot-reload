package org.jetbrains.compose.reload.orchestration

import kotlinx.coroutines.flow.SharedFlow

sealed interface OrchestrationHandle : AutoCloseable {
    suspend fun port(): Int
    suspend fun send(message: OrchestrationMessage)
    val receive: SharedFlow<OrchestrationMessage>
}