package org.jetbrains.compose.reload.orchestration

import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.connection
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.readInt
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.invoke.MethodHandles

private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

internal fun CoroutineScope.launchSocketHandler(
    socket: Socket,
    incomingMessages: MutableSharedFlow<OrchestrationMessage>,
    outgoingMessages: MutableSharedFlow<OrchestrationMessage>,
) {

    /* Receiver */
    launch {
        val readChannel = socket.connection().input
        while (!readChannel.isClosedForRead) {
            try {
                logger.debug("${socket.remoteAddress}: Receive")
                val packageSize = readChannel.readInt()
                logger.debug("${socket.remoteAddress}: Receive, packageSize=$packageSize")

                val pkg = readChannel.readByteArray(packageSize)
                val message = ObjectInputStream(ByteArrayInputStream(pkg)).readObject() as OrchestrationMessage
                logger.debug("${socket.remoteAddress}: Received, ${message.messageId}")
                incomingMessages.emit(message)
            } catch (e: Throwable) {
                logger.debug("${socket.remoteAddress}: Receive, Goodbye", e)
                socket.close()
                cancel()
            }
        }
    }

    /* Sender */
    launch {
        val sendChannel = socket.connection().output
        outgoingMessages.collect { message ->
            try {
                logger.debug("${socket.remoteAddress}: Send, ${message.messageId}")
                val pkg = ByteArrayOutputStream().use { baos ->
                    ObjectOutputStream(baos).writeObject(message)
                    baos.toByteArray()
                }

                sendChannel.writeInt(pkg.size)
                sendChannel.writeByteArray(pkg)
                sendChannel.flush()
            } catch (e: Throwable) {
                logger.debug("${socket.remoteAddress}: Send, Goodbye", e)
                socket.close()
                cancel()
            }
        }
    }
}