# Konsole

```kotlin
konsole {
    textLine("Would you like to learn Konsole? (Y/n)")
    text("> $input");
    blinkingCursor()
}.onInputEntered { evt ->
    if (evt.input.toLowercase().startsWith("y")) {
        p { textLine("""\(^o^)/""") }
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

## Gradle

(To be updated when this project is in a ready state)

## Usage

### Basic

The following is equivalent to `println("Hello, World")`. In this simple case, it's definitely overkill!

```kotlin
konsole {
  textLine("Hello, World")
}
```

### Background work

Konsole starts to show its strength when doing background work (or other async tasks like waiting for user input):

```kotlin
var result: Int? = null
konsole {
    text("Calculating... ")
    if (result != null) {
        textLine("Done! Result = $result")
    }
}.withBackgroundWork {
    result = doNetworkFetchAndExpensiveCalculation()
    // When background work finishes, the konsole block will run one last time
    // and then allow the program to continue.
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
}.withBackgroundWork(rerenderOnFinished = false) {
    // ^ rerenderOnFinished not required because we'll call it manually
    while (percent < 100f) {
        delay(100)
        percent += 1f
        rerender()
    }
}
```

### User input

Konsole, of course, handles user input as well. 

Konsole consumes keypresses, so as the user types into the console, nothing will show up unless you intentionally print
it. You can easily do this using the `input` property, which contains the user's input typed so far (excluding control
characters):

```kotlin
konsole {
    text("Please enter your name: $input")
    blinkingCursor()
}
```

You can respond to the input as it is typed by using the `onInput` event:

```kotlin
konsole {
    text("Please enter your name: $input")
    blinkingCursor()
}.onInput { evt ->
    evt.input = evt.input.filter { it.isLetter() }
}
```

If you don't care about intermediate states, you can also use `onInputEntered`. This will be triggered with an event
that contains the user's input string after they pressed the ENTER key.

Here, we tweak the example from the beginning of this README:

```kotlin
var wantsToLearn = false
konsole {
    textLine("Would you like to learn Konsole? (Y/n)")
    text("> ")
    blinkingCursor() 
    // `input` is a property that contains the user's input typed so far in
    // this konsole block. It is automatically updated and the block rerendered
    // when it changes.
    if ("yes".startsWith(input)) {
        grey()
        text("yes".substringAfter(input))
    }
    if (wantsToLearn) {
        p {
            textLine("""\(^o^)/""")
        }
    }
}.onInputEntered { evt ->
    if ("yes".startsWith(evt.input)) {
        evt.input = "yes" // Update the input to make it feel like we autocompleted their answer
        wantsToLearn = true
    }
}
```

This will cause the following to be printed to the console:

```bash
Would you like to learn Konsole? (Y/n)
> |yes
```

After the user presses "ye":

```bash
Would you like to learn Konsole? (Y/n)
> ye|s
```

And after the user presses enter:

```bash
Would you like to learn Konsole? (Y/n)
> yes

\(^o^)/

(Next line of text will go here...)
```

A common situation in a console is preventing bad input. To prevent `onInputEntered` from advancing blindly, you can
reject its input using `evt.rejectInput()`:

```kotlin
val VALID_ANSWERS = setOf("yes", "no")
var errorMessage: String? = null
konsole {
    text("Would you like to learn Konsole? (Y/n)")
    text("> $input");
    blinkingCursor()
    if (errorMessage != null) {
        newLine()
        red()
        textLine(errorMessage)
    }
}.onInputEntered { evt ->
    if (!VALID_ANSWERS.any { it.startsWith(evt.input) }) {
        evt.rejectInput()
        errorMessage = "Please try again. Reason: \"$evt.input\" was invalid"
    }
    else if ("yes".startsWith(evt.input)) { /* ... */ }
}
```

### Colors

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

Colors can be turned off by modifying the global Konsole settings:

```kotlin
KonsoleSettings.colorsEnabled = false
```

### State

To reduce the chance of introducing unexpected bugs later, state changes (like colors) will be localized to the current
block only:

```kotlin
konsole {
    blue(BG)
    red()
    text("This text is red on blue")
}

