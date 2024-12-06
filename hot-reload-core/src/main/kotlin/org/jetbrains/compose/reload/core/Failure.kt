package org.jetbrains.compose.reload.core

public data class Failure(val message: String?, val throwable: Throwable? = null)