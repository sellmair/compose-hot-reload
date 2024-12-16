package org.jetbrains.compose.reload.tests.gradle

import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.RecompileRequest
import org.jetbrains.compose.reload.orchestration.asChannel
import org.jetbrains.compose.reload.utils.*
import kotlin.io.path.appendLines
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.test.assertEquals

class NonContinuousRecompileTest {
    @HotReloadTest
    @TestOnlyLatestVersions
    @TestOnlyDefaultCompilerOptions
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    fun `test - non continuous build`(fixture: HotReloadTestFixture) = fixture.runTest {
        fixture.projectDir.resolve("gradle.properties")
            .createParentDirectories().createFile()
            .appendLines(listOf("compose.build.continuous=false"))

        fixture initialSourceCode """
            import androidx.compose.foundation.layout.*
            import androidx.compose.ui.unit.sp
            import androidx.compose.ui.window.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    TestText("Before")
                }
            }
            """.trimIndent()
        fixture.checkScreenshot("0-before")

        fixture.replaceSourceCode("Before", "After")
        fixture.checkScreenshot("1-beforeRequest")

        val recompileRequest = RecompileRequest()
        val recompileResultChannel = fixture.orchestration.asChannel()
        val uiRenderedChannel = fixture.orchestration.asChannel()

        fixture.sendMessage(recompileRequest)

        val result = fixture.skipToMessage<OrchestrationMessage.RecompileResult>(recompileResultChannel) {
            it.recompileRequestId == recompileRequest.messageId
        }
        assertEquals(0, result.exitCode)

        fixture.skipToMessage<OrchestrationMessage.ReloadClassesRequest>(uiRenderedChannel)
        fixture.skipToMessage<OrchestrationMessage.UIRendered>(uiRenderedChannel)
        fixture.checkScreenshot("2-afterRecompile")
    }
}
