@file:OptIn(ExperimentalCoroutinesApi::class)
@file:JvmName("DevApplicationHeadless")

package org.jetbrains.compose.reload.jvm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.compose.reload.InternalHotReloadApi
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import org.jetbrains.compose.reload.core.createLogger
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ShutdownRequest
import org.jetbrains.compose.reload.orchestration.asChannel
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Runs a 'headless' compose application which will participate in the orchestration.
 * This method will block until the application is finished.
 */
@InternalHotReloadApi
fun runDevApplicationHeadless(
    timeout: Duration,
    width: Int, height: Int,
    content: @Composable (applicationScope: CoroutineScope) -> Unit
) {
    val logger = createLogger()
    val applicationScope = CoroutineScope(Dispatchers.Main + Job())
    val orchestration = ComposeHotReloadAgent.orchestration
    val messages = orchestration.asChannel()

    val scene = ImageComposeScene(width, height, coroutineContext = applicationScope.coroutineContext)
    scene.setContent {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            content(applicationScope)
        }
    }

    applicationScope.launch {
        delay(timeout)
        logger.error("Application timed out...")
        exitProcess(-1)
    }

    /* Main loop */
    applicationScope.launch {
        var time = 0L
        val delay = 128.milliseconds
        while (isActive) {
            val render = scene.render(time)
            while (isActive) {
                if (scene.hasInvalidations()) break
                val message = messages.tryReceive().getOrNull() ?: break

                if (message !is OrchestrationMessage.Ack) {
                    orchestration.sendMessage(OrchestrationMessage.Ack(message.messageId)).get()
                }

                /* Break out for TestEvents and give the main thread time to handle that */
                if (message is OrchestrationMessage.TestEvent) {
                    yield()
                    break
                }

                if (message is OrchestrationMessage.TakeScreenshotRequest) {
                    val baos = ByteArrayOutputStream()
                    ImageIO.write(render.toComposeImageBitmap().toAwtImage(), "png", baos)
                    orchestration.sendMessage(OrchestrationMessage.Screenshot("png", baos.toByteArray())).get()
                    logger.debug("Screenshot sent")
                }

                if (message is ShutdownRequest) {
                    applicationScope.coroutineContext.job.cancelChildren()
                    return@launch
                }
            }

            delay(delay)
            time += delay.inWholeNanoseconds
        }
    }.asCompletableFuture().join()
}
