package org.jetbrains.compose.hotreload

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate
import kotlin.reflect.KClass

class HotReloadProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return HotReloadProcessor(environment)
    }

}

class HotReloadProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotated = resolver.findAnnotations(HotReload::class).toList()
        val functionImports = annotated.map { it.qualifiedName?.asString() }
        val functionNames = annotated.map { it.simpleName.asString() }

        val sourceFiles = annotated.mapNotNull { it.containingFile }
        try {
            val file = environment.codeGenerator.createNewFile(
                Dependencies(
                    false,
                    *sourceFiles.toList().toTypedArray(),
                ),
                "org.jetbrains.compose.hotreload.ksp",
                "HotReloadFunctions"
            )

            file.write(buildString {
                appendLine("import androidx.compose.ui.awt.ComposePanel")
                functionImports.forEach { import -> appendLine("import $import") }
                appendLine()
                functionNames.forEach { name -> hotReloadFunctionText(name) }
            }.toByteArray())
        } catch (e: Exception) {}
        return annotated.filter { !it.validate() }.toList()
    }

    private fun Resolver.findAnnotations(kClass: KClass<*>) =
        getSymbolsWithAnnotation(kClass.qualifiedName.toString())
        .filterIsInstance<KSFunctionDeclaration>().filter { it.parameters.isEmpty() }

    private fun StringBuilder.hotReloadFunctionText(name: String) {
        appendLine("fun hotreload_${name}(): ComposePanel = ComposePanel().apply {")
        appendLine("    setContent { ${name}() }")
        appendLine("}")
        appendLine()
    }
}