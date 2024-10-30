package org.jetbrains.compose.hotreload

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.lineMarker.RunLineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

private const val HOT_RELOAD_ANNOTATION_FQN = "org.jetbrains.compose.reload.DevelopmentEntryPoint"

class HotReloadLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: com.intellij.psi.PsiElement): LineMarkerInfo<*>? {
        if (element !is LeafPsiElement) return null
        if (element.node.elementType != KtTokens.IDENTIFIER) return null

        val ktFun = element.parent as? KtNamedFunction ?: return null
        if (!ktFun.isValidDevEntryPoint()) return null

        val module = ProjectFileIndex.getInstance(ktFun.project).getModuleForFile(ktFun.containingFile.virtualFile)
        if (module == null || module.isDisposed) return null
        val modulePath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return null

        val entryPoint = DevEntryPoint(
            ktFun.containingKtFile.javaFileFacadeFqName.asString(),
            ktFun.name.orEmpty(),
            modulePath
        )

        return RunLineMarkerProvider.createLineMarker(
            element,
            AllIcons.Actions.RerunAutomatically,
            listOf(RunLineMarkerContributor.Info(RunDevEntryPointAction(entryPoint)))
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

private class RunDevEntryPointAction(
    private val entryPoint: DevEntryPoint
) : AnAction({ "Run development app" }, AllIcons.Actions.RerunAutomatically) {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val service = project.service<HotReloadService>()
        service.runDevEntryPoint(entryPoint)
    }
}