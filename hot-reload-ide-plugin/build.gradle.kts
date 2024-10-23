plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2")
        instrumentationTools()
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.kotlin")
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "org.jetbrains.compose.hotreload"
        name = "Compose Hot Reload"
        version = "1.0.0"
        description = "Compose Hot Reload Experiments"
    }
}