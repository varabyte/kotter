import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
}

group = "com.varabyte.kotter.examples"
version = "1.0-SNAPSHOT" 

allprojects {
    repositories {
        mavenCentral()
    }
}
