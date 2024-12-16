package org.jetbrains.compose.reload.tests

import org.jetbrains.compose.reload.utils.*
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

class DevelopmentEntryPointTests {

    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    @TestOnlyLatestVersions
    fun `test - simple jvm project`(fixture: HotReloadTestFixture) = fixture.runTest {
        val mainKt = fixture.projectDir
            .resolve("src")
            .resolve(fixture.projectMode.fold("jvmMain", "main"))
            .resolve("kotlin/Main.kt")
            .createParentDirectories()

        val devKt = fixture.projectDir
            .resolve("src")
            .resolve(fixture.projectMode.fold("jvmDev", "dev"))
            .resolve("kotlin/Dev.kt")
            .createParentDirectories()

        mainKt.writeText(
            """
            import org.jetbrains.compose.reload.underTest.*
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.unit.sp
            
            @Composable
            fun Widget(text: String) {
                TestText("Before: " + text, fontSize = 48.sp)
            }
        """.trimIndent()
        )

        devKt.writeText(
            """
            import androidx.compose.runtime.Composable
            import org.jetbrains.compose.reload.*

            @Composable
            @DevelopmentEntryPoint
            fun DevWidget() {
                Widget("Foo")
            }
        """.trimIndent()
        )

        fixture.launchDevApplicationAndWait(className = "DevKt", funName = "DevWidget")
        fixture.checkScreenshot("0-initial")

        fixture.runTransaction {
            devKt.replaceText("Foo", "Bar")
            awaitReload()
            fixture.checkScreenshot("1-change-in-devKt")
        }

        fixture.runTransaction {
            mainKt.replaceText("Before", "After")
            awaitReload()
            fixture.checkScreenshot("2-change-in-mainKt")
        }
    }
}
