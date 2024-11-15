package org.jetbrains.compose.reload.jvm

internal object HotReloadEnvironment {
    val isHeadless = System.getProperty("compose.reload.headless")?.toBoolean() == true
    val showDevToolWindow = System.getProperty("compose.reload.showDevToolWindow")?.toBoolean() == true
}