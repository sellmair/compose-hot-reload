package org.jetbrains.compose.reload.orchestration

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.logging.Logger

private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

interface OrchestrationClient : OrchestrationHandle

fun OrchestrationClient(port: Int? = OrchestrationClientEnvironment.orchestrationServerPort): OrchestrationClient? {
    if (port == null) return null
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val incomingMessages = MutableSharedFlow<OrchestrationMessage>()
    val outgoingMessages = MutableSharedFlow<OrchestrationMessage>()

    val job = scope.launch {
        val socket = aSocket(SelectorManager(Dispatchers.IO)).tcp().connect(
            "localhost", port
        )

        currentCoroutineContext().job.invokeOnCompletion {
            logger.debug("Closing ${socket.localAddress}")
            socket.close()
        }

        launchSocketHandler(
            socket,
            incomingMessages = incomingMessages,
            outgoingMessages = outgoingMessages
        )
    }

    return object : OrchestrationClient {
        override suspend fun port(): Int = port
        override suspend fun send(message: OrchestrationMessage) {
            outgoingMessages.emit(message)
        }

        override val receive: SharedFlow<OrchestrationMessage> = incomingMessages.asSharedFlow()
        override fun close() = job.cancel()
    }
}
