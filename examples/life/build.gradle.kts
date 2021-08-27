plugins {
    kotlin("jvm") version "1.5.10"
    application
}

group = "com.varabyte"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":konsole"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
}

application {
    mainClass.set("MainKt")
}
