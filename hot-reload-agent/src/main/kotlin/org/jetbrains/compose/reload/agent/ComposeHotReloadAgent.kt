package org.jetbrains.compose.reload.agent

import javassist.ClassPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent.reloadLock
import org.jetbrains.compose.reload.orchestration.OrchestrationClient
import org.jetbrains.compose.reload.orchestration.OrchestrationHandle
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ReloadClassesRequest
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ReloadClassesRequest.ChangeType
import org.jetbrains.compose.reload.orchestration.launchOrchestrationServer
import java.lang.instrument.ClassDefinition
import java.lang.instrument.Instrumentation
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes

private val logger = createLogger()

private val agentScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

object ComposeHotReloadAgent {

    val reloadLock = ReentrantLock()

    private val beforeReloadListeners = mutableListOf<() -> Unit>()
    private val afterReloadListeners = mutableListOf<(error: Throwable?) -> Unit>()


    @Volatile
    private var instrumentation: Instrumentation? = null

    val orchestration by lazy { startOrchestration() }

    fun invokeBeforeReload(block: () -> Unit) = reloadLock.withLock {
        beforeReloadListeners.add(block)
    }

    fun invokeAfterReload(block: (error: Throwable?) -> Unit) = reloadLock.withLock {
        afterReloadListeners.add(block)
    }

    internal fun executeBeforeReloadListeners() = reloadLock.withLock {
        beforeReloadListeners.forEach { it() }
    }

    internal fun executeAfterReloadListeners(error: Throwable?) = reloadLock.withLock {
        afterReloadListeners.forEach { it(error) }
    }

    fun retryPendingChanges() {
        agentScope.launch {
            orchestration.send(ReloadClassesRequest())
        }
    }

    @JvmStatic
    fun premain(args: String?, instrumentation: Instrumentation) {
        this.instrumentation = instrumentation
        enableComposeHotReloadMode()
        launchReloadClassesRequestHandler(instrumentation)
    }
}

private fun startOrchestration(): OrchestrationHandle {
    /* Connecting to a server if we're instructed to */
    OrchestrationClient()?.let { return it }

    /* Otherwise, we start our own orchestration server */
    return agentScope.launchOrchestrationServer()
}

private fun launchReloadClassesRequestHandler(instrumentation: Instrumentation) = agentScope.launch {
    var pendingChanges = mapOf<Path, ChangeType>()

    ComposeHotReloadAgent.orchestration.receive.filterIsInstance<ReloadClassesRequest>().collect { request ->
        reloadLock.withLock {
            pendingChanges = pendingChanges + request.changedClassFiles

            ComposeHotReloadAgent.executeBeforeReloadListeners()
            val result = runCatching { reload(instrumentation, pendingChanges) }

            /*
            Yuhuu! We reloaded the classes; We can reset the 'pending changes'; No re-try necessary
             */
            if (result.isSuccess) {
                pendingChanges = emptyMap()
                resetComposeErrors()
            }

            ComposeHotReloadAgent.executeAfterReloadListeners(result.exceptionOrNull())
        }
    }
}

private fun reload(
    instrumentation: Instrumentation, pendingChanges: Map<Path, ChangeType>
) = reloadLock.withLock {
    val definitions = pendingChanges.mapNotNull { (path, change) ->
        if (change == ChangeType.Removed) {
            return@mapNotNull null
        }

        if (path.extension != "class") {
            logger.warn("$change: $path is not a class")
            return@mapNotNull null
        }

        if (!path.isRegularFile()) {
            logger.warn("$change: $path is not a regular file")
            return@mapNotNull null
        }

        logger.debug("Loading: $path")
        val code = path.readBytes()
        val clazz = ClassPool.getDefault().makeClass(code.inputStream())
        ClassDefinition(Class.forName(clazz.name), code)
    }

    instrumentation.redefineClasses(*definitions.toTypedArray())
}
