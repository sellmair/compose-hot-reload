package org.jetbrains.compose.reload.analysis

/**
 * The actual key emitted by the Compose Compiler associated with the group.
 * For example, a call like
 * ```kotlin
 * startRestartGroup(1902)
 *```
 *
 * will have the value '1902' recorded as [ComposeGroupKey].
 */
@JvmInline
value class ComposeGroupKey(val key: Int)

fun ComposeGroupKey(key: Int?): ComposeGroupKey? = key?.let(::ComposeGroupKey)

internal object SpecialComposeGroupKeys {
    /**
     * W/o 'OptimizeNonSkippingGroup' each call to remember is wrapped in its own group with this key.
     */
    val remember = ComposeGroupKey(1849434622)
}