package org.jetbrains.compose.reload.analysis

import org.jetbrains.compose.reload.core.withClosure
import java.util.zip.CRC32

@JvmInline
value class RuntimeScopeInvalidationKey(val value: Long)

fun RuntimeInfo.resolveInvalidationKey(groupKey: ComposeGroupKey): RuntimeScopeInvalidationKey? {
    /*
    w/o 'OptimizeNonSkippingGroup remember {} blocks have their own dedicated group.
    This groups shall not act on their own, but are to be invalidated by their parents.
    We therefore return a stable invalidation key and later include those scopes when building
    the invalidation key for parents.
    */
    if (groupKey == SpecialComposeGroupKeys.remember) {
        return RuntimeScopeInvalidationKey(0)
    }

    val crc = CRC32()
    val scopes = groups[groupKey] ?: return null

    val scopesWithTransitiveDependencies = scopes.withClosure<RuntimeScopeInfo> { scope ->
        val dependencies = scope.dependencies.flatMap { methodId -> methods[methodId] ?: emptyList() }

        /*
        Including relevant children
         */
        val children = scope.children.filter { info ->
            /* w/o 'OptimizeNonSkippingGroup, remember blocks will have their own group */
            info.tree.group == SpecialComposeGroupKeys.remember
        }

        dependencies + children
    }

    scopesWithTransitiveDependencies.forEach { scope ->
        crc.updateLong(scope.hash.value)
    }

    return RuntimeScopeInvalidationKey(crc.value)
}
