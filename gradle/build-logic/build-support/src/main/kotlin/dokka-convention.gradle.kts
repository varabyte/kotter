plugins {
    id("org.jetbrains.dokka")
}

dokka {
    dokkaSourceSets.configureEach {
        pluginsConfiguration.html {
            homepageLink.set("https://github.com/varabyte/kotter")
            footerMessage.set("Copyright © ${java.time.Year.now().value} Varabyte")
        }

        sourceLink {
            // Safe relative path resolution without accessing rootProject state
            val rootDir = layout.settingsDirectory.asFile
            val path = project.projectDir.relativeTo(rootDir).invariantSeparatorsPath

            localDirectory = project.projectDir.resolve("src")
            remoteUrl("https://github.com/varabyte/kotter/tree/main/$path/src")
            remoteLineSuffix = "#L"
        }
    }
}