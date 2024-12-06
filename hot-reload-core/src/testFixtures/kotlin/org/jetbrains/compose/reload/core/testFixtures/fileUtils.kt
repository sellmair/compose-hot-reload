package org.jetbrains.compose.reload.core.testFixtures

fun String.asFileName(): String = replace("""\\W+""", "_")