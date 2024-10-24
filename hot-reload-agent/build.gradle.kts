plugins {
    kotlin("jvm")
    `maven-publish`
    `publishing-conventions`
}

tasks.withType<Jar>().named(kotlin.target.artifactsTaskName).configure {
    manifest.attributes(
        "Premain-Class" to "org.jetbrains.compose.reload.agent.ComposeHotReloadAgent",
        "Can-Redefine-Classes" to "true",
        "Can-Retransform-Classes" to "true",
    )
}

publishing {
    publications.create("maven", MavenPublication::class) {
        from(components["java"])
    }
}

dependencies {
    implementation(project(":hot-reload-orchestration"))
    implementation(deps.slf4j.api)
    implementation(deps.coroutines.core)
    implementation(deps.javassist)
}