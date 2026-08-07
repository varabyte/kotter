plugins {
    id("compiler-setup")
    id("kotter-publication")
    id("dokka-convention")
}

group = "com.varabyte.kotterx"
version = libs.versions.kotter.get()

kotlin {
    // Targets set in kotter-build-support plugin

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(project(":kotter"))
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.truthish)
        }
    }
}

kotterPublication {
    name.set("Kotter test support")
    description.set("Helper classes for writing tests for code using Kotter.")
}
