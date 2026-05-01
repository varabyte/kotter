plugins {
    id("org.jetbrains.dokka")
}

val skipDokka = (project.findProperty("kotter.publication.skipDokka") as? String)?.toBoolean() ?: false

if (!skipDokka) {
    dokka {
        dokkaSourceSets.configureEach {
            sourceLink {
                val path = project.projectDir.relativeTo(project.rootProject.projectDir).invariantSeparatorsPath
                localDirectory = project.projectDir.resolve("src")
                remoteUrl("https://github.com/varabyte/kotter/tree/main/$path/src")
                remoteLineSuffix = "#L"
            }
        }
    }
}