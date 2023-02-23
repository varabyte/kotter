import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.dokka)
    `maven-publish`
    signing
}

group = "com.varabyte.kotterx"
version = libs.versions.kotter.get()

fun shouldSign() = (findProperty("kotter.sign") as? String).toBoolean()
fun shouldPublishToGCloud(): Boolean {
    return (findProperty("kotterx.test.gcloud.publish") as? String).toBoolean()
            && findProperty("gcloud.artifact.registry.secret") != null
}

fun shouldPublishToMavenCentral(): Boolean {
    // Only publish snapshots to our varabyte repo for now, we may change our mind later
    return !version.toString().endsWith("SNAPSHOT")
            && (findProperty("kotterx.test.maven.publish") as? String).toBoolean()
            && findProperty("ossrhUsername") != null && findProperty("ossrhPassword") != null
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
        username = findProperty("ossrhUsername") as String
        password = findProperty("ossrhPassword") as String
    }
    authentication {
        create<BasicAuthentication>("basic")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.coroutines)

    implementation(project(":kotter"))

    testImplementation(libs.kotlin.test)
    testImplementation(libs.truthish)
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
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

        create<MavenPublication>("kotterTestSupport") {
            val githubPath = "https://github.com/varabyte/kotter"
            from(components["java"])
            pom {
                artifactId = "kotter-test-support"
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
    signing {
        // Signing requires following steps at https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials
        // and adding singatory properties somewhere reachable, e.g. ~/.gradle/gradle.properties
        sign(publishing.publications["kotterTestSupport"])
    }
}