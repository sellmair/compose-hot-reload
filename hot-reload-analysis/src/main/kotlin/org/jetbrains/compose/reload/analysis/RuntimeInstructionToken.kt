package org.jetbrains.compose.reload.analysis

import org.jetbrains.compose.reload.analysis.RuntimeInstructionToken.EndReplaceGroup
import org.jetbrains.compose.reload.analysis.RuntimeInstructionToken.ReturnToken
import org.jetbrains.compose.reload.analysis.RuntimeInstructionToken.StartReplaceGroup
import org.jetbrains.compose.reload.analysis.RuntimeInstructionToken.StartRestartGroup
import org.jetbrains.compose.reload.analysis.RuntimeInstructionTokenizer.TokenizerContext
import org.jetbrains.compose.reload.core.Either
import org.jetbrains.compose.reload.core.Failure
import org.jetbrains.compose.reload.core.leftOr
import org.jetbrains.compose.reload.core.toLeft
import org.jetbrains.compose.reload.core.toRight
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode


sealed class RuntimeInstructionToken {
    abstract val instructions: List<AbstractInsnNode>

    data class StartRestartGroup(
        val key: ComposeGroupKey,
        override val instructions: List<AbstractInsnNode>
    ) : RuntimeInstructionToken() {
        override fun toString(): String {
            return "StartRestartGroup(key=$key)"
        }
    }

    data class EndRestartGroup(
        override val instructions: List<AbstractInsnNode>
    ) : RuntimeInstructionToken() {
        override fun toString(): String {
            return "EndRestartGroup()"
        }
    }

    data class StartReplaceGroup(
        val key: ComposeGroupKey,
        override val instructions: List<AbstractInsnNode>
    ) : RuntimeInstructionToken() {
        override fun toString(): String {
            return "StartReplaceGroup(key=$key)"
        }
    }

    data class EndReplaceGroup(
        override val instructions: List<AbstractInsnNode>
    ) : RuntimeInstructionToken() {
        override fun toString(): String {
            return "EndReplaceGroup()"
        }
    }

    data class JumpToken(
        val jumpInsn: JumpInsnNode
    ) : RuntimeInstructionToken() {
        override val instructions = listOf(jumpInsn)
        override fun toString(): String {
            return "JumpToken(jumpInsn=${jumpInsn.opcode})"
        }
    }

    data class ReturnToken(
        val returnInsn: AbstractInsnNode
    ) : RuntimeInstructionToken() {
        override val instructions = listOf(returnInsn)
        override fun toString(): String {
            return "ReturnToken(returnInsn=${returnInsn.opcode})"
        }
    }

    data class LabelToken(
        val labelInsn: LabelNode
    ) : RuntimeInstructionToken() {
        override val instructions: List<AbstractInsnNode> = listOf(labelInsn)
        override fun toString(): String {
            return "LabelToken(label=${labelInsn.label})"
        }
    }

    data class BockToken(
        override val instructions: List<AbstractInsnNode>
    ) : RuntimeInstructionToken() {
        override fun toString(): String {
            return "BlockToken(${instructions.size})"
        }
    }
}

internal sealed class RuntimeInstructionTokenizer {
    data class TokenizerContext(
        val instructions: List<AbstractInsnNode>,
        val index: Int = 0,
    ) {
        fun skip(count: Int = 1): TokenizerContext? {
            val newIndex = index + count
            if (newIndex >= instructions.size) return null
            return TokenizerContext(instructions, newIndex)
        }

        operator fun get(i: Int) = instructions.getOrNull(index + i)
    }


    abstract fun nextToken(context: TokenizerContext): Either<RuntimeInstructionToken, Failure>?
}


private class CompositeInstructionTokenizer(
    val children: List<RuntimeInstructionTokenizer>
) : RuntimeInstructionTokenizer() {
    constructor(vararg children: RuntimeInstructionTokenizer) : this(children.toList())

    override fun nextToken(context: TokenizerContext): Either<RuntimeInstructionToken, Failure>? {
        return children.firstNotNullOfOrNull { tokenizer -> tokenizer.nextToken(context) }
    }
}

private class SingleInstructionTokenizer(
    private val token: (instruction: AbstractInsnNode) -> RuntimeInstructionToken?
) : RuntimeInstructionTokenizer() {
    override fun nextToken(context: TokenizerContext): Either<RuntimeInstructionToken, Failure>? {
        val instruction = context[0] ?: return null
        return (token(instruction) ?: return null).toLeft()
    }
}

private val priorityTokenizer by lazy {
    CompositeInstructionTokenizer(
        LabelTokenizer,
        JumpTokenizer,
        ReturnTokenizer,
        StartRestartGroupTokenizer,
        EndRestartGroupTokenizer,
        StartReplaceGroupTokenizer,
        EndReplaceGroupTokenizer,
    )
}

