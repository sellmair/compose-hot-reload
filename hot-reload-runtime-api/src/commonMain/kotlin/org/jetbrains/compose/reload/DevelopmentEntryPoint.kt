package org.jetbrains.compose.reload

import androidx.compose.runtime.Composable

@Composable
public expect fun DevelopmentEntryPoint(child: @Composable () -> Unit)

public annotation class DevelopmentEntryPoint(
    val windowWidth: Int = 576,
    val windowHeight: Int = 1024,
)
