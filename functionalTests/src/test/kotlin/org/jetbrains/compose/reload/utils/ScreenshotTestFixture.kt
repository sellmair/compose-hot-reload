package org.jetbrains.compose.reload.utils

import org.intellij.lang.annotations.Language
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.*
import org.jetbrains.compose.reload.orchestration.asChannel
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail


suspend infix fun HotReloadTestFixture.initialSourceCode(source: String): Path {
    val file = writeCode(source = source)
    launchApplicationAndWait()
    return file
}

suspend fun HotReloadTestFixture.launchApplicationAndWait(
    projectPath: String = ":",
    mainClass: String = "MainKt",
) {
    val uiRenderedChannel = orchestration.asChannel()
    val recompilerReadyChannel = orchestration.asChannel()

    launchApplication(projectPath, mainClass)

    logger.info("Waiting for UI to render")
    val rendered = skipToMessage<UIRendered>(uiRenderedChannel)
    assertNull(rendered.reloadRequestId)

    logger.info("Waiting for Daemon to become ready")
    skipToMessage<RecompilerReady>(recompilerReadyChannel)
}

suspend fun HotReloadTestFixture.replaceSourceCodeAndReload(
    oldValue: String, newValue: String
) {
    replaceSourceCodeAndReload(sourceFile = getDefaultMainKtSourceFile(), oldValue, newValue)
}

fun HotReloadTestFixture.replaceSourceCode(
    sourceFile: String,
    oldValue: String, newValue: String
) {
    val resolvedFile = projectDir.resolve(sourceFile)
    val previousText = resolvedFile.readText()
    val updatedText = previousText.replace(oldValue, newValue)
    if (updatedText == previousText) {
        error("Replacement '$oldValue' -> '$newValue' not recognized did not change source code. Typo?")
    }
    writeCode(sourceFile, updatedText)
}

fun HotReloadTestFixture.replaceSourceCode(oldValue: String, newValue: String) =
    replaceSourceCode(getDefaultMainKtSourceFile(), oldValue, newValue)

suspend fun HotReloadTestFixture.replaceSourceCodeAndReload(
    sourceFile: String = getDefaultMainKtSourceFile(),
    oldValue: String, newValue: String
) {
    replaceSourceCode(sourceFile, oldValue, newValue)
    awaitSourceCodeReloaded()
}

suspend fun HotReloadTestFixture.awaitSourceCodeReloaded() {
    logger.info("Waiting for reload request")
    val reloadRequest = run {
        val reloadRequest = skipToMessage<ReloadClassesRequest>()
        if (reloadRequest.changedClassFiles.isEmpty()) fail("No changedClassFiles in reload request")
        reloadRequest
    }

    logger.info("Waiting for UI render")
    run {
        val rendered = skipToMessage<UIRendered>()
        assertEquals(reloadRequest.messageId, rendered.reloadRequestId)
    }
}

suspend fun HotReloadTestFixture.awaitReload() {
    val reloadRequest = skipToMessage<ReloadClassesRequest>()
    val rendered = skipToMessage<UIRendered>()
    assertEquals(reloadRequest.messageId, rendered.reloadRequestId)
}

private fun HotReloadTestFixture.writeCode(
    sourceFile: String = getDefaultMainKtSourceFile(),
    @Language("kotlin") source: String
): Path {
    val resolvedFile = projectDir.resolve(sourceFile)
    resolvedFile.createParentDirectories()
    resolvedFile.writeText(source)
    return resolvedFile
}

internal fun HotReloadTestFixture.getDefaultMainKtSourceFile(): String {
    return when (projectMode) {
        ProjectMode.Kmp -> "src/commonMain/kotlin/Main.kt"
        ProjectMode.Jvm -> "src/main/kotlin/Main.kt"
    }
}
