package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import io.sellmair.evas.compose.composeValue
import kotlinx.coroutines.delay
import org.jetbrains.compose.reload.jvm.tooling.states.ReloadState
import kotlin.time.Duration.Companion.milliseconds

private val DevToolingSidecarShape = RoundedCornerShape(8.dp)

@Composable
fun ApplicationScope.DevToolingSidecar(windowState: WindowState) {
    val animationDuration = 512
    var isExpanded by remember { mutableStateOf(false) }
    var sideCarWidth by remember { mutableStateOf(if (isExpanded) 512.dp else 64.dp) }

    if (isExpanded) {
        sideCarWidth = 512.dp
    }

    if (!isExpanded) {
        LaunchedEffect(Unit) {
            delay(animationDuration.milliseconds)
            sideCarWidth = 64.dp
        }
    }

    val targetX = windowState.position.x - sideCarWidth.value.dp
    val targetY = windowState.position.y

    val xAnimatable = remember { Animatable(targetX.value) }
    val yAnimatable = remember { Animatable(targetY.value) }

    val x by xAnimatable.asState()
    val y by yAnimatable.asState()

    val height by animateDpAsState(targetValue = windowState.size.height)

    LaunchedEffect(windowState.position) {
        xAnimatable.animateTo(targetX.value)
    }

    LaunchedEffect(windowState.position) {
        yAnimatable.animateTo(targetY.value)
    }

    LaunchedEffect(sideCarWidth) {
        xAnimatable.snapTo(targetX.value)
        yAnimatable.snapTo(targetY.value)
    }

    val sidecarWindowState = WindowState(
        width = sideCarWidth, height = height,
        position = WindowPosition(x = x.dp, y = y.dp)
    )

    Window(
        onCloseRequest = {},
        state = sidecarWindowState,
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = true,
        alwaysOnTop = true
    ) {

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.End,
        ) {
            AnimatedContent(
                isExpanded,
                modifier = Modifier
                    .heightIn(max = windowState.size.height)
                    .reloadBorder(
                        shape = DevToolingSidecarShape,
                        idleColor = if (isExpanded) Color.LightGray else Color.Transparent
                    )
                    .clip(DevToolingSidecarShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .reloadBackground(if (isExpanded) Color.LightGray else reloadColorOk)
                    .weight(1f, fill = false),
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 128)) +
                            scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 128)))
                        .togetherWith(fadeOut(animationSpec = tween(90)))
                },
                contentAlignment = Alignment.TopEnd,
            ) { expandedState ->
                if (!expandedState) {
                        IconButton(
                            onClick = { isExpanded = true },
                            modifier = Modifier
                                .animateEnterExit(
                                    enter = fadeIn(tween(220)),
                                    exit = fadeOut(tween(50)))
                                .size(32.dp)
                        ) {
                            ComposeLogo(Modifier.size(24.dp))
                        }

                } else {
                    Column {
                        DevToolingToolbar({ isExpanded = false })
                        DevToolingWidget(Modifier.padding(8.dp).fillMaxSize())
                    }
                }
            }

            ReloadStateBanner(
                ReloadState.composeValue(),
                modifier = Modifier.fillMaxHeight()
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun DevToolingToolbar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ComposeLogo(modifier = Modifier.size(32.dp))
        Text("Save your code to recompile!", fontSize = 16.0f.sp)
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .padding(2.dp)
                .size(24.dp)
        ) {
            Icon(
                Icons.Default.Close, contentDescription = "Close",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
