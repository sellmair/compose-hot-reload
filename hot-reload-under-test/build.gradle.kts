@file:OptIn(ExperimentalComposeLibrary::class)

import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    `maven-publish`
    `publishing-conventions`
}

publishing {
    publications.register<MavenPublication>("maven") {
        from(components["java"])
    }
}

dependencies {
    compileOnly(project(":hot-reload-agent"))
    compileOnly(project(":hot-reload-orchestration"))
    compileOnly(project(":hot-reload-runtime-api"))

    implementation(deps.logback)
    implementation(deps.coroutines.swing)
    implementation(compose.uiTest)
    implementation(compose.components.resources)

    api(compose.desktop.common)
    api(compose.material3)
}

/* Add special 'dev' runtime dependency */
internal class ComposeDevJavaRuntimeCompatibilityRule : AttributeCompatibilityRule<Usage> {
    override fun execute(details: CompatibilityCheckDetails<Usage>) {
        if (details.consumerValue?.name == "compose-dev-java-runtime" &&
            details.producerValue?.name == Usage.JAVA_RUNTIME
        ) {
            details.compatible()
        }
    }
}

dependencies.attributesSchema.attribute(Usage.USAGE_ATTRIBUTE).compatibilityRules.add(ComposeDevJavaRuntimeCompatibilityRule::class.java)

configurations.compileClasspath {
    attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named("compose-dev-java-runtime"))
}

dependencies {
    compileOnly(project(":hot-reload-runtime-jvm", configuration = "jvmDevRuntimeElements"))
}