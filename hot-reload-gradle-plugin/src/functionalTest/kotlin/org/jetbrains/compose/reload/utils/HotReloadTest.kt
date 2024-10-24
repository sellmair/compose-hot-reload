@file:OptIn(ExperimentalPathApi::class)

package org.jetbrains.compose.reload.utils

import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.compose.reload.utils.HotReloadTestFixtureProvider.Companion.testFixtureKey
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.*
import java.nio.file.Files
import java.util.stream.Stream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

@TestTemplate
@ExtendWith(HotReloadTestInvocationContextProvider::class)
annotation class HotReloadTest

class HotReloadTestFixture(
    val projectDir: ProjectDir,
    val gradleRunner: GradleRunner,
)

internal class HotReloadTestInvocationContextProvider : TestTemplateInvocationContextProvider {
    override fun supportsTestTemplate(context: ExtensionContext): Boolean {
        if (context.testMethod.isEmpty) return false
        return context.testMethod.get().isAnnotationPresent(HotReloadTest::class.java)
    }

    override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> {
        val testedVersions = TestedGradleVersion.entries.flatMap { testedGradleVersion ->
            TestedKotlinVersion.entries.flatMap { testedKotlinVersion ->
                TestedComposeVersion.entries.map { testedComposeVersion ->
                    TestedVersions(
                        gradle = testedGradleVersion,
                        kotlin = testedKotlinVersion,
                        compose = testedComposeVersion,
                    )
                }
            }
        }

        return testedVersions.stream().map { versions -> HotReloadTestInvocationContext(versions) }
    }
}

class HotReloadTestInvocationContext(
    private val versions: TestedVersions,
) : TestTemplateInvocationContext {
    override fun getDisplayName(invocationIndex: Int): String {
        return "Gradle ${versions.gradle.version}, Kotlin ${versions.kotlin.version}, Compose ${versions.compose.version}"
    }

    override fun getAdditionalExtensions(): List<Extension> {
        return listOf(
            SimpleValueProvider(versions),
            SimpleValueProvider(versions.gradle),
            SimpleValueProvider(versions.kotlin),
            SimpleValueProvider(versions.compose),
            HotReloadTestFixtureProvider(versions),
            DefaultSettingsGradleKtsExtension(versions)
        )
    }
}

private inline fun <reified T : Any> SimpleValueProvider(value: T): SimpleValueProvider<T> {
    return SimpleValueProvider(T::class.java, value)
}

private class SimpleValueProvider<T : Any>(
    private val type: Class<T>, private val value: T,
) : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == type
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return value
    }
}

private class HotReloadTestFixtureProvider(private val versions: TestedVersions) :
    ParameterResolver, BeforeEachCallback, AfterEachCallback {

    companion object {
        const val testFixtureKey = "hotReloadTestFixture"
    }

    override fun beforeEach(context: ExtensionContext) {
        context.getOrCreateTestFixture()
    }

    override fun afterEach(context: ExtensionContext) {
        context.getHotReloadTestFixtureOrThrow().projectDir.path.deleteRecursively()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type in listOf(
            HotReloadTestFixture::class.java,
            GradleRunner::class.java, ProjectDir::class.java
        )
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? {
        return when (parameterContext.parameter.type) {
            HotReloadTestFixture::class.java -> extensionContext.getHotReloadTestFixtureOrThrow()
            else -> throw IllegalArgumentException("Unknown type: ${parameterContext.parameter.type}")
        }
    }

    private fun ExtensionContext.getOrCreateTestFixture(): HotReloadTestFixture {
        return getStore(namespace).getOrComputeIfAbsent(
            testFixtureKey,
            { createTestFixture() },
            HotReloadTestFixture::class.java
        )
    }

    private fun createTestFixture(): HotReloadTestFixture {
        val projectDir = ProjectDir(Files.createTempDirectory("hot-reload-test"))

        val gradleRunner = GradleRunner.create()
            .withProjectDir(projectDir.path.toFile())
            .withGradleVersion(versions.gradle.version.version)
            .addedArguments("-PcomposeVersion=${versions.compose}")
            .addedArguments("-PkotlinVersion=${versions.kotlin}")
            .addedArguments("--configuration-cache")
            .addedArguments("-s")

        return HotReloadTestFixture(projectDir, gradleRunner)
    }
}

internal fun ExtensionContext.getHotReloadTestFixtureOrThrow(): HotReloadTestFixture {
    return getStore(namespace).get(testFixtureKey, HotReloadTestFixture::class.java)
        ?: error("Missing '${HotReloadTestFixture::class.simpleName}'")
}