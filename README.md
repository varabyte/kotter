# Konsole

```kotlin
konsole {
    textLine("Would you like to learn Konsole? (Y/n)")
    text("> ");
    blinkingCursor()
}.waitForInput { input ->
    if (input.startsWith("Y")) {
        textLine("""\(^o^)/""") 
    }
}
```

---

Konsole aims to be a relatively thin, Kotlin-idiomatic API that provides useful functionality for writing delightful
command line applications. It strives to keep things simple, providing a solution a bit more interesting than making
raw `println` calls but way less featured than something like _Java Curses_.

Specifically, this library helps with:

* Colors
* Repainting existing lines
* Support for animated behavior
* Handling user input

# Usage

## Basic

The following is equivalent to `println("Hello, World")`. In this simple case, it's definitely overkill!

```kotlin
konsole {
  textLine("Hello, World")
}
```

## Background work

Konsole starts to show its strength when doing background work (or other async tasks like waiting for user input):

```kotlin
var result: Int? = null
konsole {
    text("Calculating... ")
    if (result != null) {
        textLine("Done! Result = $result")
    }
}.waitForBackgroundWork {
    result = doNetworkFetchAndExpensiveCalculation()
}
```

You can use it for something like a progress bar:

```kotlin
var percent = 0f
konsole {
    text("[")
    val numSquares = 10
    val numCompleteSquares = percent * numSquares
    for (val i in 0 until numSquares) {
        text(if (i < numCompleteSquares) "*" else "-")
    }
    text("]")
}.waitForBackgroundWork {
    while (percent < 100f) {
        delay(100)
        percent += 1f
        rerender()
    }
}
```

## Colors

You can call color methods directly:

```kotlin
konsole {
    green(layer = BG)
    red() // defaults to FG layer
    text("Hello")
    clearColor()
    text(" ")
    blue()
    text("World")
    clearColor(layer = BG)
}
```

or you can use scoped helper versions that handle clearing colors for you automatically at the end of their block:

```kotlin
konsole {
    green(layer = BG) {
        red {
            text("Hello")
        }
        text(" ")
        blue {
            text("World")
        }
    }
}
```

There are constants you can use for within template strings as well, if you prefer it:

```kotlin
konsole {
    text("${RED}Hello${CLEAR} ${BLUE}World")
}
```

If you want to change the foreground and background at the same time:

```kotlin
konsole {
    colors(fg = RED, bg = BLUE)
    text("Hello world")
    clearColors()
}
```

or the scoped helper version:

```kotlin
konsole {
    colors(fg = RED, bg = BLUE) { text("Hello world") }
}
```

Finally, as some terminals don't support colors, note that they can be turned off by modifying the global Konsole
settings:

```kotlin
KonsoleSettings.colorsEnabled = false
```

## Animations

Blinking cursors are actually a built-in animation:

```kotlin
konsole {
    text("Cursor >>> ")
    // Calls something like `animation(BLINKING_CURSOR)` under the hood
    blinkingCursor()
    text(" <<<")
}.waitForInput { /* ... */ }
```

But you can easily create custom animations as well, by implementing the `KonsoleAnimation` interface:

```kotlin
val SPINNER = object : KonsoleAnimation(Duration.ofMillis(250)) {
    override val frames = arrayOf("\\", "|", "/", "-") 
}

var finished = false
konsole {
    if (!finished) {
        animation(SPINNER)
    }
    else {
        text("✓")
    }
    text(" Searching for files...")
    if (finished) {
        textLine(" Done!")
    }
}.waitForBackgroundWork {
    doExpensiveFileSearching()
    finished = true
}
```

Here's a way to create the progress bar from earlier using animations. Note that you have to instantiate the animation
yourself, so you can advance the frames manually:

```kotlin
val PROGRESS_BAR = object : KonsoleAnimation(Duration.MAX_VALUE) {
    override val frames = arrayOf(
        "[----------]",
        "[*---------]",
        "[**--------]",
        "[***-------]",
        "[****------]",
        "[*****-----]",
        "[******----]",
        "[*******---]",
        "[********--]",
        "[*********-]",
        "[**********]",
    )
}

val progressBar = PROGRESS_BAR.createInstance()
konsole {
    animation(progressBar)
}.waitForBackgroundWork {
    while (progressBar.currFrame < progressBar.numFrames) {
        delay(1000)
        progressBar.advance()
    }
}
```

# Gradle

(To be updated when this project is in a ready state)
