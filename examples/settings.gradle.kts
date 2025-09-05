// include kotter
includeBuild("..") {
    dependencySubstitution {
        substitute(module("com.varabyte.kotter:kotter"))
            .using(project(":kotter"))

    }
}

include(":anim")
include(":blink")
include(":border")
include(":chatgpt")
include(":clock")
include(":compiler")
include(":doomfire")
include(":extend")
include(":grid")
include(":input")
include(":keys")
include(":life")
include(":list")
include(":mandelbrot")
include(":native")
include(":picker")
include(":sliding")
include(":snake")
include(":splash")
include(":text")
include(":wordle")

include(":mosaic:counter")
include(":mosaic:jest")
include(":mosaic:robot")
