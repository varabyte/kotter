import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.jvm.java

// Note: This seems to need to exist here and NOT in settings.gradle.kts pluginManagment, or else Gradle/Kotlin whines
// at build about the same plugin being loaded multiple times.
// See also: https://docs.gradle.org/current/userguide/plugins.html#sec:subprojects_plugins_dsl
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinx.kover) apply false
    alias(libs.plugins.jetbrains.dokka) apply false
    alias(libs.plugins.vanniktech.publish) apply false
}

group = "com.varabyte.kotter"
version = libs.versions.kotter.get()

// Set JVM target compatibility to prevent Gradle errors when compiling. Choose a very old version since we can as that
// should maximize flexibility.
subprojects {
    val jvmTarget = JvmTarget.JVM_11
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = jvmTarget.target
        targetCompatibility = jvmTarget.target
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(jvmTarget)
    }

    // Set jdk-release for all compilation targets. See also: https://jakewharton.com/kotlins-jdk-release-compatibility-flag/
    // (Short version: resolves ambiguity Kotlin extension methods and new methods added into more recent JDKs)

    fun KotlinJvmCompilerOptions.addCommonCompilerArgs() {
        freeCompilerArgs.addAll("-Xjdk-release=${jvmTarget.target}")
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.findByType(KotlinMultiplatformExtension::class.java)?.let { kotlin ->
            kotlin.targets.withType<KotlinJvmTarget> {
                compilerOptions.addCommonCompilerArgs()
            }
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        project.tasks.withType(KotlinCompile::class.java).configureEach {
            compilerOptions.addCommonCompilerArgs()
        }
    }
}
