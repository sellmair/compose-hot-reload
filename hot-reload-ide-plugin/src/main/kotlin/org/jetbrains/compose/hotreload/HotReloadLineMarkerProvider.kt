package org.jetbrains.compose.hotreload

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

private const val HOT_RELOAD_ANNOTATION_FQN = "org.jetbrains.compose.reload.DevEntryPoint"

class HotReloadLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: com.intellij.psi.PsiElement): LineMarkerInfo<*>? {
        if (element !is LeafPsiElement) return null
        if (element.node.elementType != KtTokens.IDENTIFIER) return null
        val ktFun = element.parent as? KtNamedFunction ?: return null
        if (!ktFun.isValidDevEntryPoint()) return null

        return LineMarkerInfo(
            element,
            element.textRange,
            AllIcons.Actions.RerunAutomatically,
            null,
            null,
            GutterIconRenderer.Alignment.LEFT,
            { "Hot reload" }
        )
    }

    private fun KtNamedFunction.isValidDevEntryPoint(): Boolean {
        fun calculate(): Boolean {
            if (isPrivate()) return false
            if (!isTopLevel) return false
            if (valueParameters.size > 0) return false
            if (receiverTypeReference != null) return false

            return annotationEntries.any { ktAnnotationEntry ->
                ktAnnotationEntry.fqNameMatches(HOT_RELOAD_ANNOTATION_FQN)
            }
        }

        return CachedValuesManager.getCachedValue(this) {
            CachedValueProvider.Result.create(
                calculate(),
                containingKtFile,
                ProjectRootModificationTracker.getInstance(project)
            )
        }
    }

    private fun KtAnnotationEntry.fqNameMatches(fqName: String): Boolean {
        val shortName = shortName?.asString() ?: return false
        return fqName.endsWith(shortName) && fqName == getQualifiedName()
    }

    @Suppress("UnstableApiUsage")
    private fun KtAnnotationEntry.getQualifiedName(): String? =
        analyze(BodyResolveMode.PARTIAL).get(BindingContext.ANNOTATION, this)?.fqName?.asString()
}