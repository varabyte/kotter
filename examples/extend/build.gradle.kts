plugins {
    kotlin("jvm")
    application
}

group = "com.varabyte.kotter"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":kotter"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
}

application {
    mainClass.set("MainKt")
}