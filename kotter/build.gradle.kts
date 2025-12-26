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
            implementation(libs.bundles.jline)

            // For GuardedBy concurrency annotation
            implementation(libs.jcip.annotations)
        }
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
 * Extract the line coverage calculation from a generated Kover XML report.
 *
 * It is expected that someone will call the `koverReportXml` task and set its output into [koverReportFile].
 */
abstract class PrintLineCoverageFromKoverTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val koverReportFile: RegularFileProperty

    @TaskAction
    fun run() {
        val report = koverReportFile.get().asFile

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report)
        val rootNode = doc.firstChild
        var childNode = rootNode.firstChild

        var coveragePercent = 0.0

        while (childNode != null) {
            if (childNode.nodeName == "counter") {
                val typeAttr = childNode.attributes.getNamedItem("type")
                if (typeAttr.textContent == "LINE") {
                    val missedAttr = childNode.attributes.getNamedItem("missed")
                    val coveredAttr = childNode.attributes.getNamedItem("covered")

                    val missed = missedAttr.textContent.toLong()
                    val covered = coveredAttr.textContent.toLong()

                    coveragePercent = (covered * 100.0) / (missed + covered)

                    break
                }
            }
            childNode = childNode.nextSibling
        }

        println("%.1f".format(coveragePercent))
    }
}

val koverXmlReportTask = tasks.named("koverXmlReport")
tasks.register<PrintLineCoverageFromKoverTask>("printLineCoverage") {
    group = "verification"
    dependsOn(koverXmlReportTask)
    koverReportFile.set(layout.buildDirectory.file("reports/kover/report.xml"))
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
