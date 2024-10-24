package org.jetbrains.compose.reload.orchestration

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

interface OrchestrationServer : OrchestrationHandle

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun CoroutineScope.launchOrchestrationServer(): OrchestrationServer {
    logger.debug("Starting new 'Orchestration Server'")
    val port = CompletableDeferred<Int>()
    val incomingMessages = MutableSharedFlow<OrchestrationMessage>()
    val outgoingMessages = MutableSharedFlow<OrchestrationMessage>()

    val job = launch(Dispatchers.IO) {
        /* The server shall reply all incoming messages to all clients */
        launch(Dispatchers.IO) {
            incomingMessages.collect { message ->
                outgoingMessages.emit(message)
            }
        }

        val socket = aSocket(SelectorManager(Dispatchers.IO)).tcp().bind(null)
        val localPort = when (val address = socket.localAddress) {
            is InetSocketAddress -> address.port
            is UnixSocketAddress -> error("Unexpected socket address: $address")
        }

        logger.debug("listening on port $localPort")
        port.complete(localPort)

        while (currentCoroutineContext().job.isActive) {
            val socket = socket.accept()

            logger.debug("Client connected (${socket.remoteAddress})")
            launchSocketHandler(socket, incomingMessages, outgoingMessages)
        }
    }

    return object : OrchestrationServer {
        override suspend fun port(): Int = port.await()
        override suspend fun send(message: OrchestrationMessage) = outgoingMessages.emit(message)
        override val receive: SharedFlow<OrchestrationMessage> = incomingMessages.asSharedFlow()
        override fun close(): Unit = job.cancel()
    }
}
