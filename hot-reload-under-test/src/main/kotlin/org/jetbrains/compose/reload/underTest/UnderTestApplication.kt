@file:OptIn(ExperimentalTestApi::class)
@file:Suppress("unused") // Used by tests in test data

package org.jetbrains.compose.reload.underTest

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.hot_reload_under_test.generated.resources.Res
import org.jetbrains.compose.hot_reload_under_test.generated.resources.Roboto_Medium
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.jetbrains.compose.reload.InternalHotReloadApi
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent.orchestration
import org.jetbrains.compose.reload.jvm.runDevApplicationHeadless
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.asChannel
import org.jetbrains.compose.reload.orchestration.invokeWhenReceived
import org.jetbrains.compose.resources.Font
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import kotlin.time.Duration.Companion.minutes

internal val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())


/**
 * Handle for code written 'under test'
 * Example:
 *
 * ```kotlin
 * underTestApplication {
 *     var state by remember { mutableStateOf(0) }
 *     onTestEvent { // <- can nicely call into such APIs without additional imports.
 *         state++
 *     }
 *     Text("Before: ${d}state", fontSize = 48.sp)
 * }
 * ```
 */
class UnderTestApplication(
    val applicationScope: CoroutineScope
) {

    private val orchestrationChannel = orchestration.asChannel()

    @Composable
    fun onTestEvent(handler: suspend (payload: Any?) -> Unit) {
        LaunchedEffect(Unit) {
            orchestrationChannel.receiveAsFlow().filterIsInstance<OrchestrationMessage.TestEvent>().collect { value ->
                logger.debug("TestEvent received: $value")
                handler.invoke(value.payload)
            }
        }
    }

    /**
     * Intended to forcefully create a new Group/Scope.
     */
    @Composable
    fun Group(child: @Composable () -> Unit) {
        child()
    }

    fun sendTestEvent(any: Any? = null) {
        orchestration.sendMessage(OrchestrationMessage.TestEvent(any))
    }

    fun sendLog(any: Any?) {
        orchestration.sendMessage(OrchestrationMessage.LogMessage("test", any.toString()))
    }
}

fun invokeOnTestEvent(handler: suspend (payload: Any?) -> Unit) {
    orchestration.invokeWhenReceived<OrchestrationMessage.TestEvent> { value ->
        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            handler.invoke(value.payload)
        }
    }
}

/*
Text component which is set up to render as consistent as possible on different platforms.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun TestText(value: String, fontSize: TextUnit = 96.sp) {
    val fontFamily = FontFamily(
        Font(
            Res.font.Roboto_Medium, weight = FontWeight.Medium, style = FontStyle.Normal
        )
    )

    Text(
        text = value,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        lineHeight = fontSize,
        style = TextStyle(
            platformStyle = PlatformTextStyle(
                spanStyle = null,
                paragraphStyle = PlatformParagraphStyle(
                    fontRasterizationSettings = FontRasterizationSettings(
                        smoothing = FontSmoothing.AntiAlias,
                        hinting = FontHinting.None,
                        subpixelPositioning = true,
                        autoHintingForced = false
                    )
                )
            )
        )
    )
}

/**
 * Entry points for "Applications under test"
 */
@OptIn(InternalHotReloadApi::class)
@Suppress("unused") // Used by integration tests
fun underTestApplication(
    timeout: Int = 5,
    width: Int = 512,
    height: Int = 512,
    content: @Composable UnderTestApplication.() -> Unit
) {
    runDevApplicationHeadless(
        timeout.minutes, width = width, height = height,
    ) { applicationScope ->
        DevelopmentEntryPoint {
            UnderTestApplication(applicationScope).content()
        }
    }
}
