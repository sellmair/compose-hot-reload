package org.jetbrains.compose.reload.core.testFixtures

import java.nio.file.Path
import kotlin.io.path.Path

fun String.sanitized(): String {
    return lines().joinToString("\n").trim()
}

val repositoryRoot: Path by lazy {
    Path(System.getProperty("repo.path") ?: error("Missing 'repo.path' system property"))
}
