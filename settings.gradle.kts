pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    includeBuild("gradle/build-logic")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            mavenContent { includeGroup("com.varabyte.truthish") }
        }
    }
}

rootProject.name = "kotter"

include(":kotter")
include(":kotterx:kotter-test-support")
include(":docs")

