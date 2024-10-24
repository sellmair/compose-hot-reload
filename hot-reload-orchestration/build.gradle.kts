plugins {
    kotlin("jvm")
    `maven-publish`
    `publishing-conventions`
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

kotlin {
    compilerOptions {
        explicitApi()
    }
}

dependencies {
    api(deps.coroutines.core)
    implementation(deps.slf4j.api)
    implementation(deps.ktorNetwork)

    testImplementation(kotlin("test"))
    testImplementation(deps.junit.jupiter)
    testImplementation(deps.junit.jupiter.engine)
    testImplementation(deps.coroutines.test)
    testImplementation(deps.logback)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}