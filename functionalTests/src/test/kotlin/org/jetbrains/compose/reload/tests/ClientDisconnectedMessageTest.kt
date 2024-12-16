package org.jetbrains.compose.reload.tests

import org.jetbrains.compose.reload.orchestration.OrchestrationClientRole
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ClientDisconnected
import org.jetbrains.compose.reload.utils.*
import kotlin.time.Duration.Companion.minutes

class ClientDisconnectedMessageTest {

    @HotReloadTest
    @TestOnlyLatestVersions
    @TestOnlyKmp
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    fun `send shutdown request - receive ClientDisconnected`(
        testFixture: HotReloadTestFixture
    ) = testFixture.runTest(timeout = 5.minutes) {
        testFixture.projectDir.writeText(
            "src/jvmMain/kotlin/Main.kt", """
            import androidx.compose.material3.*
            import org.jetbrains.compose.reload.underTest.*
            import androidx.compose.ui.unit.sp
            fun main() {
                underTestApplication {
                    TestText("Hello", fontSize = 48.sp)
                }
            }
        """.trimIndent()
        )

        testFixture.launchApplication()
        testFixture.skipToMessage<OrchestrationMessage.UIRendered>()
        testFixture.sendMessage(OrchestrationMessage.ShutdownRequest())

        while (true) {
            val disconnected = testFixture.skipToMessage<ClientDisconnected>()
            if (disconnected.clientRole == OrchestrationClientRole.Application) break
        }
    }
}
