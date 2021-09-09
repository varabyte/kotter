![version: 0.9.0](https://img.shields.io/badge/konsole-v0.9.0-blue)

# Konsole

```kotlin
konsoleApp {
  var wantsToLearn by konsoleVarOf(false)
  konsole {
    text("Would you like to learn "); cyan { text("Konsole") }; textLine("? (Y/n)")
    text("> "); input(Completions("yes", "no"))

    if (wantsToLearn) {
      yellow(isBright = true) { p { textLine("""\(^o^)/""") } }
    }
  }.runUntilInputEntered {
    onInputEntered { wantsToLearn = "yes".startsWith(input.lowercase()) }
  }
}
```

![Code sample in action](examples/input/konsole-input.gif)

---

Konsole aims to be a relatively thin, declarative, Kotlin-idiomatic API that provides useful functionality for writing
delightful command line applications. It strives to keep things simple, providing a solution a bit more opinionated than
making raw `println` calls but way less featured than something like _Java Curses_.

Specifically, this library helps with:

* Setting colors and text decorations (e.g. underline, bold)
* Handling user input
* Creating timers and animations
* Seamlessly repainting terminal text when values change

## Gradle

### Dependency

The artifacts for this project are hosted in our own artifact repository, so to include Konsole in your project, modify
your Gradle build file as follows:

```groovy
repositories {
  /* ... */
  maven { url 'https://us-central1-maven.pkg.dev/varabyte-repos/public' }
}

dependencies {
  /* ... */
  implementation 'com.varabyte:konsole:0.9.0'
}
```

### Running examples

If you've cloned this repository, examples are located under the [examples](examples) folder. To try one of them, you
can navigate into it on the command line and run it via Gradle.

```bash
$ cd examples/life
$ ../../gradlew run
```

However, because Gradle itself has taken over the terminal to do its own fancy command line magic, the example will
actually open up and run inside a virtual terminal.

If you want to run the program directly inside your system terminal, which is hopefully the way most users will see your
application, you should use the `installDist` task to accomplish this:

```bash
$ cd examples/life
$ ../../gradlew installDist
$ cd build/install/life/bin
$ ./life
```

***Note:** If your terminal does not support features needed by Konsole, then this still may end up running inside a
virtual terminal.*

## Usage

### Basic

The following is equivalent to `println("Hello, World")`. In this simple case, it's definitely overkill!

```kotlin
konsoleApp {
  konsole {
    textLine("Hello, World")
  }.run()
}
```

`konsole { ... }` defines a `KonsoleBlock` which, on its own, is inert. It needs to be run to output text to the
console. Above, we use the `run` method above to trigger this. The method blocks until the render is finished (which,
for console text, probably won't be very long).

While the above simple case is a bit verbose for what it's doing, Konsole starts to show its strength when doing
background work (or other async tasks like waiting for user input) during which time the block may update several times.
We'll see many examples throughout this document later.

### Text Effects

You can call color methods directly, which remain in effect until the next color method is called:

```kotlin
konsole {
  green(layer = BG)
  red() // defaults to FG layer
  textLine("Red on green")
  blue()
  textLine("Blue on green")
}.run()
```

or, if you only want the color effect to live for a limited time, you can use scoped helper versions that handle
clearing colors for you automatically at the end of their block:

```kotlin
konsole {
  green(layer = BG) {
    red {
      textLine("Red on green")
    }
    textLine("Default on green")
    blue {
      textLine("Blue on green")
    }
  }
}.run()
```

Various text effects are also available:

```kotlin
konsole {
  bold {
    textLine("Title")
  }

  p {
    textLine("This is the first paragraph of text")
  }

  p {
    text("This paragraph has an ")
    underline { text("underlined") }
    text(" word in it")
  }
}.run()
```

### Konsole state and scopedState

To reduce the chance of introducing unexpected bugs later, state changes (like colors) will be localized to the current
`konsole` block only:

```kotlin
konsole {
  blue(BG)
  red()
  text("This text is red on blue")
}.run()

konsole {
  text("This text is rendered using default colors")
}.run()
```

Within a Konsole block, you can also use the `scopedState` method. This creates a new scope within which any state will
be automatically discarded after it ends.

```kotlin
konsole {
  scopedState {
    red()
    blue(BG)
    underline()
    text("Underlined red on blue")
  }
  text("Text without color or decorations")
}.run()
```

This is what the scoped text effect methods (like `red { ... }`) are doing for you under the hood, actually.

### Dynamic Konsole block

The `konsole` block is designed to be run one _or more_ times. That is, you can write logic inside it which may not get
executed on the first run but will be on a followup run.

Here, we pass in a callback to the `run` method which updates a value referenced by the `konsole` block (the `result`
integer). This example will run the Konsole block twice - once when `run` is first called and again when it calls
`rerender`:

```kotlin
var result: Int? = null
konsole {
  text("Calculating... ")
  if (result != null) {
    text("Done! Result = $result")
  }
  textLine()
}.run {
  result = doNetworkFetchAndExpensiveCalculation()
  rerender()
}
```

The `run` callback automatically runs on a background thread for you (as a suspend function, so you can call other
suspend methods from within it).

Unlike using `run` without a callback, here your program will be blocked until the callback has finished (or, if it
has triggered a rerender, until the last rerender finishes after your callback is done).

#### KonsoleVar

As you can see above, the `run` callback uses a `rerender` method, which you can call to request another render pass.

However, remembering to call `rerender` yourself is potentially fragile and could be a source of bugs in the future when
trying to figure out why your console isn't updating.

For this purpose, Konsole provides the `KonsoleVar` class, which, when modified, will automatically request a rerender
to the last block that referenced it. An example will demonstrate this in action shortly.

To create a `KonsoleVar`, simply change a line like:

```kotlin
konsoleApp {
  var result: Int? = null
  /* ... */
}
```

to:

```kotlin
konsoleApp {
  var result by konsoleVarOf<Int?>(null)
  /* ... */
}
```

***Note:** The `konsoleVarOf` method can't be called outside of the `konsoleApp` block. For many remaining examples,
we'll elide the `konsoleApp` boilerplate, but that doesn't mean you can omit it in your own program!*

Let's apply `konsoleVarOf` to our earlier example in order to remove the `rerender` call:

```kotlin
var result by konsoleVarOf<Int?>(null)
konsole {
  /* ... no changes ... */
}.run {
  result = doNetworkFetchAndExpensiveCalculation()
}
```

And done! Fewer lines and less error pone.

Here's another example, showing how you can use `run` for something like a progress bar:

```kotlin
// Prints something like: [****------]
val BAR_LENGTH = 10
var numFilledSegments by konsoleVarOf(0)
konsole {
  text("[")
  for (i in 0 until BAR_LENGTH) {
    text(if (i < numFilledSegments) "*" else "-")
  }
  text("]")
}.run {
  var percent = 0
  while (percent < 100) {
    delay(Random.nextLong(10, 100))
    percent += Random.nextInt(1,5)
    numFilledSegments = ((percent / 100f) * BAR_LENGTH).roundToInt()
  }
}
```

#### KonsoleList

Similar to `KonsoleVar`, a `KonsoleList` is a reactive primitive which, when modified by having elements added to or
removed from it, causes a rerender to happen automatically. You don't need to use the `by` keyword with `KonsoleList`.
Instead, in a `konsoleApp` block, use the `konsoleListOf` method:

```kotlin
val fileWalker = FileWalker(".")
val fileMatches = konsoleListOf<String>()
konsole {
  textLine("Matches found so far: ")
  for (match in fileMatches) {
    textLine(" - $match")
  }
}.run {
  fileWalker.findFiles("*.txt") { file ->
    fileMatches += file.name
  }
  /* ... */
}
```

The `KonsoleList` class is thread safe, but you can still run into trouble if you check multiple values on the list one
after the other, as a lock is released between each check, and if multiple threads are hammering on the same list at the
same time, it's always possible that you missed an update. To handle this, you can use the `KonsoleList#withLock`
method:

```kotlin
val fileMatches = konsoleListOf<String>()
konsole {
  fileMatches.withLock {
    if (isEmpty()) {
      textLine("No matches found so far")
    } else {
      textLine("Matches found so far: ")
      for (match in this) {
        textLine(" - $match")
      }
    }
  }
}.run {
  /* ... */
}
```

The general rule of thumb is: use `withLock` if you want to access or modify more than one property from the list
at the same time.

#### Signals and waiting

A common pattern is for the `run` block to wait for some sort of signal before finishing, e.g. in response to some
event. You could always use a general threading trick for this, such as a `CountDownLatch` or a
`CompletableDeffered<Unit>` to stop the block from finishing until you're ready:

```kotlin
val fileDownloader = FileDownloader("...")
fileDownloader.start()
konsole {
  /* ... */
}.run {
  val finished = CompletableDeffered<Unit>()
  fileDownloader.onFinished += { finished.complete(Unit) }
  finished.await()
}
```

but, for convenience, Konsole provides the `signal` and `waitForSignal` methods, which do this for you.

```kotlin
val fileDownloader = FileDownloader("...")
konsole {
  /* ... */
}.run {
  fileDownloader.onFinished += { signal() }
  waitForSignal()
}
```

These methods are enough in most cases. Note that if you call `signal` before you reach `waitForSignal`, then
`waitForSignal` will just pass through without stopping.

Alternately, there's a `runUntilSignal` you can use, within which you don't need to call `waitForSignal` yourself:

```kotlin
val fileDownloader = FileDownloader("...")
konsole {
  /* ... */
}.runUntilSignal {
  fileDownloader.onFinished += { signal() }
}
```

This is actually a bit more powerful than the above `run` + `waitForSignal` version, as `signal` will quit your `run`
block even if it's still running. This could be useful if you have a forever-running while loop that should abort when
the user or some external event indicates they should quit.

### User input

#### Typed input

Konsole consumes keypresses, so as the user types into the console, nothing will show up unless you intentionally print
it. You can easily do this using the `input` method, which handles listening to kepresses and adding text into your
Konsole block at that location:

```kotlin
konsole {
  // `input` is a method that appends the user's input typed so far in this
  // konsole block. If your block uses it, the block is automatically
  // rerendered when it changes.
  text("Please enter your name: "); input()
}.run { /* ... */ }
```

Note that the input method automatically adds a cursor for you. This also handles keys like LEFT/RIGHT and HOME/END,
moving the cursor back and forth between the bounds of the input string.

You can intercept input as it is typed using the `onInputChanged` event:

```kotlin
konsole {
  text("Please enter your name: "); input()
}.run {
  onInputChanged {
    input = input.toUpperCase()
  }
  /* ... */
}
```

You can also use the `rejectInput` method to return your input to the previous (presumably valid) state.

```kotlin
konsole {
  text("Please enter your name: "); input()
}.run {
  onInputChanged {
    if (input.any { !it.isLetter() }) { rejectInput() }
    // Would also work: input = input.filter { it.isLetter() }
  }
  /* ... */
}
```

You can also use `onInputEntered`. This will be triggered whenever the user presses the ENTER key.

```kotlin
var name = ""
konsole {
  text("Please enter your name: "); input()
}.runUntilSignal {
  onInputChanged { input = input.filter { it.isLetter() } }
  onInputEntered { name = input; signal() }
}
```

There's actually a shortcut for cases like the above, since they're pretty common: `runUntilInputEntered`.
Using it, we can slightly simplify the above example, typing fewer characters for identical behavior:

```kotlin
var name = ""
konsole {
  text("Please enter your name: "); input()
}.runUntilInputEntered {
  onInputChanged { input = input.filter { it.isLetter() } }
  onInputEntered { name = input }
}
```

#### Keypresses

If you're interested in specific keypresses and not simply input that's been typed in, you can register a listener to
the `onKeyPressed` event:

```kotlin
konsole {
  textLine("Press Q to quit")
  /* ... */
}.run {
  var quit = false
  onKeyPressed {
    when(key) {
      Keys.Q -> quit = true
    }
  }

  while (!quit) {
    delay(16)
    /* ... */
  }
}
```

For convenience, there's also a `runUntilKeyPressed` method you can use to help with patterns like the above.

```kotlin
konsole {
  textLine("Press Q to quit")
  /* ... */
}.runUntilKeyPressed(Keys.Q) {
  while (true) {
    delay(16)
    /* ... */
  }
}
```

### Timers

A Konsole block can manage a set of timers for you. Use the `addTimer` method in your `run` block to add some:

```kotlin
konsole {
  /* ... */
}.runUntilSignal {
  addTimer(Duration.ofMillis(500)) {
    println("500ms passed!")
    signal()
  }
}
```

You can create a repeating timer by passing in `repeat = true` to the method. And if you want to stop it from repeating
at some point, set `repeat = false` inside the timer block when it is triggered:

```kotlin
val BLINK_TOTAL_LEN = Duration.ofSeconds(5)
val BLINK_LEN = Duration.ofMillis(250)
var blinkOn by konsoleVarOf(false)
konsole {
  scopedState {
    if (blinkOn) invert()
    textLine("This line will blink for $BLINK_LEN")
  }

}.run {
  var blinkCount = BLINK_TOTAL_LEN.toMillis() / BLINK_LEN.toMillis()
  addTimer(BLINK_LEN, repeat = true) {
    blinkOn = !blinkOn
    blinkCount--
    if (blinkCount == 0) {
      repeat = false
    }
  }
  /* ... */
}
```

It's possible your block will exit while things are in a bad state due to running timers, so you can use the
`onFinishing` callback to handle this:

```kotlin
var blinkOn by konsoleVarOf(false)
konsole {
  /* ... */
}.onFinishing {
  blinkOn = false
}.run {
  addTimer(Duration.ofMillis(250), repeat = true) { blinkOn = !blinkOn }
  /* ... */
}
```

***Note:** Unlike other callbacks, `onFinishing` is registered directly against the underlying `konsole` block, because
it is actually triggered AFTER the run pass is finished but before the block is torn down.*

`onFinishing` will only run after all timers are stopped, so you don't have to worry about setting a value that an
errant timer will clobber later.

### Animations

You can easily create custom animations, by calling `konsoleAnimOf`:

```kotlin
var finished = false
val spinnerAnim = konsoleAnimOf(listOf("\\", "|", "/", "-"), Duration.ofMillis(125))
val thinkingAnim = konsoleAnimOf(listOf(".", "..", "..."), Duration.ofMillis(500))
konsole {
  if (!finished) { text(spinnerAnim) } else { text("✓") }
  text(" Searching for files")
  if (!finished) { text(thinkingAnim) } else { text("... Done!") }
  textLine()
}.run {
  doExpensiveFileSearching()
  finished = true
}
```

When you reference an animation in a render for the first time, it kickstarts a timer automatically for you. In other
words, all you have to do is treat your animation instance as if it were a string, and Konsole takes care of the rest!

#### Animation templates

If you have an animation that you want to share in a bunch of places, you can create a template for it and instantiate
instances from the template. `KonsoleAnim.Template` takes exactly the same arguments as the `konsoleAnimOf` method.

This may be useful if you have a single animation that you want to run in many places at the same time but all slightly
off from one another. For example, if you were processing 10 threads at a time, you may want the spinner for each thread
to start spinning whenever its thread activates:

```kotlin
val SPINNER_TEMPATE = KotlinAnim.Template(listOf("\\", "|", "/", "-"), Duration.ofMillis(250))

val spinners = (1..10).map { konsoleAnimOf(SPINNER_TEMPLATE) }
/* ... */
```

## Advanced

### Thread Affinity

Setting aside the fact that the `run` block runs in a background thread, Konsole blocks themselves are rendered
sequentionally on a single thread. Anytime you make a call to run a Konsole block, no matter which thread it is called
from, a single thread ultimately handles it. At the same time, if you attempt to run one `konsole` block while another
block is already running, an exception is thrown.

I made this decision so that:

* I don't have to worry about multiple Konsole blocks `println`ing at the same time - who likes clobbered text?
* Konsole handles repainting by moving the terminal cursor around, which would fail horribly if multiple Konsole blocks
tried doing this at the same time.
* Konsole embraces the idea of a dynamic, active block trailed by a bunch of static history. If two dynamic blocks
wanted to be active at the same time, what would that even mean?

In practice, I expect this decision won't be an issue for most users. Command line apps are expected to have a main flow
anyway -- ask the user a question, do some work, then ask another question, etc. It is expected that a user won't ever
even need to call `konsole` from more than one thread. It is hoped that the `konsole { ... }.run { ... }`
pattern is powerful enough for most (all?) cases.

### Virtual Terminal

It's not guaranteed that every user's command line setup supports ANSI codes. For example, debugging this project with
IntelliJ as well as running within Gradle are two such environments where functionality isn't available! According to
many online reports, Windows is also a big offender here.

Konsole will attempt to detect if your console does not support the features it uses, and if not, it will open up a
virtual terminal. This fallback gives your application better cross-platform support.

To modify the logic to ALWAYS open the virtual terminal, you can construct the virtual terminal directly and pass it
into the app:

```kotlin
konsoleApp(terminal = VirtualTerminal.create()) {
  konsole { /* ... */ }
  /* ... */
}
```

or if you want to keep the same behavior where you try to run a system terminal first and fall back to a virtual
terminal later, but perhaps you want to customize the virtual terminal with different parameters, you can use:

```kotlin
konsoleApp(terminal = SystemTerminal.or {
  VirtualTerminal.create(title = "My App", terminalSize = Dimension(30, 30))
}) {
  /* ... */
}
```

### Why Not Compose / Mosaic?

Konsole's API is inspired by Compose, which astute readers may have already noticed -- it has a core block which gets
rerun for you automatically as necessary without you having to worry about it, and special state variables which, when
modified, automatically "recompose" the current console block. Why not just use Compose directly?

In fact, this is exactly what [Jake Wharton's Mosaic](https://github.com/JakeWharton/mosaic) is doing. Actually, I tried
using it first but ultimately decided against it before deciding to write Konsole, for the following reasons:

* Compose is tightly tied to the current Kotlin compiler version, which means if you are targeting a particular
version of the Kotlin language, you can easily see the dreaded error message: `This version (x.y.z) of the Compose
Compiler requires Kotlin version a.b.c but you appear to be using Kotlin version d.e.f which is not known to be
compatible.`
  * Using Kotlin v1.3 or older for some reason? You're out of luck.
  * I suspect this issue with Compose will improve over time, but for the present, it still seems like a non-Compose
  approach could be useful to many.

* Compose is great for rendering a whole, interactive UI, but console printing is often two parts: the active part that
the user is interacting with, and the history, which is static. To support this with Compose, you'd need to manage the
history list yourself and keep appending to it, and it was while thinking about an API that addressed this limitation
that I envisioned Konsole.

* Compose encourages using a set of powerful layout primitives, namely `Box`, `Column`, and `Row`, with margins and
  shapes and layers. Command line apps don't really need this level of power, however.

* Compose has a lot of strengths built around, well, composing methods! And to enable this, it makes heavy use of
  features like `remember` blocks, which you can call inside a composable method and it gets treated in a special way.
  But for a simple CLI library, being able to focus on render blocks that don't nest too deeply and not worrying as much
  about performance allowed a more pared down API to emerge.

* Compose does a lot of nice tricks due to the fact it is ultimately a compiler plugin, but it is nice to see what the
  API would kind of look like with no magic at all (although, admittedly, with some features sacrificed).

#### Mosaic comparison

```kotlin
// Mosaic
runMosaic {
  val count by remember { mutableStateOf(0) }
  Text("The count is: $count")

  LaunchedEffect(null) {
    for (i in 1..20) {
      delay(250)
      count++
    }
  }
}

// Konsole
konsoleApp {
  var count by konsoleVarOf(0)
  konsole {
    textLine("The count is: $count")
  }.run {
    for (i in 1..20) {
      delay(250)
      count++
    }
  }
}
```

Comparisons with Mosaic are included in the [examples/mosaic](examples/mosaic) folder.

### Tested Platforms

* [x] Linux
* [ ] Mac
* [ ] Windows