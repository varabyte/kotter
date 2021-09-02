pluginManagement {
    plugins {
        kotlin("jvm") version "1.5.10" apply false
        `maven-publish` apply false
    }
}

rootProject.name = "konsole"

include(":konsole")

include(":examples:anim")
include(":examples:blink")
include(":examples:clock")
include(":examples:input")
include(":examples:life")
include(":examples:snake")
include(":examples:text")

include(":examples:mosaic:counter")
include(":examples:mosaic:jest")
include(":examples:mosaic:robot")