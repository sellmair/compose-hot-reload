package org.jetbrains.compose.reload.analysis

data class MethodId(
    val classId: ClassId,
    val methodName: String,
    val methodDescriptor: String,
) {
    override fun toString(): String {
        return "$classId.$methodName $methodDescriptor"
    }
}