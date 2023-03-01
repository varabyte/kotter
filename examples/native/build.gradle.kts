plugins {
    alias(libs.plugins.kotlin.multiplatform)
    application
}

group = "com.varabyte.kotter"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
}

kotlin {
    // List of supported binary targets
    // Note: You will need the right machine to build each one; otherwise, the target is disabled automatically
    listOf(
        linuxX64(), // Linux
        mingwX64(), // Windows
        macosArm64(), // Mac M1
        macosX64(), // Mac Legacy
    ).forEach { nativeTarget ->
        nativeTarget.apply {
            binaries {
                executable {
                    entryPoint = "main"
                }
            }
        }
    }

    sourceSets {
        val nativeMain by creating {
            dependencies {
                implementation(project(":kotter"))
                implementation(libs.kotlinx.coroutines)
            }
        }

        val linuxX64Main by getting { dependsOn(nativeMain) }
        val mingwX64Main by getting { dependsOn(nativeMain) }
        val macosArm64Main by getting { dependsOn(nativeMain) }
        val macosX64Main by getting { dependsOn(nativeMain) }
    }
}


application {
    mainClass.set("MainKt")
}

