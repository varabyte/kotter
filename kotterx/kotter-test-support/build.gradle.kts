import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("kotter-build-support")
}

group = "com.varabyte.kotterx"
version = libs.versions.kotter.get()

kotlin {
    // Targets set in kotter-build-support plugin

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines)
                implementation(project(":kotter"))
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.truthish)
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("Kotter test support")
                description.set("Helper classes for writing tests for code using Kotter.")
            }
        }
    }
}
