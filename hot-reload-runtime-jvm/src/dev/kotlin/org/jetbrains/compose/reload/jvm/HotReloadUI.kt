import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ReloadState


private val Orange1 = Color(0xFFFC801D)
private val Orange2 = Color(0xFFFDB60D)
private val Green = Color(0xFF3DEA62)
private val Red = Color(0xFFFE2857)

@Composable
fun TopBanner(state: ReloadState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(32.dp)) {
        val inf = rememberInfiniteTransition()
        val fl by inf.animateFloat(
            initialValue = 0f,
            targetValue = 800f,
            animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing))
        )
        val lin = Brush.linearGradient(
            colors = listOf(Orange1, Orange2),
            start = Offset(fl, 0f),
            end = Offset(fl + 400, 0f),
            tileMode = TileMode.Mirror,
        )

        val color by animateColorAsState(
            when (state) {
                ReloadState.Loading -> Color.Transparent
                ReloadState.Ready -> Green
                ReloadState.Error -> Red
            }
        )

        val height by animateDpAsState(
            if (state == ReloadState.Error) 32.dp else 8.dp,
        )

        val visState = remember { MutableTransitionState(false) }
        LaunchedEffect(state) {
            if (state == ReloadState.Ready) {
                delay(1000)
                visState.targetState = false
            } else {
                visState.targetState = true
            }
        }

        AnimatedVisibility(
            visibleState = visState,
            enter = slideInVertically(
                animationSpec = tween(50),
                initialOffsetY = { -it }
            ),
            exit = slideOutVertically(
                animationSpec = tween(200),
                targetOffsetY = { -it }
            ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .background(lin),
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .height(height)
                        .fillMaxWidth()
                        .background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedVisibility(
                        state == ReloadState.Error,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            "Compose hot reload failed, check for compilation errors",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}
