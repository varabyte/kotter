plugins {
    kotlin("jvm")
    `maven-publish`
    signing
}

group = "com.varabyte"
version = "0.9.0"

fun shouldSign() = (findProperty("konsole.sign") as? String).toBoolean()
fun shouldPublishToGCloud(): Boolean {
    return (findProperty("konsole.gcloud.publish") as? String).toBoolean()
            && findProperty("gcloud.artifact.registry.secret") != null
}

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

val VARABYTE_REPO_URL = uri("https://us-central1-maven.pkg.dev/varabyte-repos/public")
repositories {
    mavenCentral()
    if (shouldPublishToGCloud()) {
        maven { gcloudAuth() }
    }
}

object Versions {
    object Kotlin {
        const val Couroutines = "1.5.1"
    }
    const val Jline = "3.20.0"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.Kotlin.Couroutines}")

    // For system terminal implementation
    implementation("org.jline:jline-terminal:${Versions.Jline}")
    implementation("org.jline:jline-terminal-jansi:${Versions.Jline}")
    // TODO(#47): Windows currently not supported
    //runtimeOnly(files("libs/jansi-1.18.jar")) // Required for windows support

    // For GuardedBy concurrency annotation
    implementation("net.jcip:jcip-annotations:1.0")
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        if (shouldPublishToGCloud()) {
            repositories {
                maven { gcloudAuth() }
            }
        }

        create<MavenPublication>("konsole") {
            from(components["java"])
            pom {
                description.set("A declarative, Kotlin-idiomatic API for writing dynamic command line applications.")
                url.set("https://github.com/varabyte/konsole")
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
        sign(publishing.publications["konsole"])
    }
}