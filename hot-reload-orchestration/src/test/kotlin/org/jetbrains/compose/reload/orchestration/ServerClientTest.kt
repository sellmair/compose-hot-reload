package org.jetbrains.compose.reload.orchestration

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.jetbrains.annotations.TestOnly
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.LogMessage
import java.util.Collections
import java.util.Collections.synchronizedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ServerClientTest {
    @Test
    fun `test - simple ping pong`() = runTest {
        val server = launchOrchestrationServer()
        val client = OrchestrationClient(server.port()) ?: fail("Failed to create client")

        val serverReceivedMessages = synchronizedList(mutableListOf<OrchestrationMessage>())
        val clientReceivedMessages = synchronizedList(mutableListOf<OrchestrationMessage>())

        launch {
            server.receive.collect { message -> serverReceivedMessages.add(message) }
        }

        launch {
            client.receive.collect { message -> clientReceivedMessages.add(message) }
        }

        client.send(LogMessage("A"))

        Thread.sleep(1000)
        assertEquals(listOf(LogMessage("A")), serverReceivedMessages)
        assertEquals(listOf(LogMessage("A")), clientReceivedMessages)
    }
}