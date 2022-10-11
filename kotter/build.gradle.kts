import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    kotlin("jvm")
    `maven-publish`
    signing
    id("org.jetbrains.kotlinx.kover")
}

group = "com.varabyte.kotter"
version = "1.0.0-rc2"

fun shouldSign() = (findProperty("kotter.sign") as? String).toBoolean()
fun shouldPublishToGCloud(): Boolean {
    return (findProperty("kotter.gcloud.publish") as? String).toBoolean()
            && findProperty("gcloud.artifact.registry.secret") != null
}
fun shouldPublishToMavenCentral(): Boolean {
    // Only publish snapshots to our varabyte repo for now, we may change our mind later
    return !version.toString().endsWith("SNAPSHOT")
            && (findProperty("kotter.maven.publish") as? String).toBoolean()
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

object Versions {
    object Kotlin {
        const val Couroutines = "1.6.4"
    }
    const val Jline = "3.21.0"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.Kotlin.Couroutines}")

    // For system terminal implementation
    implementation("org.jline:jline-terminal:${Versions.Jline}")
    implementation("org.jline:jline-terminal-jansi:${Versions.Jline}")
    runtimeOnly(files("libs/jansi-1.18.jar")) // Required for windows support

    // For GuardedBy concurrency annotation
    implementation("net.jcip:jcip-annotations:1.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("com.varabyte.truthish:truthish:0.6.3")
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

kover {
    filters {
        classes {
            excludes += listOf(
                "com.varabyte.kotter.terminal.*", // Virtual terminal implementations are UI, not important to cover
                "com.varabyte.kotter.foundation.SessionKt*", // Untested session code is related to terminals
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
        val report = file("$buildDir/reports/kover/xml/report.xml")

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

        create<MavenPublication>("kotter") {
            val githubPath = "https://github.com/varabyte/kotter"
            from(components["java"])
            pom {
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
    signing {
        // Signing requires following steps at https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials
        // and adding singatory properties somewhere reachable, e.g. ~/.gradle/gradle.properties
        sign(publishing.publications["kotter"])
    }
}