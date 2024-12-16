package org.jetbrains.compose.reload.core.testFixtures

import org.jetbrains.compose.reload.core.testFixtures.CompilerOption.entries

enum class CompilerOption(val default: Boolean) {
    OptimizeNonSkippingGroups(true),
    GenerateFunctionKeyMetaAnnotations(true),
    SourceInformation(false);
}

object CompilerOptions {
    val default: Map<CompilerOption, Boolean> = entries.associate { it to it.default }
}