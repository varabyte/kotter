# Konsole

```kotlin
konsole {
    textLine("Would you like to learn Konsole? (Y/n)")
    text("> $input");
    blinkingCursor()
}.onInputEntered {
    if (input.toLowercase().startsWith("y")) {
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
}.run()
```

`konsole { ... }` defines a `KonsoleBlock` which, on its own, is inert. It needs to be run at least once to output text
to the console. Above, we use the `run` method above to trigger this. `run` will execute the block once, blocking the
program until it is finished running.

Only one block can run at a time, at which point it's active and dynamic. After it has finished running, it becomes
static history, at which point a new block usually takes over.

Although here we just ran once, Konsole starts to show its strength when doing background work (or other async tasks
like waiting for user input) that cause the block to update several times, which we'll see many examples of  in the
following sections.

### Background work

By default, a Konsole block will run in a loop and wait for the user to call `advance`, but it's more common to register
event handlers that do this for us.

The first handler we'll look at is `withBackgroundWork`, which is a suspend function that runs on a background thread
automatically, and when it finishes, triggers the Konsole block to rerun one last time before advancing.

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
const val BAR_LENGTH = 100
var percent = 0
konsole {
    text("[")
    val numComplete = percent * BAR_LENGTH
    for (val i in 0 until BAR_LENGTH) {
        text(if (i < numComplete) "*" else "-")
    }
    text("]")
}.withBackgroundWork {
    // rerenderOnFinished not required because we'll call it manually
    rerenderOnFinished = false
    while (percent < 100) {
        delay(100)
        percent += 1
        rerender()
    }
}
```

### Cursor

By default, Konsole doesn't display a cursor indicator, but you can easily do it yourself by calling `blinkingCursor`:

```kotlin
konsole {
    text("Cursor is here >>> ")
    blinkingCursor()
    text(" <<<")
}.onInputEntered { /* ... */ }
```

The next section goes over reading in user input, which works well with the cursor.

### User input

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
}.onInput {
    // Reject invalid name characters
    input = input.filter { it.isLetter() }
}
```

If you don't care about intermediate states, you can also use `onInputEntered`. This will be triggered whenever the user
presses the ENTER key.

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
        grey {
            text("yes".substringAfter(input))
        }
    }
    if (wantsToLearn) {
        p {
            textLine("""\(^o^)/""")
        }
    }
}.onInputEntered {
    if ("yes".startsWith(input)) {
        input = "yes" // Update the input to make it feel like we autocompleted their answer
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

A common situation in a console is responding to bad input. To prevent `onInputEntered` from advancing blindly, you can
reject its input using `rejectInput()`:

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
}.onInputEntered {
    if (!VALID_ANSWERS.any { it.startsWith(input) }) {
        rejectInput()
        errorMessage = "Please try again. Reason: \"$input\" was invalid"
    }
    else if ("yes".startsWith(evt.input)) { /* ... */ }
}
```

### Text Effects

You can call color methods directly:

```kotlin
konsole {
    green(layer = BG)
    red() // defaults to FG layer
    text("Hello")
    clearColor() // defaults to FG layer
    text(" ")
    blue()
    text("World")
    clearColor()
    clearColor(layer = BG)
    // ^ You could also have used clearColors() instead of calling clearColor twice
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

You could do this yourself manually, using the `scopedState` method, which is what the scoped color methods are doing
for you under the hood:

```kotlin
konsole {
    scopedState {
        green(layer = BG)
        scopedState {
            red()
            text("Hello")
        }
        text(" ")
        scopedState {
            blue()
            text("World")
        }
    }
}
```

While `scopedState` is more verbose for the single color case, it can be useful if you want to change the foreground and
background colors at the same time:

```kotlin
konsole {
    scopedState {
        red()
        blue(BG)
        text("Hello world")
    }
}
```

### State

To reduce the chance of introducing unexpected bugs later, state changes (like colors) will be localized to the current
`konsole` block only:

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

If you intentionally want to set a custom style that lasts across blocks, the recommended approach is to create your own
custom konsole method:

```kotlin
fun KonsoleScope.customStyle() {
    blue(BG)
    red()
}

fun customKonsole(block: KonsoleScope.() -> Unit) {
    konsole {
        customStyle()
        block()
    }
}

fun main() {
    customKonsole {
        text("This text is red on blue")
    }
}
```

You can also use this pattern for reusable styles:

```kotlin
fun KonsoleScope.titleStyle() {
    red()
    underline()
}

konsole {
    titleStyle()
    text("Objective")
}

konsole {
    text("... normal text ...")
}

konsole {
    titleStyle()
    text("Background")
}

/* ... etc ... */
```

or, if you're feeling fancy, use `scopedState` and take in a block as an argument to your method:

```kotlin
fun KonsoleScope.titleStyle(block: KonsoleScope.() -> Unit) {
    scopedState {
        red()
        underline()
        block()
    }
}

konsole {
    titleStyle {
        text("Objective")
    }
    text("... normal text ...")
    titleStyle {
        text("Background")
    }
}
```

### Animations

You can easily create custom animations, by implementing the `KonsoleAnimation` interface and then instantiating an
animation instance from it:

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

### Virtual Terminal

It's not guaranteed that every user's command line setup supports ANSI codes. For example, debugging this project with
IntelliJ as well as Gradle are two such environments! According to many online reports, Windows is also a big offender
here.

Konsole will attempt to detect if your console does not support the features it uses, and if not, it will open up a
fake virtual terminal backed by Swing. This workaround gives us better cross-platform support.

TODO: Write how you can intentionally trigger the virtual terminal yourself when this code is done. 

### Why Not Compose?

Konsole's API is inspired by Compose, in that it has a core block which gets rerun for you automatically as necessary
without you having to worry about it. Why not just use Compose directly?

In fact, this is exactly what [Jake Wharton's Mosaic](https://github.com/JakeWharton/mosaic) is doing. I tried using it
first but ultimately decided against it before deciding to write Konsole, for the following reasons:

* Compose is tightly tied to the current Kotlin compiler version, which means if you are targeting a particular
version of the Kotlin language, you can easily see the dreaded error message: `This version (x.y.z) of the Compose
Compiler requires Kotlin version a.b.c but you appear to be using Kotlin version d.e.f which is not known to be
compatible.` I suspect this issue with Compose will improve over time, but for the present, and for something as simple
as a glorified console printer, I didn't want to tie things down.

* Compose is great for rendering a whole, interactive UI, but console printing is often two parts: the active part that
the user is interacting with, and the history, which is static. To support this with Compose, you'd need to manage the
history list yourself and keep appending to it, and it was while thinking about an API that addresses this limitation
that I envisioned Konsole.

### Tested Platforms

* [x] Linux
* [ ] Mac
* [ ] Windows