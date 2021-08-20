plugins {
    kotlin("jvm") version "1.5.10"
}

group = "com.varabyte"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

object Versions {
    object Kotlin {
        const val Couroutines = "1.5.1"
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.Kotlin.Couroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:${Versions.Kotlin.Couroutines}")
}
