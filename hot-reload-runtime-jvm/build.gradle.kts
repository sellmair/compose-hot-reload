plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    `maven-publish`
    `publishing-conventions`
    `dev-runtime-jar`
}

kotlin {
    compilerOptions {
        explicitApi()
    }
}

dependencies {
    implementation(compose.runtime)

    devCompileOnly(project(":hot-reload-agent"))
    devCompileOnly(deps.hotswapAgentCore)
    devImplementation(project(":hot-reload-orchestration"))

    devImplementation(deps.javassist)
    devImplementation(deps.slf4j.api)
    devImplementation(compose.desktop.common)
    devImplementation(compose.material3)

    testImplementation(kotlin("test"))
    testImplementation(deps.junit.jupiter)
    testImplementation(deps.junit.jupiter.engine)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

publishing {
    publications.register<MavenPublication>("maven") {
        from(components["java"])
    }
}