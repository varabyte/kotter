import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    `maven-publish`
    signing
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

fun shouldSign() = (findProperty("kotter.sign") as? String).toBoolean()
fun shouldPublishToGCloud(): Boolean {
    return (findProperty("kotter.gcloud.publish") as? String).toBoolean()
            && findProperty("gcloud.artifact.registry.secret") != null
}
fun shouldPublishToMavenCentral(): Boolean {
    return (findProperty("kotter.maven.publish") as? String).toBoolean()
            && findProperty("ossrhToken") != null && findProperty("ossrhTokenPassword") != null
}


val VARABYTE_REPO_URL = uri("https://us-central1-maven.pkg.dev/varabyte-repos/public")
fun MavenArtifactRepository.gcloudAuth() {
    url = VARABYTE_REPO_URL
    credentials {
        username = "_json_key_base64"
        password = findProperty("gcloud.artifact.registry.secret") as String
    }
    authentication {
        create<BasicAuthentication>("basic")
    }
}

val SONATYPE_RELEASE_REPO_URL = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
val SONATYPE_SNAPSHOT_REPO_URL = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
fun MavenArtifactRepository.sonatypeAuth() {
    url = if (!version.toString().endsWith("SNAPSHOT")) SONATYPE_RELEASE_REPO_URL else SONATYPE_SNAPSHOT_REPO_URL
    credentials {
        username = findProperty("ossrhToken") as String
        password = findProperty("ossrhTokenPassword") as String
    }
    authentication {
        create<BasicAuthentication>("basic")
    }
}

val dokkaHtml by tasks.getting(DokkaTask::class)

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

publishing {
    publications {
        if (shouldPublishToGCloud()) {
            repositories {
                maven {
                    name = "GCloudMaven"
                    gcloudAuth()
                }
            }
        }
        if (shouldPublishToMavenCentral()) {
            repositories {
                maven {
                    name = "SonatypeMaven"
                    sonatypeAuth()
                }
            }
        }

        withType<MavenPublication> {
            artifact(javadocJar)
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
                    }
                }

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
}

if (shouldSign()) {
    // Workaround for https://youtrack.jetbrains.com/issue/KT-61858
    val signingTasks = tasks.withType<Sign>()
    tasks.withType<AbstractPublishToMaven>().configureEach {
        mustRunAfter(signingTasks)
    }

    // If "shouldSign" returns true, then singing password should be set
    val signingPassword = findProperty("signing.password") as String

    signing {
        // If here, we're on a CI. Check for the signing key which must be set in an environment variable.
        // See also: https://docs.gradle.org/current/userguide/signing_plugin.html#sec:in-memory-keys
        val secretKeyRingExists = (findProperty("signing.secretKeyRingFile") as? String)
            ?.let { File(it).exists() }
            ?: false

        if (!secretKeyRingExists) {
            val signingKey: String? by project
            useInMemoryPgpKeys(signingKey, signingPassword)
        }

        // Signing requires the following steps at https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials
        // and adding signatory properties somewhere reachable, e.g. ~/.gradle/gradle.properties
        sign(publishing.publications)
    }
}