konsole {
    text("This text is rendered using default colors")
}
```

If you intentionally want to set a custom, global theme, you can do this by updating the settings:

```kotlin
KonsoleSettings.baseStyle = konsoleStyle {
    blue(BG)
    red()
}
```

You can also save styles into reusable variables:

```kotlin
val titleStyle = konsoleStyle {
    red()
    underline()
}

konsole {
    style(titleStyle)
    text("Objective")
}

konsole {
    text(".......")
}

konsole {
    style(titleStyle)
    text("Background")
}

/* ... etc ... */
```

### Animations

Blinking cursors are actually a built-in animation:

```kotlin
konsole {
    text("Cursor >>> ")
    // Calls something like `animation(BLINKING_CURSOR)` under the hood
    blinkingCursor()
    text(" <<<")
}.onInputEntered { /* ... */ }
```

But you can easily create custom animations as well, by implementing the `KonsoleAnimation` interface and then
instantiating an animation instance from it:

```kotlin
val SPINNER = object : KonsoleAnimation(Duration.ofMillis(250)) {
    override val frames = arrayOf("\\", "|", "/", "-") 
}

var finished = false
val spinner = SPINNER.createInstance()
konsole {
    if (!finished) {
        animation(spinner)
    }
    else {
        text("✓")
    }
    text(" Searching for files...")
    if (finished) {
        textLine(" Done!")
    }
}.withBackgroundWork {
    doExpensiveFileSearching()
    finished = true
}
```

If it's a one-use animation that you don't want to share as a template, you can create the instance directly of course:

```kotlin
val spinner = object : KonsoleAnimation(Duration.ofMillis(250)) {
    override val frames = arrayOf("\\", "|", "/", "-") 
}.createInstance()
```

Here's a way to create the progress bar from earlier using animations:

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
}.withBackgroundWork {
    while (progressBar.currFrame < progressBar.numFrames) {
        delay(1000)
        progressBar.advance()
    }
}
```

## Advanced

### Thread Affinity

Konsole is single-threaded (implemented in a flexible way, using ThreadLocal, so that we can review this decision
later). I made this decision so that:

* I don't have to worry about multiple Konsole blocks println'ing at the same time - who likes clobbered text?
* The API is heavily stateful, similar in some ways to an API like OpenGL, and limiting possible interactions across
Konsole blocks means that the mental model for users will be less overloaded. 

As a user of the API, you don't need to worry about setting the thread yourself. The first time you call `konsole` or
if you change a global setting via the `KonsoleSetting` class, this will be done for you.

By default, Konsole creates a separate thread, and when you call `konsole { ... }`, the current thread is blocked until
the Konsole thread signals that everything can proceed.

### Why Not Compose?

Konsole's API is inspired by Compose, in that it has a core block which gets rerun for you automatically as necessary
without you having to worry about it. Why not just Compose directly?

In fact, this is exactly what [Jake Wharton's Mosaic](https://github.com/JakeWharton/mosaic) is doing. I tried using it
first, before deciding to implement Konsole, for two reasons:

1. Compose is tightly tied to the current Kotlin compiler version, which means if you are targeting a particular
version of the Kotlin language, you can easily see the dreaded error message: `This version (x.y.z) of the Compose
Compiler requires Kotlin version a.b.c but you appear to be using Kotlin version d.e.f which is not known to be
compatible.` I suspect this issue with Compose will improve over time, but for the present, and for something as simple
as a glorified console printer, I didn't want to tie things down.

2. Compose is great for rendering a whole, interactive UI, but console printing is often two parts: the active part that
the user is interacting with, and the history, which is static. To support this with Compose, you'd need to manage the
history list yourself and keep appending to it, and it was while playing with an API thinking about this limitation that
I instead decided to work on Konsole.

### Tested Platforms

* [x] Linux
* [ ] Mac
* [ ] Windows