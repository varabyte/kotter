// Note: This seems to need to exist here and NOT in settings.gradle.kts pluginManagment, or else Gradle/Kotlin whines
// at build about the same plugin being loaded multiple times.
// See also: https://docs.gradle.org/current/userguide/plugins.html#sec:subprojects_plugins_dsl
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlinx.kover) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.jetbrains.dokka) apply false
}
