package org.jetbrains.compose.reload.utils

import java.nio.file.Path

data class ProjectDir(val path: Path, val parent: ProjectDir? = null) {
    fun subproject(name: String): ProjectDir = ProjectDir(path.resolve(name), parent = this)

    val buildGradleKts: Path get() = path.resolve("build.gradle.kts")
    val settingsGradleKts: Path get() = path.resolve("settings.gradle.kts")
    val gradleProperties: Path get() = path.resolve("gradle.properties")

    override fun toString(): String {
        return path.toString()
    }
}


fun ProjectDir.writeText(relativePath: String, text: String)  {
    val path = path.resolve(relativePath)
    path.parent.toFile().mkdirs()
    path.toFile().writeText(text)
}
