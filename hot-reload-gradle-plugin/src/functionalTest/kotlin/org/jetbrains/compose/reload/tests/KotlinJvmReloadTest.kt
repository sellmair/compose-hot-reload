package org.jetbrains.compose.reload.tests

import org.jetbrains.compose.reload.utils.DefaultSettingsGradleKts
import org.jetbrains.compose.reload.utils.HotReloadTest
import org.jetbrains.compose.reload.utils.HotReloadTestFixture
import org.jetbrains.compose.reload.utils.addedArguments
import org.jetbrains.compose.reload.utils.writeText
import kotlin.concurrent.thread
import kotlin.io.path.writeText

class KotlinJvmReloadTest {

    @HotReloadTest
    @DefaultSettingsGradleKts
    fun `test - simple change`(fixture: HotReloadTestFixture) {
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

        fixture.gradleRunner.addedArguments("wrapper", "run")
            .build()
    }
}