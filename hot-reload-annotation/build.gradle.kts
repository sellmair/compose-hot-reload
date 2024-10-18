import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    `maven-publish`
    `publishing-conventions`
}

kotlin {
    androidTarget()
    jvm()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
}

android {
    namespace = "org.jetbrains.compose.hotreload.annotation"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}
