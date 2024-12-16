package org.jetbrains.compose.reload.orchestration

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

public fun OrchestrationHandle.asBlockingQueue(): BlockingQueue<OrchestrationMessage> {
    val queue = LinkedBlockingQueue<OrchestrationMessage>()
    invokeWhenMessageReceived { message -> queue.put(message) }
    return queue
}

public fun OrchestrationHandle.asFlow(): Flow<OrchestrationMessage> = channelFlow {
    val registration = invokeWhenMessageReceived { message ->
        trySendBlocking(message)
    }

    awaitClose {
        registration.dispose()
    }
}

public fun OrchestrationHandle.asChannel(): ReceiveChannel<OrchestrationMessage> {
    val channel = Channel<OrchestrationMessage>(Channel.UNLIMITED)
    val registration = invokeWhenMessageReceived { message -> channel.trySendBlocking(message) }
    channel.invokeOnClose { cause -> registration.dispose() }
    invokeWhenClosed { channel.close() }
    return channel
}
