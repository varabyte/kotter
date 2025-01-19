plugins {
    `kotlin-dsl`
}

group = "com.varabyte.kotter.gradle"

dependencies {
    implementation(libs.kotlin.multiplatform.plugin)
    implementation(libs.jetbrains.dokka.plugin)
}
