import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform").version("2.0.21")
    kotlin("plugin.compose").version("2.0.21")
    id("org.jetbrains.compose").version("1.7.0")
    id("com.google.devtools.ksp").version("2.0.21-1.0.25")
    id("org.jetbrains.compose-hot-reload")
}

kotlin {

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Multiplatform App"
            packageVersion = "1.0.0"
            macOS {
                bundleID = "org.company.app.desktopApp"
            }
        }
    }
}
