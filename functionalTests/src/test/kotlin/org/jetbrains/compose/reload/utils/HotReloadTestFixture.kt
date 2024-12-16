@file:OptIn(ExperimentalPathApi::class)

package org.jetbrains.compose.reload.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.test.TestScope
import org.jetbrains.compose.reload.core.testFixtures.CompilerOption
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationServer
import org.jetbrains.compose.reload.orchestration.asChannel
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


@TransactionDslMarker
class HotReloadTestFixture(
    val testClassName: String,
    val testMethodName: String,
    val projectDir: ProjectDir,
    val gradleRunner: GradleRunner,
    val orchestration: OrchestrationServer,
    val projectMode: ProjectMode,
    val compilerOptions: Map<CompilerOption, Boolean>,
    val isDebug: Boolean
) : AutoCloseable {

    suspend fun <T> runTransaction(
        block: suspend TransactionScope.() -> T
    ): T {
        return coroutineScope {
            val scope = TransactionScope(this@HotReloadTestFixture, this@coroutineScope, createReceiveChannel())
            scope.block()
        }
    }

    suspend fun createReceiveChannel(): ReceiveChannel<OrchestrationMessage> {
        val channel = orchestration.asChannel()
        currentCoroutineContext().job.invokeOnCompletion { channel.cancel() }
        return channel
    }

    suspend fun <T> sendMessage(
        message: OrchestrationMessage,
        transaction: suspend TransactionScope.() -> T
    ): T {
        return runTransaction {
            message.send()
            transaction()
        }
    }

    lateinit var testScope: TestScope
        private set

    /**
     * Coroutines launched in this scope will not keep the 'runTest' blocking.
     * This scope will be canceled after the 'runTest' finished (e.g., useful for launching 'Daemon Coroutines)
     */
    lateinit var daemonTestScope: CoroutineScope

    fun runTest(timeout: Duration = 15.minutes, test: suspend HotReloadTestFixture.() -> Unit) {
        kotlinx.coroutines.test.runTest(timeout = if (isDebug) 24.hours else timeout) {
            testScope = this
            daemonTestScope = CoroutineScope(currentCoroutineContext() + Job(currentCoroutineContext().job))
            try {
                test()
            } finally {
                daemonTestScope.cancel()
                daemonTestScope.coroutineContext[Job]?.join()
            }
        }
    }

    private val resourcesLock = ReentrantLock()
    private val resources = mutableListOf<AutoCloseable>()

    override fun close() {
        orchestration.sendMessage(OrchestrationMessage.ShutdownRequest()).get()
        orchestration.closeGracefully().get()

        testScope.cancel()
        daemonTestScope.cancel()

        /* Kludge: Windows tests failed to delete the project dir (maybe some files are still in use?) */
        run deleteProjectDir@{
            repeat(10) {
                runCatching { projectDir.path.deleteRecursively() }
                    .onSuccess { return@deleteProjectDir }
            }
        }

        resourcesLock.withLock {
            resources.forEach { resource -> resource.close() }
            resources.clear()
        }
    }
}
