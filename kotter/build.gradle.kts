import org.jetbrains.dokka.gradle.DokkaTask
import javax.xml.parsers.DocumentBuilderFactory

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    `maven-publish`
    signing
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.jetbrains.dokka)
}

group = "com.varabyte.kotter"
version = libs.versions.kotter.get()

kotlin {
    jvm {
        jvmToolchain(11)

        val main by compilations.getting {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    targets.all {
        compilations.all {
            kotlinOptions {
                // Posix APIs are experimental (but hopefully we're not using anything too controversial)
                freeCompilerArgs += listOf("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
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
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.truthish)
                implementation(project(":kotterx:kotter-test-support"))
            }
        }

        val jvmMain by getting {
            dependencies {
                // For system terminal implementation
                implementation(libs.jline.terminal.core)
                implementation(libs.jline.terminal.jansi)
                runtimeOnly(files("libs/jansi-1.18.jar")) // Required for windows support

                // For GuardedBy concurrency annotation
                implementation(libs.jcip.annotations)
            }
        }

        val nativeMain by creating { dependsOn(commonMain) }
        val posixMain by creating { dependsOn(nativeMain) }

        val linuxMain by creating { dependsOn(posixMain) }
        val linuxX64Main by getting { dependsOn(linuxMain) }

        val macosMain by creating { dependsOn(posixMain) }
        val macosArm64Main by getting { dependsOn(macosMain) }
        val macosX64Main by getting { dependsOn(macosMain) }

        val winMain by creating { dependsOn(nativeMain) }
        val mingwX64Main by getting { dependsOn(winMain) }
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

kover {
    filters {
        classes {
            excludes += listOf(
                "com.varabyte.kotter.foundation.SessionKt*", // Untested session code is related to terminals
                // ANSI codes are not worth testing exhaustively. If we ever get a report that one of them is broken, we
                // can add new tests elsewhere to cover them indirectly.
                "com.varabyte.kotter.runtime.internal.ansi.Ansi*",
                "com.varabyte.kotter.terminal.*", // Virtual terminal implementations are UI, not important to cover
            )
        }
    }
}

/**
 * A task which outputs *just* the line coverage value (as a percent) from the Kover report.
 *
 * For example, this might output just the text `65.3` for a project that is covering 653 out of 1000 lines.
 *
 * This is a useful value to expose for GitHub CI actions, allowing us to create a custom code coverage badge.
 */
tasks.register("printLineCoverage") {
    group = "verification"
    dependsOn("koverXmlReport")
    doLast {
        val report = layout.buildDirectory.file("reports/kover/xml/report.xml").get().asFile

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report)
        val rootNode = doc.firstChild
        var topLevelNode = rootNode.firstChild

        var coveragePercent = 0.0

        // The example snippet of XML we want to parse:
        //
        // <?xml version="1.0" ?>
        // <report name="Intellij Coverage Report">
        //   ...
        //   <counter type="INSTRUCTION" missed="6591" covered="5058"/>
        //   <counter type="BRANCH" missed="565" covered="236"/>
        //   <counter type="LINE" missed="809" covered="700"/>
        //   <counter type="METHOD" missed="375" covered="386"/>
        //   <counter type="CLASS" missed="194" covered="156"/>
        // </report>
        //
        // Particularly, we want to extract the "missed" and "covered" LINE values.
        while (topLevelNode != null) {
            if (topLevelNode.nodeName == "counter") {
                val typeAttr = topLevelNode.attributes.getNamedItem("type")
                if (typeAttr.textContent == "LINE") {
                    val missedAttr = topLevelNode.attributes.getNamedItem("missed")
                    val coveredAttr = topLevelNode.attributes.getNamedItem("covered")

                    val missed = missedAttr.textContent.toLong()
                    val covered = coveredAttr.textContent.toLong()

                    coveragePercent = (covered * 100.0) / (missed + covered)

                    break
                }
            }
            topLevelNode = topLevelNode.nextSibling
        }

        println("%.1f".format(coveragePercent))
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
