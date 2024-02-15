plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "com.varabyte.kotter.examples"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.varabyte.kotter:kotter")
}

application {
    mainClass.set("MainKt")
}
