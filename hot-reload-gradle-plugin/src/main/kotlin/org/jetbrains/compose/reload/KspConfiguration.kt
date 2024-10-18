package org.jetbrains.compose.reload

import org.gradle.api.Project
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

internal fun Project.configureKsp() {
    val kotlin = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return
    val commonMainSourceSet = kotlin.sourceSets.getByName("commonMain")
    commonMainSourceSet.dependencies {
        implementation("org.jetbrains.compose:hot-reload-annotation:$HOT_RELOAD_VERSION")
    }

    plugins.withId("com.google.devtools.ksp") {
        kotlin.targets.all {
            val target = this
            if (target.platformType == KotlinPlatformType.common) return@all
            dependencies.add(
                "ksp${target.name.uppercaseFirstChar()}",
                "org.jetbrains.compose:hot-reload-ksp:$HOT_RELOAD_VERSION"
            )
        }
    }
}