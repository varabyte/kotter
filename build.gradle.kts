// Note: This seems to need to exist here and NOT in settings.gradle.kts pluginManagment, or else Gradle/Kotlin whines
// at build about the same plugin being loaded multiple times.
// See also: https://docs.gradle.org/current/userguide/plugins.html#sec:subprojects_plugins_dsl
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinx.kover) apply false
    alias(libs.plugins.jetbrains.dokka) apply false
    alias(libs.plugins.nexus.publish)
}

group = "com.varabyte.kotter"
version = libs.versions.kotter.get()

nexusPublishing {
    repositories {
        sonatype {  //only for users registered in Sonatype after 24 Feb 2021
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            (findProperty("ossrhToken") as? String)?.let { username.set(it) }
            (findProperty("ossrhTokenPassword") as? String)?.let { password.set(it) }
        }
    }
}
