plugins {
    `kotlin-dsl`
}

group = "com.varabyte.kotter.gradle"
version = libs.versions.kotter.get()

dependencies {
    implementation(libs.kotlin.multiplatform.plugin)
    implementation(libs.jetbrains.dokka.plugin)
}
