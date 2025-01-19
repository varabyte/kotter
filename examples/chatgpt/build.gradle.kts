plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
    application
}

group = "com.varabyte.kotter"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":kotter"))
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
}

application {
    mainClass.set("MainKt")
}