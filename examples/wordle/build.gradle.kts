plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "com.varabyte.kotter"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":kotter"))
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines)
}

application {
    applicationDefaultJvmArgs = listOf(
        // JDK24 started reporting warnings for libraries that use restricted native methods, at least one which Kotter
        // uses indirectly (via jline/jansi). It looks like this:
        //
        // WARNING: A restricted method in java.lang.System has been called
        // WARNING: java.lang.System::loadLibrary has been called by ...
        // WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
        // WARNING: Restricted methods will be blocked in a future release unless native access is enabled
        //
        // The best solution we have for now is to disable the warning by explicitly enabling access.
        // We also suggest the IgnoreUnrecognizedVMOptions flag here to allow kotter applications to be able to compile
        // with JDKs older than JDK24. You can remove it if you are intentionally using JDK24+.
        // See also: https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/doc-files/RestrictedMethods.html
        // And also: https://github.com/jline/jline3/issues/1067
        "-XX:+IgnoreUnrecognizedVMOptions",
        "--enable-native-access=ALL-UNNAMED",
    )
    mainClass.set("MainKt")
}
