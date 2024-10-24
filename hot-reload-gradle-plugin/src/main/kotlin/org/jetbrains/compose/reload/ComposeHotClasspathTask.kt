package org.jetbrains.compose.reload

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.withType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.jetbrains.compose.reload.orchestration.OrchestrationClient
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage
import org.jetbrains.compose.reload.orchestration.OrchestrationMessage.ReloadClassesRequest.ChangeType
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import java.net.InetAddress
import java.net.Socket
import java.nio.file.Path
import kotlin.io.path.absolute


internal fun Project.setupComposeHotClasspathTasks() {
    kotlinMultiplatformOrNull?.targets?.all { target ->
        target.compilations.all { compilation -> setupComposeHotClasspathTask(compilation) }
    }

    kotlinJvmOrNull?.target?.compilations?.all { compilation -> setupComposeHotClasspathTask(compilation) }

    tasks.withType<ComposeHotClasspathTask>().configureEach { task ->
        task.outputs.upToDateWhen { true }
        task.group = "compose"
        task.agentPort.set(project.providers.systemProperty("compose.hot.reload.agent.port").map { it.toInt() })
    }
}

internal fun Project.setupComposeHotClasspathTask(compilation: KotlinCompilation<*>): TaskProvider<ComposeHotClasspathTask> {
    val name = composeHotClasspathTaskName(compilation)
    if (name in tasks.names) return tasks.named(name, ComposeHotClasspathTask::class.java)

    return tasks.register(name, ComposeHotClasspathTask::class.java) { task ->
        task.classpath.from(compilation.createComposeHotReloadRunClasspath())
    }
}

internal fun composeHotClasspathTaskName(compilation: KotlinCompilation<*>): String {
    return compilation.compileKotlinTaskName + "HotClasspath"
}

internal open class ComposeHotClasspathTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Incremental
    val classpath: ConfigurableFileCollection = project.objects.fileCollection()

    @Internal
    val agentPort = project.objects.property<Int>()


    @TaskAction
    fun execute(inputs: InputChanges) {
        if (inputs.isIncremental) {
            logger.quiet("Incremental run")
        }

        val changedClassFiles = mutableMapOf<Path, ChangeType>()
        inputs.getFileChanges(classpath).forEach { change ->
            val changeType = when (change.changeType) {
                org.gradle.work.ChangeType.ADDED -> ChangeType.Added
                org.gradle.work.ChangeType.MODIFIED -> ChangeType.Modified
                org.gradle.work.ChangeType.REMOVED -> ChangeType.Removed
            }

            changedClassFiles[change.file.toPath().absolute()] = changeType
            logger.quiet("[${change.changeType}] ${change.file}")
        }

        val client = OrchestrationClient() ?: error("Failed to create 'OrchestrationClient'!")
        logger.quiet("Sending 'ReloadClassesRequest'")

        runBlocking {
            val message = OrchestrationMessage.ReloadClassesRequest(changedClassFiles)

            logger.quiet("Entered run blocking")
            client.send(message)

            logger.quiet("Awaiting response for ${message.messageId}")
            client.receive.first { reply -> reply == message }

        }
    }
}
