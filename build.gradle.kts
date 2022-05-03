// Note: This seems to need to exist here and NOT in settings.gradle.kts pluginManagment, or else Gradle/Kotlin whines
// at build about the same plugin being loaded multiple times.
// See also: https://docs.gradle.org/current/userguide/plugins.html#sec:subprojects_plugins_dsl
plugins {
    kotlin("jvm") version "1.6.10" apply false
    id("org.jetbrains.compose") version "1.1.0" apply false
}
