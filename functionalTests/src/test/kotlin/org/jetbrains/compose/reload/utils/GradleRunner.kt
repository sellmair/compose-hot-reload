package org.jetbrains.compose.reload.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import org.jetbrains.compose.reload.core.createLogger
import org.jetbrains.compose.reload.core.testFixtures.repositoryRoot
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.utils.GradleRunner.ExitCode
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.*
import kotlin.test.fail

private val logger = createLogger()

data class GradleRunner(
    val projectRoot: Path,
    val gradleVersion: String,
    val arguments: List<String> = emptyList(),
    val gradleHome: Path = Path("build/gradleHome"),
) {
    @JvmInline
    value class ExitCode(val value: Int)
}

fun ExitCode?.assertSuccess() {
    if (this == null) fail("Expected successful execution; No exit code received")
    if (value != 0) fail("Expected successful execution; Exit code: $value")
}

suspend fun GradleRunner.build(vararg args: String): ExitCode? {
    gradleHome.createDirectories()
    createWrapperPropertiesFile()
    copyGradleWrapper()

    val gradleScriptCommand = if ("win" in System.getProperty("os.name").lowercase()) {
        arrayOf("cmd", "/c", "start", "gradlew.bat")
    } else {
        arrayOf("./gradlew")
    }

    val processBuilder = ProcessBuilder().directory(projectRoot.toFile()).command(
        *gradleScriptCommand,
        "-Dgradle.user.home=${gradleHome.absolutePathString()}",
        "--no-daemon",
        *arguments.toTypedArray(),
        *args,
    )

    val exitCode = CompletableDeferred<ExitCode?>()

    val thread = thread(name = "Gradle Runner") {
        val process = processBuilder.start()

        thread(name = "Gradle Runner Output Reader") {
            process.inputStream.bufferedReader().use { reader ->
                while (true) {
                    logger.trace(reader.readLine() ?: break)
                }
            }
        }

        val shutdownHook = Thread {
            process.destroy()
        }

        Runtime.getRuntime().addShutdownHook(shutdownHook)

        try {
            exitCode.complete(ExitCode(process.waitFor()))
        } catch (_: InterruptedException) {
            process.destroy()
            exitCode.complete(null)
        }

        Runtime.getRuntime().removeShutdownHook(shutdownHook)
    }

    currentCoroutineContext().job.invokeOnCompletion {
        thread.interrupt()
    }

    return exitCode.await()
}

private fun GradleRunner.createWrapperPropertiesFile() {
    projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties").createParentDirectories().writeText(
        """
        distributionBase=GRADLE_USER_HOME
        distributionPath=wrapper/dists
        distributionUrl=https\://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip
        networkTimeout=10000
        validateDistributionUrl=true
        zipStoreBase=GRADLE_USER_HOME
        zipStorePath=wrapper/dists
    """.trimIndent()
    )
}


private fun GradleRunner.copyGradleWrapper() {
    projectRoot.createDirectories()
    repositoryRoot.resolve("gradlew").copyTo(projectRoot.resolve("gradlew"), overwrite = true)
    repositoryRoot.resolve("gradlew.bat").copyTo(projectRoot.resolve("gradlew.bat"), overwrite = true)
    repositoryRoot.resolve("gradle/wrapper/gradle-wrapper.jar")
        .copyTo(projectRoot.resolve("gradle/wrapper/gradle-wrapper.jar").createParentDirectories(), overwrite = true)
}
