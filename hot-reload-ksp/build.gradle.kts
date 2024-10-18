plugins {
    kotlin("jvm")
    `maven-publish`
    `publishing-conventions`
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.25")
    implementation(project(":hot-reload-annotation"))
}
