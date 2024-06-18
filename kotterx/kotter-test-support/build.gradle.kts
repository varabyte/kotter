import org.jetbrains.dokka.gradle.DokkaTask

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.dokka)
    `maven-publish`
    signing
}

group = "com.varabyte.kotterx"
version = libs.versions.kotter.get()

kotlin {
    jvm {
        jvmToolchain(8)

        val main by compilations.getting {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    linuxX64()
    macosArm64()
    macosX64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines)

                implementation(project(":kotter"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.truthish)
            }
        }
    }
}

fun shouldSign() = (findProperty("kotter.sign") as? String).toBoolean()
fun shouldPublishToGCloud(): Boolean {
    return (findProperty("kotterx.test.gcloud.publish") as? String).toBoolean()
            && findProperty("gcloud.artifact.registry.secret") != null
}

fun shouldPublishToMavenCentral(): Boolean {
    return (findProperty("kotterx.test.maven.publish") as? String).toBoolean()
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
            val githubPath = "https://github.com/varabyte/kotter"
            pom {
                name.set("Kotter test support")
                description.set("Helper classes for writing tests for code using Kotter.")
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

        // Signing requires following steps at https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials
        // and adding singatory properties somewhere reachable, e.g. ~/.gradle/gradle.properties
        sign(publishing.publications)
    }
}