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
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
}

application {
    mainClass.set("MainKt")
}