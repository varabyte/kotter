plugins {
    id("dokka-convention")
}

dependencies {
    dokka(project(":kotter"))
    dokka(project(":kotterx:kotter-test-support"))
}

dokka {
    moduleName.set("Kotter docs")
}
