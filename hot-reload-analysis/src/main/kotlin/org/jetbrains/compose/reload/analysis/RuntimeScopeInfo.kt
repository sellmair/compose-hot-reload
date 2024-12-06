package org.jetbrains.compose.reload.analysis

data class RuntimeScopeInfo(
    val methodId: MethodId,
    val tree: RuntimeInstructionTree,
    val dependencies: List<MethodId>,
    val children: List<RuntimeScopeInfo>,
    val hash: RuntimeInstructionTreeCodeHash,
)