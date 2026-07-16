plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.example.hackernews.collector.ClassicsCollectorKt")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.rssparser)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}

tasks.named<JavaExec>("run") {
    // run from the repo root so the default asset paths resolve.
    workingDir = rootProject.projectDir
}
