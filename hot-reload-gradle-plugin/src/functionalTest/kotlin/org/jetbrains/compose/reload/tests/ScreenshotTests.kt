@file:Suppress("DuplicatedCode", "FunctionName")

package org.jetbrains.compose.reload.tests

import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.utils.*
import kotlin.io.path.appendLines
import kotlin.io.path.appendText

class ScreenshotTests {

    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    fun `test - simple change`(fixture: HotReloadTestFixture) = fixture.runTest {
        fixture initialSourceCode """
            import androidx.compose.foundation.layout.*
            import androidx.compose.ui.unit.sp
            import androidx.compose.ui.window.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    TestText("Hello")
                }
            }
            """.trimIndent()
        fixture.checkScreenshot("before")

        fixture.replaceSourceCodeAndReload("Hello", "Goodbye")
        fixture.checkScreenshot("after")
    }

    @HostIntegrationTest
    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    fun `test - retained state`(fixture: HotReloadTestFixture) = fixture.runTest {
        val d = "\$"
        fixture initialSourceCode """
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.*
            import androidx.compose.ui.unit.*
            import androidx.compose.ui.window.*
            import androidx.compose.runtime.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    var state by remember { mutableStateOf(0) }
                    onTestEvent {
                        state++
                    }
                    
                    Group {
                        TestText("Before: ${d}state")
                    }
                }
            }
            """.trimIndent()
        fixture.checkScreenshot("before-0")

        fixture.sendTestEvent()
        fixture.checkScreenshot("before-1")

        fixture.sendTestEvent()
        fixture.checkScreenshot("before-2")

        fixture.replaceSourceCodeAndReload("Before", "After")
        fixture.checkScreenshot("after-2")
    }

