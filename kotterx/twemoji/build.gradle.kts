plugins {
    id("kotter-publication")
}

group = "com.varabyte.kotterx"
version = libs.versions.kotter.get()

kotlin {
    // Targets set in kotter-build-support plugin

    sourceSets {
        jvmMain {
            dependencies {
                implementation(project(":kotter"))
                implementation(libs.jsvg)
            }
        }
    }
}

kotterPublication {
    name.set("Kotter virtual terminal Twemoji renderer")
    description.set("Provides SVG emoji icons that can be used by the virtual terminal for a consistent, cross-platform experience")
}
