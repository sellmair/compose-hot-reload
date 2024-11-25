@file:Suppress("UnstableApiUsage")

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    `publishing-conventions`
}

/* Setup integration test */
run {
    val main = kotlin.target.compilations.getByName("main")
    val functionalTest = kotlin.target.compilations.create("functionalTest")
    functionalTest.associateWith(main)

    val functionalTestTask = tasks.register<Test>("functionalTest") {
        testClassesDirs = functionalTest.output.classesDirs
        classpath = functionalTest.output.allOutputs + functionalTest.runtimeDependencyFiles
        systemProperty("firework.version", deps.versions.firework.get())
    }

    tasks.check.configure {
        dependsOn(functionalTestTask)
    }
}

tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.JETBRAINS)
    })

    useJUnitPlatform {
        if (providers.gradleProperty("host-integration-tests").orNull == "true") {
            includeTags("HostIntegrationTest")
            environment("TEST_ONLY_LATEST_VERSIONS", "true")
        }
    }

    dependsOn(":publishLocally")
    systemProperty("local.test.repo", rootProject.layout.buildDirectory.dir("repo").get().asFile.absolutePath)
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
    systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "4")
    systemProperty("apple.awt.UIElement", true)

    jvmArgs("-DlogLevel=DEBUG")
    maxHeapSize = "4G"

    maxParallelForks = 2


    testLogging {
        showStandardStreams = true
    }
}

gradlePlugin {
    plugins.create("hot-reload") {
        id = "org.jetbrains.compose-hot-reload"
        implementationClass = "org.jetbrains.compose.reload.ComposeHotReloadPlugin"
        testSourceSet(sourceSets.getByName("functionalTest"))
    }
}

dependencies {
    val functionalTestImplementation by configurations

    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    compileOnly(kotlin("gradle-plugin"))
    compileOnly(deps.compose.gradlePlugin)

    implementation(project(":hot-reload-orchestration"))

    functionalTestImplementation(gradleTestKit())
    functionalTestImplementation(testFixtures(project(":hot-reload-orchestration")))
    functionalTestImplementation(kotlin("test"))
    functionalTestImplementation(kotlin("tooling-core"))
    functionalTestImplementation(deps.junit.jupiter)
    functionalTestImplementation(deps.junit.jupiter.engine)
    functionalTestImplementation(deps.coroutines.core)
    functionalTestImplementation(deps.coroutines.test)

    testImplementation(kotlin("test"))
    testImplementation(gradleKotlinDsl())
    testImplementation(deps.junit.jupiter)
    testImplementation(deps.junit.jupiter.engine)
    testImplementation(kotlin("gradle-plugin"))
    testImplementation("com.android.tools.build:gradle:8.6.1")
}


/* Make the current 'Hot Reload Version (aka version of this project) available */
run {
    val generatedSourceDir = file("build/generated/main/kotlin")

    val writeBuildConfig = tasks.register("writeBuildConfig") {
        val file = generatedSourceDir.resolve("BuildConfig.kt")

        val versionProperty = project.providers.gradleProperty("version").get()
        inputs.property("version", versionProperty)

        println(versionProperty)


        outputs.file(file)

        val text = """
            package org.jetbrains.compose.reload
            
            internal const val HOT_RELOAD_VERSION = "$versionProperty"
            """
            .trimIndent()

        inputs.property("text", text)

        doLast {
            file.parentFile.mkdirs()
            logger.quiet(text)
            file.writeText(text)
        }
    }

    kotlin {
        sourceSets.main.get().kotlin.srcDir(generatedSourceDir)
    }

    tasks.register("prepareKotlinIdeaImport") {
        dependsOn(writeBuildConfig)
    }

    kotlin.target.compilations.getByName("main").compileTaskProvider.configure {
        dependsOn(writeBuildConfig)
    }
}


