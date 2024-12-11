package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.sellmair.evas.compose.composeState
import io.sellmair.evas.compose.composeValue
import kotlinx.coroutines.delay
import org.jetbrains.compose.reload.jvm.tooling.states.ReloadState

private val Orange1 = Color(0xFFFC801D)
private val Orange2 = Color(0xFFFDB60D)
private val Green = Color(0xFF3DEA62)
private val Red = Color(0xFFFE2857)

@Composable
fun Modifier.reloadBorder(): Modifier {
    val reloadState by ReloadState.composeState()
    val reloadStateColor by animateReloadStateColor(reloadState)
    val brush = animatedReloadStateBrush()

    return this.drawWithContent {
        drawContent()

        val cornerRadius = CornerRadius(8.dp.toPx())
        val strokeWidth = Stroke(4.dp.toPx())
        drawRoundRect(
            brush = brush,
            cornerRadius = cornerRadius,
            style = strokeWidth,
        )
        drawRoundRect(
            color = reloadStateColor,
            cornerRadius = cornerRadius,
            style = strokeWidth,
        )
    }
}

@Composable
private fun animateReloadStateColor(state: ReloadState = ReloadState.composeValue()): State<Color> {
    val color = remember { Animatable(Color.LightGray) }

    LaunchedEffect(state) {
        when (state) {
            is ReloadState.Reloading -> {
                color.snapTo(Color.Transparent)
            }

            is ReloadState.Ok -> {
                color.snapTo(Green)
                delay(1000)
                color.animateTo(Color.LightGray, tween(250))
            }

            is ReloadState.Failed -> {
                color.snapTo(Red)
            }
        }
    }

    return color.asState()
}

@Composable
private fun animatedReloadStateBrush(): Brush {
    val infiniteTransition = rememberInfiniteTransition()
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing))
    )
    return Brush.linearGradient(
        colors = listOf(Orange1, Orange2),
        start = Offset(0f, gradientShift),
        end = Offset(0f, gradientShift + 400),
        tileMode = TileMode.Mirror,
    )
}
