package org.jetbrains.compose.reload.tests.gradle

import org.jetbrains.compose.reload.orchestration.OrchestrationClientRole
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.UIRendered
import org.jetbrains.compose.reload.utils.*
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class PidFileTest {
    @HotReloadTest
    @TestOnlyDefaultCompilerOptions
    @TestOnlyLatestVersions
    @DefaultBuildGradleKts
    @DefaultSettingsGradleKts
    fun `test - pid file`(fixture: HotReloadTestFixture) = fixture.runTest {
        fixture initialSourceCode """
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                }
            }
        """.trimIndent()

        fixture.skipToMessage<UIRendered>()
        val pidFile = fixture.projectDir.resolve("build/run/jvmMain/jvmRun.pid")
        assertTrue(pidFile.exists(), "Expected .pid file to exist: ${pidFile.toUri()}")

        val properties = Properties()
        properties.load(pidFile.readText().reader())


        val pid = properties.getProperty("pid")
        if (pid.toLongOrNull() == null) {
            fail("Invalid pid value: $pid")
        }

        val orchestrationPort = properties.getProperty("orchestration.port")
        if (orchestrationPort.toIntOrNull() == null) {
            fail("Invalid orchestration port value: $orchestrationPort")
        }
        assertEquals(fixture.orchestration.port, orchestrationPort.toInt())

        fixture.sendMessage(OrchestrationMessage.ShutdownRequest())
        fixture.skipToMessage<OrchestrationMessage.ClientDisconnected> { message ->
            message.clientRole == OrchestrationClientRole.Application
        }

        if (pidFile.exists()) fail(
            "Expected .pid file to be deleted: ${pidFile.toUri()}"
        )
    }
}
