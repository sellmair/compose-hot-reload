package org.jetbrains.compose.reload.jvm

import androidx.compose.runtime.Composable

@Composable
@InternalHotReloadApi
public fun JvmDevelopmentEntryPoint(child: @Composable () -> Unit) {
    child()
}

@RequiresOptIn("Internal API: Do not use!", RequiresOptIn.Level.ERROR)
public annotation class InternalHotReloadApi