package org.jetbrains.compose.reload.jvm

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.jetbrains.compose.reload.agent.ComposeHotReloadAgent
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private val logger = createLogger()

private val composeBuildRoot: String? = System.getProperty("compose.build.root")
private val composeBuildProject: String? = System.getProperty("compose.build.project")
private val composeBuildCompileTask: String? = System.getProperty("compose.build.compileTask")

internal suspend fun composeRecompilerApplication() {
    val recompilerWindowThread = thread(name = "Compose Recompiler Window") {
        try {
            runRecompilerApplication()
        } catch (_: InterruptedException) {

        }

        logger.debug("Compose Recompiler Window finished")
    }

    currentCoroutineContext().job.invokeOnCompletion {
        recompilerWindowThread.interrupt()
    }

    awaitCancellation()
}

private fun runRecompilerApplication() {
    singleWindowApplication(
        title = "Compose Hot Recompiler",
        state = WindowState(position = WindowPosition.Aligned(Alignment.BottomEnd)),
        exitProcessOnExit = false,
    ) {

        var outputLines by remember { mutableStateOf(listOf<String>()) }

        LaunchedEffect(Unit) {
            runGradleContinuousCompilation().collect { output ->
                outputLines = (outputLines + output).takeLast(1024)
            }
        }

        var composeLogo by remember { mutableStateOf<ImageBitmap?>(null) }


        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                runCatching {
                    composeLogo =
                        URL("https://github.com/JetBrains/compose-multiplatform/blob/master/artwork/compose-logo.png?raw=true")
                            .openStream().buffered().use { input -> loadImageBitmap(input) }
                }.onFailure {
                    logger.error("Failed loading compose-logo.png", it)
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
            Row {
                composeLogo?.let { bitmap ->
                    Image(
                        bitmap, "Compose Logo", modifier = Modifier.size(64.dp)
                    )
                }

                Column {
                    Text("Compose Application Recompiler", fontSize = 24.0f.sp, fontWeight = FontWeight.Bold)
                    Text("Save your code to recompile!", fontSize = 16.0f.sp)
                }
            }


            Row {
                Card(Modifier.padding(16.dp).fillMaxWidth()) {
                    Box(Modifier.padding(16.dp)) {
                        val listState = LazyListState(outputLines.lastIndex)

                        LazyColumn(state = listState) {
                            items(outputLines) { text ->
                                Row {
                                    Text(text, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun runGradleContinuousCompilation(): Flow<String> {
    val composeBuildRoot = composeBuildRoot ?: run {
        logger.error("Missing 'compose.build.root' property")
        return emptyFlow()
    }

    val composeBuildProject = composeBuildProject ?: run {
        logger.error("Missing 'compose.build.project' property")
        return emptyFlow()
    }

    val composeBuildCompileTask = composeBuildCompileTask ?: run {
        logger.error("Missing 'compose.build.compile.task' property")
        return emptyFlow()
    }

    val port = ComposeHotReloadAgent.orchestration.port()
    logger.debug("'Compose Recompiler': Orchestration listening on port '$port'")

    val output = MutableSharedFlow<String>(
        extraBufferCapacity = 1024,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val job = currentCoroutineContext().job
    val recompilerThread = thread(name = "Compose Recompiler") {
        logger.debug("'Compose Recompiler' started")

        val process = ProcessBuilder().directory(File(composeBuildRoot))
            .command(
                if ("win" in System.getProperty("os.name").lowercase()) "gradlew.bat" else "./gradlew",
                createGradleCommand(composeBuildProject, composeBuildCompileTask),
                "--console=plain",
                "--no-daemon",
                "--priority=low",
                "-Dcompose.reload.orchestration.port=$port",
                "-t"
            )
            .redirectErrorStream(true)
            .start()

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            process.destroy()
        })

        thread(name = "Compose Recompiler Output", isDaemon = true) {
            process.inputStream.bufferedReader().use { reader ->
                while (job.isActive) {
                    val nextLine = reader.readLine() ?: break
                    logger.debug("'Compose Recompiler' output: $nextLine")
                    output.tryEmit(nextLine)
                }
            }
        }
        try {
            process.waitFor()
        } catch (_: InterruptedException) {

        }

        logger.debug("'Compose Recompiler': Destroying process")
        process.destroy()
        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            logger.debug("'Compose Recompiler': Force destroying process")
            process.destroyForcibly()
        }

        logger.debug("'Compose Recompiler' finished")
    }

    job.invokeOnCompletion {
        logger.debug("'Compose Recompiler': Sending close signal (${job.isActive}")
        recompilerThread.interrupt()
        recompilerThread.join()
    }

    return output
}

internal fun createGradleCommand(projectPath: String, task: String): String {
    if(projectPath == ":") return ":$task"
    return "$projectPath:$task"
}