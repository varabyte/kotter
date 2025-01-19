import javax.xml.parsers.DocumentBuilderFactory

plugins {
    id("kotter-build-support")
    alias(libs.plugins.kotlinx.kover)
}

group = "com.varabyte.kotter"
version = libs.versions.kotter.get()

kotlin {
    // Targets set in kotter-build-support plugin

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.truthish)
            implementation(project(":kotterx:kotter-test-support"))
        }

        jvmMain.dependencies {
            // For system terminal implementation
            implementation(libs.jline.terminal.core)
            implementation(libs.jline.terminal.jansi)
            runtimeOnly(files("libs/jansi-1.18.jar")) // Required for windows support

            // For GuardedBy concurrency annotation
            implementation(libs.jcip.annotations)
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

kover {
    reports {
        filters {
            excludes {
                classes(
                    // Untested session code is related to terminals
                    "com.varabyte.kotter.foundation.SessionKt*",
                    "com.varabyte.kotter.foundation.SessionSupportKt*",
                    // ANSI codes are not worth testing exhaustively. If we ever get a report that one of them is broken, we
                    // can add new tests elsewhere to cover them indirectly.
                    "com.varabyte.kotter.runtime.internal.ansi.Ansi*",
                    // Virtual terminal implementations are UI, not important to cover
                    "com.varabyte.kotter.terminal.*",
                )
            }
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
        val report = layout.buildDirectory.file("reports/kover/report.xml").get().asFile

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
        withType<MavenPublication> {
            pom {
                name.set("Kotter")
                description.set("A declarative, Kotlin-idiomatic API for writing dynamic command line applications.")
            }
        }
    }
}
