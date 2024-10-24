package org.jetbrains.compose.reload

import androidx.compose.runtime.Composable
import org.jetbrains.compose.reload.jvm.JvmDevelopmentEntryPoint
import org.jetbrains.compose.reload.jvm.InternalHotReloadApi

@Composable
public actual fun DevelopmentEntryPoint(child: @Composable () -> Unit) {
    @OptIn(InternalHotReloadApi::class)
    JvmDevelopmentEntryPoint(child)
}