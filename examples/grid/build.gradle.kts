plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "com.varabyte.kotter"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":kotter"))
    implementation(libs.kotlinx.coroutines)
}

application {
    mainClass.set("MainKt")
}