package org.jetbrains.compsoe.reload.analyzer.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.evas.compose.installEvas
import io.sellmair.evas.eventsOrThrow
import io.sellmair.evas.statesOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.jetbrains.compsoe.reload.analyzer.app.states.launchFileTreeState
import org.jetbrains.compsoe.reload.analyzer.app.states.launchRuntimeTreeState
import org.jetbrains.compsoe.reload.analyzer.app.states.launchJavapState
import org.jetbrains.compsoe.reload.analyzer.app.states.launchOpenFileState
import org.jetbrains.compsoe.reload.analyzer.app.states.launchRuntimeInfoState
import org.jetbrains.compsoe.reload.analyzer.app.ui.AppTheme
import org.jetbrains.compsoe.reload.analyzer.app.ui.FileView
import org.jetbrains.compsoe.reload.analyzer.app.ui.NavigationBar

private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + Events() + States())

fun main() {
    applicationScope.launchFileTreeState()
    applicationScope.launchOpenFileState()
    applicationScope.launchJavapState()
    applicationScope.launchRuntimeInfoState()
    applicationScope.launchRuntimeTreeState()

    singleWindowApplication(
        title = "Bytecode Analyzer",
        state = WindowState(size = DpSize(1280.dp, 720.dp)),
        alwaysOnTop = true,
    ) {
        installEvas(applicationScope.coroutineContext.eventsOrThrow, applicationScope.coroutineContext.statesOrThrow) {
            DevelopmentEntryPoint {
                AppTheme {
                    Row(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                        Box(
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            NavigationBar()
                        }

                        FileView()
                    }
                }
            }
        }
    }
}