    @AndroidHotReloadTest
    @DefaultSettingsGradleKts
    @DefaultAndroidAndJvmBuildSetup
    fun `test - kmp with android and jvm`(fixture: HotReloadTestFixture) = fixture.runTest {
        fixture initialSourceCode """
            import androidx.compose.foundation.layout.*
            import androidx.compose.ui.unit.sp
            import androidx.compose.ui.window.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    TestText("Hello")
                }
            }
            """.trimIndent()
        fixture.checkScreenshot("before")

        fixture.replaceSourceCodeAndReload("Hello", "Goodbye")
        fixture.checkScreenshot("after")
    }

    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts("app", "widgets")
    fun `test - change in dependency project`(fixture: HotReloadTestFixture) = fixture.runTest {
        fixture.projectDir.settingsGradleKts.appendLines(
            listOf(
                "",
                """include(":widgets")""",
                """include(":app")"""
            )
        )

        if (fixture.projectMode == ProjectMode.Kmp) {
            fixture.projectDir.subproject("app").buildGradleKts.appendText(
                """
                kotlin {
                    sourceSets.commonMain.dependencies {
                        implementation(project(":widgets"))
                    }
                }
                """.trimIndent()
            )
        }

        if (fixture.projectMode == ProjectMode.Jvm) {
            fixture.projectDir.subproject("app").buildGradleKts.appendText(
                """
                dependencies {
                    implementation(project(":widgets"))
                }
            """.trimIndent()
            )
        }

        fixture.projectDir.writeText(
            "widgets/src/${fixture.projectMode.fold("commonMain", "main")}/kotlin/Widget.kt", """
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.unit.sp
            import org.jetbrains.compose.reload.underTest.*
            
            @Composable
            fun Widget(text: String) {
                TestText("Before: " + text)
            }
            """.trimIndent()
        )

        fixture.projectDir.writeText(
            "app/src/${fixture.projectMode.fold("commonMain", "main")}/kotlin/Main.kt", """
            import androidx.compose.foundation.layout.*
            import androidx.compose.ui.unit.sp
            import androidx.compose.ui.window.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    Widget("Hello") // <- calls into other module
                }
            }
            """.trimIndent()
        )

        fixture.launchApplicationAndWait(":app")
        fixture.checkScreenshot("before")

        fixture.replaceSourceCodeAndReload(
            "widgets/src/${fixture.projectMode.fold("commonMain", "main")}/kotlin/Widget.kt",
            "Before:", "After:"
        )
        fixture.checkScreenshot("after")
    }

    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    @TestOnlyLatestVersions
    fun `test - add button`(fixture: HotReloadTestFixture) = fixture.runTest {
        fixture initialSourceCode """
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.*
            import androidx.compose.ui.unit.*
            import androidx.compose.ui.window.*
            import androidx.compose.runtime.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    Column {
                        TestText("Initial")
                        // Add button
                    }
                }
            }
            """.trimIndent()
        fixture.checkScreenshot("0-before")

        fixture.replaceSourceCodeAndReload(
            """// Add button""", """
                Button(onClick = { }) {
                    TestText("Button")
                }
            """.trimIndent()
        )
        fixture.checkScreenshot("1-withButton")
    }


    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    @TestOnlyLatestVersions
    fun `test - add remembered state`(fixture: HotReloadTestFixture) = fixture.runTest {
        fixture initialSourceCode """
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.*
            import androidx.compose.ui.unit.*
            import androidx.compose.ui.window.*
            import androidx.compose.runtime.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    // add state
                    
                    // add effect
                    
                    Column {
                        TestText("Initial")
                        // Add button
                    }
                }
            }
            """.trimIndent()
        fixture.checkScreenshot("0-before")

        fixture.replaceSourceCodeAndReload("""// add state""", """var state by remember { mutableStateOf(0) }""")
        fixture.checkScreenshot("1-addedState")

        fixture.replaceSourceCodeAndReload(""""Initial"""", """"State: %state"""".replace("%", "$"))
        fixture.checkScreenshot("2-renderState")

        fixture.replaceSourceCodeAndReload("""// add effect""", """onTestEvent { state++ }""")
        fixture.checkScreenshot("3-addedEffect")

        fixture.sendTestEvent()
        fixture.checkScreenshot("4-afterEvent")
    }

    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    @TestOnlyLatestVersions
    fun `test - update remembered value`(fixture: HotReloadTestFixture) = fixture.runTest {
        fixture initialSourceCode """
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.*
            import androidx.compose.ui.unit.*
            import androidx.compose.ui.window.*
            import androidx.compose.runtime.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    var state by remember { mutableStateOf(0) }
                    onTestEvent { state++ }
                    Group {
                        TestText("Before: %state")
                    }
                }
            }
            """.trimIndent().replace("%", "$")
        fixture.checkScreenshot("0-before")

        fixture.sendTestEvent()
        fixture.checkScreenshot("1-afterEvent")

        fixture.replaceSourceCodeAndReload("Before:", "After:")
        fixture.checkScreenshot("2-afterSimpleCodeChange")

        fixture.replaceSourceCodeAndReload("mutableStateOf(0)", "mutableStateOf(42)")
        fixture.checkScreenshot("3-afterChangeInsideRememberBlock")
    }

    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    @TestOnlyLatestVersions
    fun `test - change lambda from non-capturing to capturing`(fixture: HotReloadTestFixture) = fixture.runTest {
        fixture initialSourceCode """
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.*
            import androidx.compose.ui.unit.*
            import androidx.compose.ui.window.*
            import androidx.compose.runtime.*
            import org.jetbrains.compose.reload.underTest.*
            
            fun main() {
                underTestApplication {
                    var state by remember { mutableStateOf(0) }
                   
                    val myLambda = {
                        // lambda body
                        sendLog("run: myLambda")
                        sendTestEvent("run: myLambda")
                    }
                    
                    onTestEvent { value ->
                        if(value == "inc") myLambda() 
                     }
                    
                    TestText("%state")
                }
            }
            """.trimIndent().replace("%", "$")
        fixture.checkScreenshot("0-before")

        fixture.sendTestEvent("inc")
        fixture.checkScreenshot("0-before")

        fixture.replaceSourceCodeAndReload("// lambda body", "state++")
        fixture.sendTestEvent("inc")

        fixture.skipToMessage<OrchestrationMessage.TestEvent> { event ->
            event.payload == "run: myLambda"
        }

        fixture.checkScreenshot("1-afterLambdaEngaged")
    }

    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    @TestOnlyLatestVersions
    fun `test - add top level value`(fixture: HotReloadTestFixture) = fixture.runTest {
        fixture initialSourceCode """
            import androidx.compose.material3.*
            import androidx.compose.ui.unit.*
            import org.jetbrains.compose.reload.underTest.*
            
            val foo = 42
            // add field
            
            fun main() {
                underTestApplication {
                   TestText("%foo")
                }
            }
            """.trimIndent().replace("%", "$")
        fixture.checkScreenshot("0-before")

        fixture.replaceSourceCode("// add field", "val bar = 1902")
        fixture.replaceSourceCode("\$foo", "\$bar")
        fixture.awaitSourceCodeReloaded()

        fixture.checkScreenshot("1-after")
    }

    @HotReloadTest
    @DefaultSettingsGradleKts
    @DefaultBuildGradleKts
    @TestOnlyLatestVersions
    fun `test - changing spacedBy`(fixture: HotReloadTestFixture) = fixture.runTest {
        fixture initialSourceCode """
            import androidx.compose.foundation.layout.Arrangement
            import androidx.compose.foundation.layout.Row
            import androidx.compose.foundation.layout.fillMaxWidth
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp
            import org.jetbrains.compose.reload.underTest.*


            fun main() {
                underTestApplication {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp) ,
                    ) {
                        TestText("A")
                        TestText("B")
                    }
                }
            }

            """.trimIndent().replace("%", "$")
        fixture.checkScreenshot("0-before")

        /* Increase value passed to 'spacedBy' */
        fixture.replaceSourceCodeAndReload("spacedBy(6.dp)", "spacedBy(32.dp)")
        fixture.checkScreenshot("1-larger-spacedBy")
    }
}
