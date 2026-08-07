import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.jvm.java

// Set JVM target compatibility to prevent Gradle errors when compiling. Choose a very old version since we can as that
// should maximize flexibility.
val kotterJvmTarget = JvmTarget.JVM_11

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = kotterJvmTarget.target
    targetCompatibility = kotterJvmTarget.target
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(kotterJvmTarget)
}

// Set jdk-release for all compilation targets. See also: https://jakewharton.com/kotlins-jdk-release-compatibility-flag/
// (Short version: resolves ambiguity Kotlin extension methods and new methods added into more recent JDKs)
fun KotlinJvmCompilerOptions.addCommonCompilerArgs() {
    freeCompilerArgs.addAll("-Xjdk-release=${kotterJvmTarget.target}")
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
