package org.jetbrains.compose.reload.agent

import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.LogMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.LogMessage.Companion.TAG_COMPILER
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private val logger = createLogger()
private val composeBuildRoot: String? = System.getProperty("compose.build.root")
private val composeBuildProject: String? = System.getProperty("compose.build.project")
private val composeBuildCompileTask: String? = System.getProperty("compose.build.compileTask")


internal fun launchRecompiler() {
    val composeBuildRoot = composeBuildRoot ?: run {
        logger.error("Missing 'compose.build.root' property")
        return
    }

    val composeBuildProject = composeBuildProject ?: run {
        logger.error("Missing 'compose.build.project' property")
        return
    }

    val composeBuildCompileTask = composeBuildCompileTask ?: run {
        logger.error("Missing 'compose.build.compile.task' property")
        return
    }

    val port = ComposeHotReloadAgent.orchestration.port
    logger.debug("'Compose Recompiler': Using orchestration at '$port'")


    val recompilerThread = thread(name = "Compose Recompiler") {
        logger.debug("'Compose Recompiler' started")

        val gradleScriptCommand = if ("win" in System.getProperty("os.name").lowercase()) {
            arrayOf("cmd", "/c", "start", "gradlew.bat")
        } else {
            arrayOf("./gradlew")
        }

        val process = ProcessBuilder().directory(File(composeBuildRoot))
            .command(
                *gradleScriptCommand,
                createGradleCommand(composeBuildProject, composeBuildCompileTask),
                "--console=plain",
                "--no-daemon",
                "--priority=low",
                "-Dcompose.reload.orchestration.port=$port",
                //"-Dorg.gradle.debug=true",
                "-t"
            )
            .redirectErrorStream(true)
            .start()

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            logger.debug("'Compose Recompiler': Destroying process")
            process.destroy()
        })

        thread(name = "Compose Recompiler Output", isDaemon = true) {
            process.inputStream.bufferedReader().use { reader ->
                while (true) {
                    val nextLine = reader.readLine() ?: break
                    logger.debug("'Compose Recompiler' output: $nextLine")
                    LogMessage(TAG_COMPILER, nextLine).send()
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

    ComposeHotReloadAgent.orchestration.invokeWhenClosed {
        logger.debug("'Compose Recompiler': Sending close signal")
        recompilerThread.interrupt()
        recompilerThread.join()
    }
}

internal fun createGradleCommand(projectPath: String, task: String): String {
    if (projectPath == ":") return ":$task"
    return "$projectPath:$task"
}