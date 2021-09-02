plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "com.varabyte"
version = "0.9.0"

repositories {
    mavenCentral()
}

object Versions {
    object Kotlin {
        const val Couroutines = "1.5.1"
    }
    const val Jline = "3.20.0"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.Kotlin.Couroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:${Versions.Kotlin.Couroutines}")

    // For system terminal implementation
    implementation("org.jline:jline-terminal:${Versions.Jline}")
    implementation("org.jline:jline-terminal-jansi:${Versions.Jline}")
    runtimeOnly(files("libs/jansi-1.18.jar")) // Required for windows support

    // For GuardedBy concurrency annotation
    implementation("net.jcip:jcip-annotations:1.0")
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("konsole") {
            from(components["java"])
        }
    }
}