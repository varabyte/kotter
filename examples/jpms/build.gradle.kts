import org.gradle.kotlin.dsl.dependencies

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "com.varabyte.kotter"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(libs.kotter)
    implementation(libs.kotterx.twemoji)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotterx.test.support)
}

application {
    applicationDefaultJvmArgs = listOf(
        // Ignoring unrecognized VM options is unnecessary if using a newer JVM. But otherwise, unknown params cause an
        // error at compile time when using older JDKs that aren't familiar with the feature.
        "-XX:+IgnoreUnrecognizedVMOptions",

        // The following hides a warning about native access caused by jline (the CLI library that Kotter delegates to)
        "--enable-native-access=org.jline.nativ",

        // The following is required to allow the Kotlin debugger to run on our tightened module setup
        "--add-modules=jdk.unsupported",
        "--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED",
        "--add-reads=kotlin.stdlib=kotlinx.coroutines.core"
    )

    mainModule.set("com.varabyte.kotter.examples.jpms")
    mainClass.set("${mainModule.get()}.MainKt")
}
