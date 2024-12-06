package org.jetbrains.compose.reload.core.testFixtures

fun String.asFileName(): String = replace("""\\W+""", "_")

fun String.sanitized(): String {
    return lines().joinToString("\n").trim()
}
