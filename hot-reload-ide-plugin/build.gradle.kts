plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("org.jetbrains.intellij.platform")
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2")
        instrumentationTools()
    }
    implementation(compose.desktop.currentOs)
}

intellijPlatform {
    pluginConfiguration {
        id = "org.jetbrains.compose.hotreload"
        name = "Compose Hot Reload"
        version = "1.0.0"
        description = "Compose Hot Reload Experiments"
    }
}