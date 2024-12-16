package org.jetbrains.compose.reload.utils

import kotlinx.coroutines.launch
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.RecompilerReady
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.UIRendered

suspend fun HotReloadTestFixture.launchDevApplicationAndWait(
    projectPath: String = ":",
    className: String,
    funName: String
) = runTransaction { launchDevApplicationAndWait(projectPath, className, funName) }

fun HotReloadTestFixture.launchDevApplication(
    projectPath: String = ":",
    className: String,
    funName: String
) {
    daemonTestScope.launch {
        val runTaskPath = buildString {
            if (projectPath != ":") {
                append(projectPath)
            }

            append(":")
            append("devRun")
        }

        gradleRunner.build(runTaskPath, "-DclassName=$className", "-DfunName=$funName")
    }
}
