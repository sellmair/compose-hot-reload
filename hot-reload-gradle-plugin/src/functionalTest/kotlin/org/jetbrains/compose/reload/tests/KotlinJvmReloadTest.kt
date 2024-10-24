package org.jetbrains.compose.reload.tests

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import org.gradle.api.logging.Logging
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.*
import org.jetbrains.compose.reload.utils.*
import kotlin.concurrent.thread
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail
import kotlin.time.Duration.Companion.minutes

class KotlinJvmReloadTest {

    private val logger = Logging.getLogger(this::class.java)

    @HotReloadTest
    @DefaultSettingsGradleKts
    fun `test - simple change`(fixture: HotReloadTestFixture) = runTest(timeout = 1.minutes) {
        fixture.projectDir.buildGradleKts.writeText(
            """
            import org.jetbrains.compose.reload.ComposeHotRun

            plugins {
                kotlin("jvm")
                kotlin("plugin.compose")
                id("org.jetbrains.compose")
                id("org.jetbrains.compose-hot-reload")
            }
            
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation("ch.qos.logback:logback-classic:1.5.9")
            }
            
            tasks.create<ComposeHotRun>("run") {
                mainClass.set("MainKt")
            }
            """.trimIndent()
        )

        fixture.projectDir.writeText(
            "src/main/kotlin/Main.kt", """
                import androidx.compose.material3.Text
                import androidx.compose.ui.window.*
                import org.jetbrains.compose.reload.DevelopmentEntryPoint
                
                fun main() {
                    singleWindowApplication {
                        DevelopmentEntryPoint {
                            Text("Hello")
                        }
                    }
                }
            """.trimIndent()
        )

        launchThread {
            fixture.gradleRunner
                .addedArguments("wrapper", "run")
                .build()
        }

        logger.quiet("Waiting for UI to render")
        run {
            val rendered = fixture.skipToMessage<UIRendered>()
            assertNull(rendered.reloadRequestId)
        }

        logger.quiet("Waiting for Daemon to become ready")
        fixture.skipToMessage<GradleDaemonReady>()

        logger.quiet("Modifying source code")
        run {
            fixture.projectDir.replaceText(
                "src/main/kotlin/Main.kt", """Text("Hello")""", """Text("Goodbye!")"""
            )
        }

        logger.quiet("Waiting for reload request")
        val reloadRequest = run {
            val reloadRequest = fixture.skipToMessage<ReloadClassesRequest>()
            if (reloadRequest.changedClassFiles.isEmpty()) fail("No changedClassFiles in reload request")
            if (reloadRequest.changedClassFiles.size > 1) fail("Too many changedClassFiles in reload request: ${reloadRequest.changedClassFiles}")
            val (requestedFile, changeType) = reloadRequest.changedClassFiles.entries.single()
            requestedFile.name.assertMatchesRegex(""".*MainKt.*\.class""")
            assertEquals(ReloadClassesRequest.ChangeType.Modified, changeType)
            reloadRequest
        }


        logger.quiet("Waiting for UI render")
        run {
            val rendered = fixture.skipToMessage<UIRendered>()
            assertEquals(reloadRequest.messageId, rendered.reloadRequestId)
        }
    }
}

suspend fun launchThread(block: () -> Unit) {
    val thread = thread {
        try {
            block()
        } catch (_: InterruptedException) {
            // Goodbye.
        }
    }

    currentCoroutineContext().job.invokeOnCompletion {
        thread.interrupt()
    }
}