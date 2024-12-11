package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.evas.compose.composeValue
import org.jetbrains.compose.reload.jvm.tooling.states.ConsoleLogState

@Composable
fun DevToolingConsole(tag: String, modifier: Modifier) {
    val listState = rememberLazyListState()
    var isExpanded by remember { mutableStateOf(true) }
    val logState = ConsoleLogState.Key(tag).composeValue()

    LaunchedEffect(logState) {
        if (logState.logs.isEmpty()) return@LaunchedEffect
        listState.scrollToItem(logState.logs.lastIndex)
    }

    Column(modifier = modifier) {
        ExpandButton(
            isExpanded = isExpanded,
            onClick = { expanded -> isExpanded = expanded },
        ) {
            Text(tag, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        AnimatedVisibility(isExpanded) {
            Card(Modifier.padding(8.dp).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(8.dp).wrapContentHeight()
                ) {
                    items(logState.logs) { text ->
                        Text(text, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandButton(
    isExpanded: Boolean,
    onClick: (expanded: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    children: @Composable RowScope.() -> Unit
) {
    val rotation by animateFloatAsState(if (isExpanded) 0f else -90f)

    Row(
        modifier = modifier
            .clickable(onClick = { onClick(!isExpanded) }),
        verticalAlignment = CenterVertically,
    ) {
        Icon(
            Icons.Default.ArrowDropDown, contentDescription = null,
            modifier = Modifier.rotate(rotation),
        )
        children()
    }
}
