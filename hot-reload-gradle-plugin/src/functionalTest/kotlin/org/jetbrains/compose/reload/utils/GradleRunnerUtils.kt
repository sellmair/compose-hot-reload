package org.jetbrains.compose.reload.utils

import org.gradle.testkit.runner.GradleRunner

fun GradleRunner.addedArguments(vararg args: String): GradleRunner {
    return withArguments(this.arguments + args)
}