@file:Suppress("DuplicatedCode")

package org.jetbrains.compose.reload.analysis

import org.jetbrains.compose.reload.core.withClosure
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

data class RuntimeInfo(val scopes: List<RuntimeScopeInfo>) {
    /**
     * Index from [MethodId] to the 'root scopes' belonging to this method id
     */
    val methods: Map<MethodId, List<RuntimeScopeInfo>> = scopes.groupBy { info -> info.methodId }

    /**
     * Index from [ComposeGroupKey] to *all* scopes that are associated with this group.
     * (null as key means 'no known group')
     */
    val groups: Map<ComposeGroupKey?, List<RuntimeScopeInfo>> =
        scopes.withClosure<RuntimeScopeInfo> { info -> info.children }
            .groupBy { info -> info.tree.group }
}

fun RuntimeInfo(bytecode: ByteArray): RuntimeInfo? {
    return RuntimeInfo(ClassNode(bytecode))
}

internal fun RuntimeInfo(classNode: ClassNode): RuntimeInfo? {
    if (isIgnoredClassId(classNode.name)) return null
    return RuntimeInfo(classNode.methods.mapNotNull { methodNode -> RuntimeScopeInfo(classNode, methodNode) })
}

internal fun RuntimeScopeInfo(classNode: ClassNode, methodNode: MethodNode): RuntimeScopeInfo? {
    val methodId = MethodId(classNode, methodNode)
    val runtimeInstructionTree = parseRuntimeInstructionTreeLenient(methodId, methodNode)
    return createRuntimeScopeInfo(methodId, runtimeInstructionTree)
}

/**
 * Creates a new [RuntimeInfo] by updating [this] RuntimeInfo with 'new' scopes from [other]
 */
operator fun RuntimeInfo?.plus(other: RuntimeInfo?): RuntimeInfo {
    if (this == null) return other ?: RuntimeInfo(emptyList())
    if (other == null) return this
    val newMethods = this.methods + other.methods
    val newScopes = newMethods.values.flatten()
    return RuntimeInfo(newScopes)
}

internal fun createRuntimeScopeInfo(
    methodId: MethodId,
    tree: RuntimeInstructionTree,
): RuntimeScopeInfo {
    return RuntimeScopeInfo(
        methodId = methodId,
        tree = tree,
        dependencies = tree.dependencies(),
        children = tree.children.map { child -> createRuntimeScopeInfo(methodId, child) },
        hash = tree.codeHash()
    )
}