private val tokenizer by lazy {
    CompositeInstructionTokenizer(
        priorityTokenizer,
        BlockTokenizer
    )
}

private val LabelTokenizer = SingleInstructionTokenizer({ instruction ->
    if (instruction is LabelNode) {
        RuntimeInstructionToken.LabelToken(instruction)
    } else null
})

private val JumpTokenizer = SingleInstructionTokenizer({ instruction ->
    if (instruction is JumpInsnNode) {
        RuntimeInstructionToken.JumpToken(instruction)
    } else null
})

private val ReturnTokenizer = SingleInstructionTokenizer({ instruction ->
    when (instruction.opcode) {
        Opcodes.RETURN -> ReturnToken(instruction)
        Opcodes.ARETURN -> ReturnToken(instruction)
        Opcodes.IRETURN -> ReturnToken(instruction)
        Opcodes.LRETURN -> ReturnToken(instruction)
        Opcodes.FRETURN -> ReturnToken(instruction)
        Opcodes.DRETURN -> ReturnToken(instruction)
        else -> null
    }
})

private object StartRestartGroupTokenizer : RuntimeInstructionTokenizer() {
    override fun nextToken(context: TokenizerContext): Either<RuntimeInstructionToken, Failure>? {
        val expectedLdc = context[0] ?: return null
        val expectedMethodInsn = context[1] ?: return null

        if (expectedMethodInsn is MethodInsnNode &&
            MethodId(expectedMethodInsn) == MethodIds.Composer.startRestartGroup
        ) {
            val keyValue = expectedLdc.intValueOrNull() ?: return Failure(
                "Failed parsing startRestartGroup token: expected key value"
            ).toRight()

            return StartRestartGroup(ComposeGroupKey(keyValue), listOf(expectedLdc, expectedMethodInsn)).toLeft()

        }

        return null
    }
}

private object EndRestartGroupTokenizer : RuntimeInstructionTokenizer() {
    override fun nextToken(context: TokenizerContext): Either<RuntimeInstructionToken, Failure>? {
        val expectedMethodIns = context[0] as? MethodInsnNode ?: return null
        if (MethodId(expectedMethodIns) == MethodIds.Composer.endRestartGroup) {
            return RuntimeInstructionToken.EndRestartGroup(listOf(expectedMethodIns)).toLeft()
        }

        return null
    }
}

private object StartReplaceGroupTokenizer : RuntimeInstructionTokenizer() {
    override fun nextToken(context: TokenizerContext): Either<RuntimeInstructionToken, Failure>? {
        val expectedLdc = context[0] ?: return null
        val expectedMethodInsn = context[1] ?: return null

        if (expectedMethodInsn is MethodInsnNode &&
            MethodId(expectedMethodInsn) == MethodIds.Composer.startReplaceGroup
        ) {
            val keyValue = expectedLdc.intValueOrNull() ?: return Failure(
                "Failed parsing startReplaceGroup token: expected key value"
            ).toRight()

            return StartReplaceGroup(ComposeGroupKey(keyValue), listOf(expectedLdc, expectedMethodInsn)).toLeft()

        }

        return null
    }
}

private object EndReplaceGroupTokenizer : RuntimeInstructionTokenizer() {
    override fun nextToken(context: TokenizerContext): Either<RuntimeInstructionToken, Failure>? {
        val expectedMethodIns = context[0] as? MethodInsnNode ?: return null
        if (MethodId(expectedMethodIns) == MethodIds.Composer.endReplaceGroup) {
            return EndReplaceGroup(listOf(expectedMethodIns)).toLeft()
        }

        return null
    }
}


private object BlockTokenizer : RuntimeInstructionTokenizer() {
    override fun nextToken(context: TokenizerContext): Either<RuntimeInstructionToken, Failure>? {
        val instructions = mutableListOf<AbstractInsnNode>()
        var currentContext = context
        while (true) {
            if (priorityTokenizer.nextToken(currentContext) != null) break
            instructions.add(currentContext[0] ?: break)
            currentContext = currentContext.skip(1) ?: break
        }
        if (instructions.isEmpty()) return null
        return RuntimeInstructionToken.BockToken(instructions).toLeft()
    }

}

internal fun tokenizeRuntimeInstructions(
    instructions: List<AbstractInsnNode>
): Either<List<RuntimeInstructionToken>, Failure> {
    if (instructions.isEmpty()) return Failure("Empty list of instructions").toRight()

    var context = TokenizerContext(instructions)
    val tokens = mutableListOf<RuntimeInstructionToken>()

    while (true) {
        val nextResult = tokenizer.nextToken(context) ?: return Failure("Cannot build next token").toRight()
        val nextToken = nextResult.leftOr { failure -> return failure }
        tokens.add(nextToken)
        context = context.skip(nextToken.instructions.size) ?: break
    }

    return tokens.toLeft()
}