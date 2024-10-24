package org.jetbrains.compose.reload.jvm

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.singleWindowApplication

@Composable
@InternalHotReloadApi
public fun JvmDevelopmentEntryPoint(child: @Composable () -> Unit) {
    HotReload { child() }
}

@RequiresOptIn("Internal API: Do not use!", RequiresOptIn.Level.ERROR)
annotation class InternalHotReloadApi