plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "com.varabyte.kotter"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(libs.kotter)
    implementation(libs.kotlinx.coroutines)
}

application {
    applicationDefaultJvmArgs = listOf(
        "-XX:+IgnoreUnrecognizedVMOptions", // Unnecessary if you are building with JDK23+
        // Starting in JDK24, the Java runtime is getting a bit more strict about what code is allowed to make native
        // calls, which jline (a major dependency used by kotter) does. This is related to JPMS, the Java module system,
        // and you are supposed to whitelist modules that you grant heightened privileges for. Unfortunately, JPMS
        // doesn't play too well with Kotlin, and it's not straightforward or even that beneficial to modularize your
        // own application. For now, the easiest recommendation is to just disable the whole system.
        // See also: https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/doc-files/RestrictedMethods.html
        // See also: https://github.com/varabyte/kotter/issues/152
        "--enable-native-access=ALL-UNNAMED",
    )
    mainClass.set("com.varabyte.kotter.examples.hiragana.MainKt")
}
