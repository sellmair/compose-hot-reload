package org.jetbrains.compose.reload.analysis

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import kotlin.collections.find
import kotlin.collections.orEmpty
import kotlin.collections.zipWithNext

internal fun AbstractInsnNode.intValueOrNull(): Int? {
    if (this is LdcInsnNode) return this.cst as? Int
    return when (opcode) {
        Opcodes.ICONST_0 -> 0
        Opcodes.ICONST_1 -> 1
        Opcodes.ICONST_2 -> 2
        Opcodes.ICONST_3 -> 3
        Opcodes.ICONST_4 -> 4
        Opcodes.ICONST_5 -> 5
        Opcodes.ICONST_M1 -> -1
        else -> null
    }
}

internal fun MethodNode.readFunctionKeyMetaAnnotation(): ComposeGroupKey? {
    val functionKey = visibleAnnotations.orEmpty().find { annotationNode ->
        annotationNode.desc == functionKeyMetaConstructorDescriptor
    }?.values?.zipWithNext()?.find { (name, _) -> name == "key" }?.second as? Int

    if (functionKey != null) {
        return ComposeGroupKey(functionKey)
    }

    return null
}

internal fun ClassNode(bytecode: ByteArray): ClassNode {
    val reader = ClassReader(bytecode)
    val node = ClassNode(ASM9)
    reader.accept(node, 0)
    return node
}

internal fun ClassId(node: ClassNode): ClassId = ClassId(node.name)

internal fun MethodId(classNode: ClassNode, methodNode: MethodNode): MethodId = MethodId(
    classId = ClassId(classNode),
    methodName = methodNode.name,
    methodDescriptor = methodNode.desc,
)

internal fun MethodId(handle: Handle) = MethodId(
    classId = ClassId(handle.owner),
    methodName = handle.name,
    methodDescriptor = handle.desc
)

internal fun MethodId(methodInsnNode: MethodInsnNode): MethodId = MethodId(
    classId = ClassId(methodInsnNode.owner),
    methodName = methodInsnNode.name,
    methodDescriptor = methodInsnNode.desc
)
