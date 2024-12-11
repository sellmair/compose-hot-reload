package org.jetbrains.compose.reload.jvm.tooling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.LogMessage

@Composable
internal fun DevToolingWidget(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        DevToolingToolbar()

        val consoleWeight = Modifier.weight(1f, fill = false)
        DevToolingConsole(LogMessage.TAG_COMPILER, consoleWeight)
        DevToolingConsole(LogMessage.TAG_AGENT, consoleWeight)
        DevToolingConsole(LogMessage.TAG_RUNTIME, consoleWeight)
    }
}
