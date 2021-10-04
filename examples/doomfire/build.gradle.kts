plugins {
    kotlin("jvm")
    application
}

group = "com.varabyte.konsole"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":konsole"))
}

application {
    mainClass.set("MainKt")
}