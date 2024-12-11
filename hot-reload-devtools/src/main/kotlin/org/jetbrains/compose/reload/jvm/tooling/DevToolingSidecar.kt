package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val DevToolingSidecarShape = RoundedCornerShape(8.dp)

@Composable
fun ApplicationScope.DevToolingSidecar(windowState: WindowState) {
    val animationDuration = 256
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

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.End,
        ) {
            AnimatedContent(
                isExpanded,
                modifier = Modifier
                    .heightIn(max = windowState.size.height)
                    .padding(8.dp)
                    .reloadBorder()
                    .clip(DevToolingSidecarShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(4.dp)
            ) { expandedState ->
                if (!expandedState) {
                    IconButton(
                        onClick = { isExpanded = true },
                        modifier = Modifier
                            .padding(2.dp)
                            .size(24.dp)
                    ) {
                        ComposeLogo(Modifier.fillMaxSize())
                    }
                } else {
                    Column {
                        DevToolingToolbar({ isExpanded = false })
                        DevToolingWidget(Modifier.padding(8.dp).fillMaxSize())
                    }
                }
            }
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
