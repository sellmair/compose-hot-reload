@file:JvmName("JvmDevelopmentEntryPoint")

package org.jetbrains.compose.reload.jvm

import TopBanner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.reload.InternalHotReloadApi
import org.jetbrains.compose.reload.agent.orchestration
import org.jetbrains.compose.reload.agent.send
import org.jetbrains.compose.reload.core.createLogger
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage

private val logger = createLogger()


@Composable
@PublishedApi
@InternalHotReloadApi
internal fun JvmDevelopmentEntryPoint(child: @Composable () -> Unit) {
    /* Checking if we're currently in the stack of a hot reload */
    if (hotReloadStateLocal.current != null) {
        child()
        return
    }


    /* Wrap the child to get access to exceptions, which we can forward into the orchestration */
    val interceptedChild: @Composable () -> Unit = {
        runCatching { child() }.onFailure { exception ->
            logger.orchestration("Failed invoking 'JvmDevelopmentEntryPoint':", exception)

            /*
            UI-Exception: Nuke state captured in the UI by incrementing the key
             */
            hotReloadState.update { state -> state.copy(key = state.key + 1, error = exception) }

            OrchestrationMessage.UIException(
                message = exception.message,
                stacktrace = exception.stackTrace.toList()
            ).send()

        }.getOrThrow()
    }

    /* Agent */
    val hotReloadState by hotReloadState.collectAsState()
    val uiState by hotReloadUiState.collectAsState()

    CompositionLocalProvider(hotReloadStateLocal provides hotReloadState) {
        key(hotReloadState.key) {
            logger.orchestration("Composing UI: $hotReloadState")
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            interceptedChild()
            TopBanner(uiState, modifier = Modifier.align(Alignment.TopCenter))
        }
    }

    /* Notify orchestration about the UI being rendered */
    OrchestrationMessage.UIRendered(hotReloadState.reloadRequestId, hotReloadState.iteration).send()
}
