package org.jetbrains.compose.reload.utils

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

enum class TestedGradleVersion(val version: GradleVersion) {
    G_8_7(GradleVersion.version("8.7")),
    G_8_10(GradleVersion.version("8.10.2"));

    override fun toString(): String {
        return version.version
    }
}

enum class TestedKotlinVersion(val version: KotlinToolingVersion) {
    KT_2_0(KotlinToolingVersion("2.0.21")),
    KT_2_1(KotlinToolingVersion("2.1.0-Beta2")), ;

    override fun toString(): String {
        return version.toString()
    }
}

enum class TestedComposeVersion(val version: String) {
    C_1_7_0(KotlinToolingVersion("1.7.0").toString()), ;

    override fun toString(): String {
        return version
    }
}

data class TestedVersions(
    val gradle: TestedGradleVersion,
    val kotlin: TestedKotlinVersion,
    val compose: TestedComposeVersion
)