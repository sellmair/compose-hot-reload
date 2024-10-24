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

    tasks.register<Test>("functionalTest") {
        testClassesDirs = functionalTest.output.classesDirs
        classpath = functionalTest.output.allOutputs + functionalTest.runtimeDependencyFiles
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    dependsOn(":hot-reload-runtime-api:publishAllPublicationsToLocalRepository")
    dependsOn(":hot-reload-runtime-jvm:publishAllPublicationsToLocalRepository")
    dependsOn(":hot-reload-orchestration:publishAllPublicationsToLocalRepository")
    dependsOn(":hot-reload-agent:publishAllPublicationsToLocalRepository")
    systemProperty("local.test.repo", rootProject.layout.buildDirectory.dir("repo").get().asFile.absolutePath)
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

    compileOnly(kotlin("gradle-plugin"))
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(project(":hot-reload-orchestration"))

    functionalTestImplementation(gradleTestKit())
    functionalTestImplementation(kotlin("test"))
    functionalTestImplementation(kotlin("tooling-core"))
    functionalTestImplementation(deps.junit.jupiter)
    functionalTestImplementation(deps.junit.jupiter.engine)


    testImplementation(kotlin("test"))
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

        val hotswapAgentCore = deps.hotswapAgentCore.get().toString()
        inputs.property("hotswapAgentCore", hotswapAgentCore)

        outputs.file(file)

        val text = """
            package org.jetbrains.compose.reload
            
            internal const val HOT_RELOAD_VERSION = "$versionProperty"
            
            internal const val HOTSWAP_AGENT_CORE = "$hotswapAgentCore"
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


