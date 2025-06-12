import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
}

val defaultKotlinVersion = "1.7.22" // Improved lambda inference
val nativeKotlinVersion = "1.9.25" // Time and Atomic system support

fun KotlinCommonCompilerOptions.setToVersion(version: String) {
    check(Regex("""\d+\.\d+\.\d+""").matches(version))
    val withoutPatch = version.substringBeforeLast('.')
    val kotlinVersion = KotlinVersion.fromVersion(withoutPatch)
    languageVersion = kotlinVersion
    apiVersion = kotlinVersion
}

// Applies to ALL Kotlin compilation tasks
tasks.withType<KotlinCompilationTask<*>> {
    compilerOptions {
        freeCompilerArgs.addAll(
            // Kotlin generates a TON of warnings about expect / actual. We're willing to take the risk here.
            "-Xexpect-actual-classes",
        )
        setToVersion(defaultKotlinVersion)
    }
}

tasks.withType<KotlinNativeCompile>().configureEach {
    compilerOptions {
        optIn.addAll(
            "kotlin.RequiresOptIn",
            // Posix APIs are experimental (but hopefully we're not using anything too controversial)
            "kotlin.experimental.ExperimentalNativeApi",
            "kotlinx.cinterop.ExperimentalForeignApi",
            // "The declaration is using numbers with different bit widths in least two actual platforms. Such types shall not be used in user-defined 'expect fun' signatures"
            "kotlinx.cinterop.UnsafeNumber",
        )
        setToVersion(nativeKotlinVersion)
    }
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    linuxX64()
    macosArm64()
    macosX64()
    mingwX64()

    // See https://kotlinlang.org/docs/multiplatform-hierarchy.html#additional-configuration
    @Suppress("OPT_IN_USAGE")
    applyDefaultHierarchyTemplate {
        common {
            group("native") {
                group("posix") {
                    withLinuxX64()
                    withMacos()
                }
                group("win") {
                    withMingwX64()
                }
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib", defaultKotlinVersion))
            }
        }

        nativeMain.dependencies {
            implementation(kotlin("stdlib", nativeKotlinVersion))
        }
    }
}

val gcloudSecret: String? = findProperty("gcloud.artifact.registry.secret") as? String

// Hack: project.version is NOT set yet but needs to be for credentials to be set correctly for snapshot builds. Put
// behind an `afterEvaluate` for now to ensure the calling build script has set the version.
afterEvaluate {
    mavenPublishing {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
        signAllPublications()

        pom {
            val githubPath = "https://github.com/varabyte/kotter"
            name.set("Kotter")
            description.set("A declarative, Kotlin-idiomatic API for writing dynamic command line applications.")
            url.set(githubPath)
            scm {
                url.set(githubPath)
                val connectionPath = "scm:git:${githubPath}.git"
                connection.set(connectionPath)
                developerConnection.set(connectionPath)
            }
            developers {
                developer {
                    id.set("bitspittle")
                    name.set("David Herman")
                    email.set("bitspittle@gmail.com")
                    url.set("https://github.com/bitspittle")
                }
            }

            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
        }
    }
}

publishing {
    publications {
        gcloudSecret?.let { gcloudSecret ->
            repositories {
                maven {
                    name = "GCloudMaven"
                    url = uri("https://us-central1-maven.pkg.dev/varabyte-repos/public")
                    credentials {
                        username = "_json_key_base64"
                        password = gcloudSecret
                    }
                    authentication {
                        create<BasicAuthentication>("basic")
                    }
                }
            }
        }
    }
}
