package org.jetbrains.compose.reload

import androidx.compose.runtime.Composable

@Composable
public actual fun DevelopmentEntryPoint(child: @Composable () -> Unit) {
    child()
}