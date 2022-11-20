![version: 1.0.0](https://img.shields.io/badge/kotter-v1.0.0-blue)
![kotter tests](https://github.com/varabyte/kotter/actions/workflows/gradle-test.yml/badge.svg)
![kotter coverage](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/bitspittle/1fab2b6fd23489bdea3f5d1f38e4dcb2/raw/kotter-coverage-badge.json)
<a href="https://varabyte.github.io/kotter">
![kotter docs](https://img.shields.io/badge/docs-grey?logo=readthedocs)
</a>
<br>
<a href="https://discord.gg/5NZ2GKV5Cs">
  <img alt="Varabyte Discord" src="https://img.shields.io/discord/886036660767305799.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2" />
</a>
[![Mastodon Follow](https://img.shields.io/mastodon/follow/109286410344376068?domain=https%3A%2F%2Fandroiddev.social&style=social)](https://androiddev.social/@bitspittle)

# Kotter 🦦

```kotlin
session {
  var wantsToLearn by liveVarOf(false)
  section {
    text("Would you like to learn "); cyan { text("Kotter") }; textLine("? (Y/n)")
    text("> "); input(Completions("yes", "no"))

    if (wantsToLearn) {
      yellow(isBright = true) { p { textLine("""\(^o^)/""") } }
    }
  }.runUntilInputEntered {
    onInputEntered { wantsToLearn = "yes".startsWith(input.lowercase()) }
  }
}
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-input.gif)

*See also: [the game of life](examples/life), [snake](examples/snake), [sliding tiles](examples/sliding), [doom fire](examples/doomfire), and [Wordle](examples/wordle) implemented in Kotter!*

---

Kotter (a **KOT**lin **TER**minal library) aims to be a relatively thin, declarative, Kotlin-idiomatic API that provides
useful functionality for writing delightful console applications. It strives to keep things simple, providing a solution
a bit more opinionated than making raw `println` calls but way less featured than something like _Java Curses_.

Specifically, this library helps with:

* Setting colors and text decorations (e.g. underline, bold)
* Handling user input
* Creating timers and animations
* Seamlessly repainting terminal text when values change

## 🐘 Gradle

### 🎯 Dependency

```groovy
// build.gradle (groovy)

repositories {
  mavenCentral()
}

dependencies {
  implementation 'com.varabyte.kotter:kotter:1.0.0'
}
```

```kotlin
// build.gradle.kts (kotlin script)

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.varabyte.kotter:kotter:1.0.0")
}
```

### 🚥 Running examples

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

***Note:** If your terminal does not support features needed by Kotter, then this still may end up running inside a
virtual terminal.*

## 📖 Usage

### 👶 Basics

The following is equivalent to `println("Hello, World")`. In this simple case, it's definitely overkill!

```kotlin
session {
  section { textLine("Hello, World") }.run()
}
```

`section { ... }` defines a `Section` which, on its own, is inert. It needs to be run to output text to the
console. Above, we use the `run` method to trigger this. The method blocks until the render (i.e. text printing to the
console) is finished (which, in the above case, will be almost instant).

`session { ... }` sets the outer scope for your whole program. While we're just calling it with default arguments here,
you can also pass in parameters that apply to the entire application.

While the above simple case is a bit verbose for what it's doing, Kotter starts to show its strength when doing
background work (or other async tasks like waiting for user input) during which time the section block may render
several times. We'll see many examples throughout this document later.

A Kotter `session` can contain one or more `section`s. Your own app may only ever contain a single `section` and that's
fine! But if you have multiple `section`s, it will feel to the user like your app has a current, active area, following
a history of text paragraphs from previous interactions that no longer change.

### 🎨 Text Effects

You can call color methods directly, which remain in effect until the next color method is called:

```kotlin
section {
  green(layer = BG)
  red() // defaults to FG layer if no layer specified
  textLine("Red on green")
  blue()
  textLine("Blue on green")
}.run()
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/images/kotter-text-ex-1.png)

If you only want the color effect to live for a limited time, you can use scoped helper versions that handle
clearing colors for you automatically at the end of their block:

```kotlin
section {
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

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/images/kotter-text-ex-2.png)

If the user's terminal supports truecolor mode, you can specify rgb (or hsv) values directly:

```kotlin
section {
  rgb(0xFFFF00) { textLine("Yellow!") }
  hsv(35, 1.0f, 1.0f) { textLine("Orange!") }
}.run()
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/images/kotter-text-ex-3.png)

***Note:** If truecolor is not supported, terminals may attempt to emulate it by falling back to a nearby color, which
may look decent! However, to be safe, you may want to avoid subtle gradient tricks, as they may come out clumped for
some users.*

Various text effects (like bold) are also available:

```kotlin
section {
  bold {
    textLine("Title")
  }

  p {
    textLine("A paragraph is content auto-surrounded by newlines")
  }

  p {
    text("This paragraph has an ")
    underline { text("underlined") }
    textLine(" word in it")
  }
}.run()
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/images/kotter-text-ex-4.png)

***Note:** Italics functionality is not currently exposed, as it is not a standard feature and is inconsistently
supported across terminals.*

You can also define links:

```kotlin
section {
  text("Would you like to ")
  link("https://github.com/varabyte/kotter", "learn Kotter")
  textLine("?")
}
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/images/kotter-text-ex-5.png)

although keep in mind that this feature is not guaranteed to work on every terminal. In that case, it will simply render
as normal text.

### 🪆 State and scopedState

To reduce the chance of introducing unexpected bugs later, state changes (like colors) will be localized to the current
`section` block only:

```kotlin
section {
  blue(BG)
  red()
  text("This text is red on blue")
}.run()

section {
  text("This text is rendered using default colors")
}.run()
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/images/kotter-scope-ex-1.png)

Within a section, you can also use the `scopedState` method. This creates a new scope within which any state will be
automatically discarded after it ends.

```kotlin
section {
  scopedState {
    red()
    blue(BG)
    underline()
    textLine("Underlined red on blue")
  }
  text("Text without color or decorations")
}.run()
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/images/kotter-scope-ex-2.png)

***Note:** Scoped text effect methods (like `red { ... }`) work by calling `scopedState` for you under the hood.*

### 🎬 Rerendering sections

The `section` block is designed to be run one _or more_ times. That is, you can write logic inside it which may not get
executed on the first run but will be on a followup run.

Here, instead of just calling `run()`, we create a `run` block, having it update a variable that is also referenced by
the `section` block. This example will render the section twice - once when `run` is first called and again when it
calls `rerender`:

```kotlin
var result: Int? = null
section {
  text("Calculating... ")
  if (result != null) {
    text("Done! Result = $result")
  }
}.run {
  result = doNetworkFetchAndExpensiveCalculation()
  rerender()
}
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-calculating.gif)

The `run` block runs as a suspend function, so you can call other suspend methods from within it.

Your program will be blocked until the run block has finished (or, if it has triggered a rerender, until the last
rerender finishes).

#### LiveVar

In our example above, the `run` block calls a `rerender` method, which you can call to request another render pass:

```kotlin
/* ... */
run {
  result = doNetworkFetchAndExpensiveCalculation()
  rerender()
}
```

However, remembering to call `rerender` yourself is potentially fragile and could be a source of bugs in the future when
trying to figure out why your console isn't updating.

For this purpose, Kotter provides the `LiveVar` class, which, when modified, will automatically request a rerender.
An example will demonstrate this in action shortly.

To create a `LiveVar`, simply change a normal variable declaration line like:

```kotlin
session {
  var result: Int? = null
  /* ... */
}
```

to:

```kotlin
session {
  var result by liveVarOf<Int?>(null)
  /* ... */
}
```

***Note:** The `liveVarOf` method is provided by the `session` block. For many remaining examples, we'll elide the
`session` boilerplate, but that doesn't mean you can omit it in your own program!*

Let's apply `liveVarOf` to our earlier example in order to remove the `rerender` call:

```kotlin
var result by liveVarOf<Int?>(null)
section {
  /* ... no changes ... */
}.run {
  result = doNetworkFetchAndExpensiveCalculation()
}
```

And done! Fewer lines and less error pone.

Here's another example, showing how you can use `run` and a `LiveVar` to render a progress bar:

```kotlin
// Prints something like: [****------]
val BAR_LENGTH = 10
var numFilledSegments by liveVarOf(0)
section {
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

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-progress.gif)

#### LiveList

Similar to `LiveVar`, a `LiveList` is a reactive primitive which, when modified by having elements added to or
removed from it, causes a rerender to happen automatically.

You don't need to use the `by` keyword with `LiveList`. Instead, within a `session`, just assign a variable to the
result of the `liveListOf` method:

```kotlin
val fileWalker = FileWalker(".") // This class doesn't exist but just pretend for this example...
val fileMatches = liveListOf<String>()
section {
  textLine("Matches found so far:")
  if (fileMatches.isNotEmpty()) {
    for (match in fileMatches) {
      textLine(" - $match")
    }
  }
  else {
    textLine("No matches so far...")
  }
}.run {
  fileWalker.findFiles("*.txt") { file ->
    fileMatches += file.name
  }
}
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-files.gif)

The `LiveList` class is thread safe, but you can still run into trouble if you access multiple values on the list one
after the other, as a lock is released between each check. It's always possible that modifying the first property will
kick off a new render which will start before the additional values are set, in other words.

To handle this, you can use the `LiveList#withWriteLock` method:

```kotlin
val fileWalker = FileWalker(".")
val last10Matches = liveListOf<String>()
section {
  /* ... */
}.run {
  fileWalker.findFiles("*.txt") { file ->
    last10Matches.withWriteLock {
      add(file.name)
      if (size > 10) { removeAt(0) }
    }
  }
}
```

The general rule of thumb is: use `withWriteLock` if you want to modify more than one property from the list at the same
time within your `run` block.

Note that you don't have to worry about locking within a `section { ... }` block. Data access is already locked for you
in that context.

#### Other Collections

In addition to `LiveList`, Kotter also provides `LiveMap` and `LiveSet`. There's no need to extensively document these
classes here as much of the earlier `LiveList` section applies to them as well. It's just the data structure that is
different.

You can create these classes using `liveMapOf(...)` and `liveSetOf(...)`, respectfully.

#### Signals and waiting

A common pattern is for the `run` block to wait for some sort of signal before finishing, e.g. in response to some
event. You could always use a general threading trick for this, such as a `CountDownLatch` or a
`CompletableDeffered<Unit>` to stop the block from finishing until you're ready:

```kotlin
val fileDownloader = FileDownloader("...")
section {
  /* ... */
}.run {
  val finished = CompletableDeffered<Unit>()
  fileDownloader.onFinished += { finished.complete(Unit) }
  fileDownloader.start()
  finished.await()
}
```

but, for convenience, Kotter provides the `signal` and `waitForSignal` methods, which do this for you.

```kotlin
val fileDownloader = FileDownloader("...")
section {
  /* ... */
}.run {
  fileDownloader.onFinished += { signal() }
  fileDownloader.start()
  waitForSignal()
}
```

These methods are enough in most cases. Note that if you call `signal` before you reach `waitForSignal`, then
`waitForSignal` will just pass through without stopping.

There's also a convenience `runUntilSignal` method you can use, within which you don't need to call `waitForSignal`
yourself, since this case is so common:

```kotlin
val fileDownloader = FileDownloader("...")
section {
  /* ... */
}.runUntilSignal {
  fileDownloader.onFinished += { signal() }
  fileDownloader.start()
}
```

### ⌨️ User input

#### Typed input

Kotter consumes keypresses, so as the user types into the console, nothing will show up unless you intentionally print
it. You can easily do this using the `input` method, which handles listening to kepresses and adding text into your
section at that location:

```kotlin
section {
  // `input` is a method that inserts any user input typed so far in place where it is called.
  // Your section block will automatically rerender when its value changes.
  text("Please enter your name: "); input()
}.run { /* ... */ }
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-enter-name.gif)

The input method automatically adds a cursor for you. It also handles keys like LEFT/RIGHT and HOME/END, moving the
cursor back and forth between the bounds of the input string.

You can intercept input as it is typed using the `onInputChanged` event:

```kotlin
section {
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
section {
  text("Please enter your name: "); input()
}.run {
  onInputChanged {
    if (input.any { !it.isLetter() }) { rejectInput() }
    // Would also work: input = input.filter { it.isLetter() }
    // although oftren `rejectInput()` specifies your intention more clearly
  }
  /* ... */
}
```

To handle when the user presses the _ENTER_ key, use the `onInputEntered` event. You can use it in conjunction with the
`onInputChanged` event we just discussed:

```kotlin
var name = ""
section {
  text("Please enter your name: "); input()
}.runUntilSignal {
  onInputChanged { input = input.filter { it.isLetter() } }
  onInputEntered { name = input; signal() }
}
```

Above, we've indicated that we want to close the section when the user presses _ENTER_. Since this is actually a fairly
common case, Kotter provides `runUntilInputEntered` for your convenience. Using it, we can simplify the above example a
bit, typing fewer characters for identical behavior and expressing clearer intention:

```kotlin
var name = ""
section {
  text("Please enter your name: "); input()
}.runUntilInputEntered {
  onInputChanged { input = input.filter { it.isLetter() } }
  onInputEntered { name = input }
}
```

#### Input Completions

You can pass in an `InputCompleter` implementation to `input` that can generate suggestions based on the current input.
The user can press RIGHT at any time to autocomplete any suggestions shown to them.

Here's the interface (with some parts elided for simplicity):

```kotlin
interface InputCompleter {
    fun complete(input: String): String?
}

input(object : InputCompleter {
    override fun complete(input: String): String? { /* ... */ }
})
```

Perhaps you have a database of names in your program? You can use it to provide suggestions. If your implementation
returns null, that means no suggestion was found:

```kotlin
object : InputCompleter {
  override fun complete(input: String): String? {
      return names
          .firstOrNull { it.startsWith(input) }
          ?.let { it.drop(input.length) }
    // ^ Don't return the whole word; just the part that comes after the user's input so far.
  }
}
```

Kotter provides a very useful implementation out of the box, called `Completions`, which lets you specify a list of
values that will be autocompleted as long as the user's input matches one of them.

```
section {
  text("Continue? "); input(Completions("yes", "no"))
}.run()
```

Order matters! If nothing is typed, the first completion will be suggested. If multiple values match, the one earliest
in the list will be suggested.

#### Keypresses

If you're interested in specific keypresses and not simply input that's been typed in, you can register a listener to
the `onKeyPressed` event:

```kotlin
section {
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

For convenience, there's also a `runUntilKeyPressed` method you can use to help with patterns like the above. It can be
nice, for example, to let the user press _Q_ to quit your application:

```kotlin
section {
  textLine("Press Q to quit")
  /* ... */
}.runUntilKeyPressed(Keys.Q) {
  while (true) {
    delay(16)
    /* ... */
  }
}
```

### ⏳ Timers

Kotter can manage a set of timers for you. Use the `addTimer` method in your `run` block to add some:

```kotlin
section {
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
var blinkOn by liveVarOf(false)
section {
  scopedState {
    if (blinkOn) invert()
    textLine("This line will blink for ${BLINK_TOTAL_LEN.toSeconds()} seconds")
  }

}.run {
  var blinkCount = BLINK_TOTAL_LEN.toMillis() / BLINK_LEN.toMillis()
  addTimer(BLINK_LEN, repeat = true) {
    blinkOn = !blinkOn
    blinkCount--
    if (blinkCount == 0L) {
      repeat = false
    }
  }
  /* ... */
}
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-blink.gif)

With timers running, it's possible your `run` block will exit while things are in a state you didn't intend (e.g. in the
above example with the blink effect still on). You should use the `onFinishing` event to handle this case:

```kotlin
var blinkOn by liveVarOf(false)
section {
  /* ... */
}.onFinishing {
  blinkOn = false // Because user might press Q while the blinking state was on
}.runUntilKeyPressed(Keys.Q) {
  addTimer(Duration.ofMillis(250), repeat = true) { blinkOn = !blinkOn }
  /* ... */
}
```

***Note:** Unlike all the other events we discussed earlier, `onFinishing` is registered directly against the underlying
`section` and not inside the `run` block, because it is actually triggered AFTER the run pass is finished but before the
block is torn down.*

`onFinishing` will only run after all timers are stopped, so you don't have to worry about setting a value that an
errant timer will clobber later.

### 🎥 Animations

Animations make a huge difference for how the user experiences your application, so Kotter strives to make it trivial to
add them into your program.

#### Text Animation

You can easily create quick animations by calling `textAnimOf`:

```kotlin
var finished = false
val spinnerAnim = textAnimOf(listOf("\\", "|", "/", "-"), Duration.ofMillis(125))
val thinkingAnim = textAnimOf(listOf("", ".", "..", "..."), Duration.ofMillis(500))
section {
  if (!finished) { text(spinnerAnim) } else { text("✓") }
  text(" Searching for files")
  if (!finished) { text(thinkingAnim) } else { text("... Done!") }
}.run {
  doExpensiveFileSearching()
  finished = true
}
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-spinner.gif)

When you reference an animation in a render for the first time, it kickstarts a timer automatically for you. In other
words, all you have to do is treat your animation instance as if it were a string, and Kotter takes care of the rest!

#### Text animation templates

If you have an animation that you want to share in a bunch of places, you can create a template for it and instantiate
instances from the template. `TextAnim.Template` takes exactly the same arguments as the `textAnimOf` method:

```kotlin
val SPINNER_TEMPATE = TextAnim.Template(listOf("\\", "|", "/", "-"), Duration.ofMillis(250))

val spinners = (1..10).map { textAnimOf(SPINNER_TEMPLATE) }
/* ... */
```

#### Render animations

If you need a bit more power than text animations, you can use a render animation instead. You create one with a
callback that is given a frame index and access to the current render scope. You can interpret the frame index however
you want and use the render scope to call any of Kotter's text rendering methods that you need.

Declare a render animation using the `renderAnimOf` method and then invoke the result inside your render block:

```kotlin
val exampleAnim = renderAnimOf(numFrames = 5, Duration.ofMillis(250)) { i -> /* ... */ }
section {
  // Call your render animation passing in the section block (i.e. `this`) as a parameter
  exampleAnim(this)
  /* ... */
}
```

For example, let's say we want to rotate through a list of colors and apply those to some text. Text animations only
deal with raw text and don't have access to text effects like colors and styles, so we can't use them here, but we can
accomplish these easily using a render animation and the `color(Color)` method:

```kotlin
// Note: `Color` is a Kotter enum that enumerates all the standard colors it supports

val colorAnim = renderAnimOf(Color.values().size, Duration.ofMillis(250)) { i ->
  color(Color.values()[i])
}
section {
  colorAnim(this) // Side-effect: sets the color for this section
  text("RAINBOW")
}.runUntilSignal { /* ... */ }
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-rainbow.gif)

#### One-shot animations

Both text and render animations can be created with a `looping` parameter set to false, if you only want them to run
once and stop:

```kotlin
val arrow = "=============>"

val wipeRightAnim = renderAnimOf(
  arrow.length + 1, // `length + 1` because empty string is also a frame
  Duration.ofMillis(40),
  looping = false
) { frameIndex ->
  textLine(arrow.take(frameIndex))
}

section {
  text("Go this way: "); wipeRightAnim(this)
}.runUntilSignal {
  // Give the animation time to complete:
  addTimer(wipeRightAnim.totalDuration) { signal() }
}
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-one-shot.gif)

You can restart a one-shot animation by setting its `currFrame` property back to 0.

### 📥 Offscreen

Occasionally, when you want to render some marked up text, you'll wish you could measure it first, for example allowing
you to pad both sides of each line with spaces to center everything, or putting the right count of "=" characters above
and below a block of text to give it a sort of header effect. But by the time you've rendered something out, then it's
too late!

`offscreen` to the rescue. You can think of `offscreen` as a temporary buffer to render to, after which you can both
query it and control when it actually renders to the screen.

`offscreen` returns a buffer, which is a read-only view of the content. You can query its raw text or line lengths,
for example. To render it, you need to call `offscreen.createRenderer` and then use `renderer.renderNextRow` to render
out each line at a time.

Here, we use `offscreen` to render the header effect described above:

```kotlin
section {
  val buffer = offscreen {
    textLine("Multi-line"); textLine("Header"); textLine("Example")
  }
  val headerLen = buffer.lineLengths.maxOrNull() ?: 0
  val renderer = buffer.createRenderer()
  repeat(headerLen) { text('=') }; textLine()
  while (renderer.hasNextRow()) {
    renderer.renderNextRow()
    textLine()
  }
  repeat(headerLen) { text('=') }; textLine()
}.run()
```

![Offscreen header example](https://github.com/varabyte/media/raw/main/kotter/images/offscreen-header-example.png)

***Note:** Although you usually won't need to, you can create multiple renderers, each which manages its own state for
what row to render out next.*

One nice thing about the offscreen buffer is it manages its own local state, and while it originally inherits its parent
scope's state, any changes you make within the offscreen buffer will be remembered to its end.

This is easier seen than described. The following example:

```kotlin
section {
  val buffer = offscreen {
    textLine("Inherited color (red)")
    cyan()
    textLine("Local color (cyan)")
    textLine("Still blue")
  }

  val renderer = buffer.createRenderer()
  red()
  while (renderer.hasNextRow()) {
    text("red -- "); renderer.renderNextRow(); textLine(" -- red")
  }
}.run()
```

will render:

![Offscreen local state example](https://github.com/varabyte/media/raw/main/kotter/images/offscreen-local-state.png)

The driving motivation for adding offscreen buffers was to be able to easily add borders around any block of text, where
the border might be a different color than its contents. So when this functionality went in, we also added the
`bordered` method ([link to example](https://github.com/varabyte/kotter/tree/main/examples/border)).

If you want to implement your own utility method that uses `offscreen` under the hood, you can check
[bordered's implementation](https://github.com/varabyte/kotter/blob/main/kotter/src/main/kotlin/com/varabyte/kotterx/decorations/BorderSupport.kt)
yourself to see how it delegates to `offscreen`, padding each row with the right number of spaces so that the border sides all line up.

### 📤 Aside

You can actually make one-off render requests directly inside a `run` block:

```kotlin
section {
    /* ... */
}.run {
    aside {
        textLine("Hello from an aside block")
    }
}
```

which will output text directly before the active section.

In order to understand aside blocks, you should start to think of Kotter output as two parts -- some static history, and
a dynamic, active area at the bottom. The static history will never change, while the active area will be written and
cleared and rewritten over and over and over again as needed.

In general, a section is active *until* it is finished running, at which point it becomes static history, and the next
section becomes active. You can almost think about *consuming* an active section, which freezes it after one final
render, at which point it becomes static.

In fact, it's a common pattern to get static instructions out of the way first, in its own section, so we don't waste
time rerendering them over and over in the main block:

```kotlin
session {
  // The following instructions are static, just render them immediately
  section {
    textLine("Press arrow keys to move")
    textLine("Press R to restart")
    textLine("Press Q to quit")
    textLine()
  }.run()

  section {
    /* ... constantly rerendered lines ... */
  }.runUntilKeyPressed(Keys.Q) { /* ... */ }
}
```

Occasionally, however, you want to generate static history *while* a block is still active.

Let's revisit an example from above, our `FileWalker` demo which searched a list of files and added every matching
result to a list. We can, instead, put a spinner in the active section and use the `aside` block to output matches:

```kotlin
val fileWalker = FileWalker(".")
var isFinished by liveVarOf(false)
val searchingAnim = textAnimOf(listOf("", ".", "..", "..."), Duration.ofMillis(500))
section {
  textLine()
  if (!isFinished) {
      textLine("Searching$searchingAnim")
  }
  else {
      textLine("Finished searching")
  }
}.run {
  aside {
    textLine("Matches found so far:")
    textLine()
  }

  fileWalker.findFiles("*.txt") { file ->
    aside { textLine(" - ${file.name}") }
  }
  isFinished = true
}
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-aside.gif)

## 🎓 Advanced

### 🔨 "Extending" Kotter

Kotter aims to provide all the primitives you need to write dynamic, interactive console applications, such as
`textLine`, `input`, `offscreen`, `aside`, `onKeyPressed`, etc.

But we may have missed _your_ use case, or maybe you just want to refactor out some logic to share across `section`s.
This is totally doable, but it requires writing extension methods against the correct receiving classes. At this point,
we need to discuss the framework in a bit more detail than beginners need to know.

For reference, you should also look at the [extend](examples/extend) sample project, which was written to demonstrate
some of the concepts that will be discussed here.

#### Scopes

Before continuing, let's look at the overview of a Kotter application. The following may look a bit complex at first
glance, but don't worry as the remaining subsections will break it down:

```
┌───────── session {
│ ┌─┬─────   section {
│ │ │           ...
│ │ 3a┌───      offscreen {
│ │ │ 3b           ...
│ │ │ └───      }
│ │ └─────   }.onFinished {
1 2             ...
│ │ ┌─────   }.run {
│ │ │           ...
│ │ 4 ┌───      aside {
│ │ │ 3c           ...
│ │ │ └───      }
│ └─┴─────   }
└───────── }
```

**1 - `Session`**

```
┌─ session {
│    section {
│      ...
│    }.run {
│      ...
│    }
└─ }
```

The top level of your whole application. `Session` owns a `data: ConcurrentScopedData` field which we'll talk more about
a little later. However, it's worth understanding that `data` lives inside a session, although many scopes additionally
expose it themselves. Any time you see some Kotter code interacting with a `data` field, it is really pointing back to
this singular one.

`Session` is the scope you need when you want to call `liveVarOf` or `liveListOf`, or even to declare a `section`:

```kotlin
fun Session.firstSection() {
  var name by liveVarOf("")
  var age by liveVarOf(18)
  section { /* ... */ }.run { /* ... */ }
}

fun Session.secondSection() { /* ... */ }
fun Session.thirdSection() { /* ... */ }

// Later ...

session {
  firstSection()
  secondSection()
  thirdSection()
}
```

**2 - `Section`**

```
┌─ section {
│    ...
│  }.run {
│    ...
└─ }
```

Unlike `Session`, you shouldn't ever need to add an extension method on top of a `Section`. This is because its
`section { ... }` block and its `run { ... }` block both receive different scope classes as their receiver classes.

It is those scope classes you usually will want to extend. These are discussed next.

**3 - `RenderScope`**

```
3a
┌─ section {
│    ...
└─ }

3b
┌─ offscreen {
│    ...
└─ }

3c
┌─ aside {
│    ...
└─ }
```

This scope represents a render pass. This is the scope that owns `textLine`, `red`, `green`, `bold`, `underline`, and
other text rendering methods.

This can be a useful scope for extracting out a common text rendering pattern. For example, let's say you wanted to
display a bunch of terminal commands and their arguments, and you want to highlight the command a particular color,
e.g. cyan:

```kotlin
section {
  cyan { text("cd") }; textLine(" /path/to/code")
  cyan { text("git") }; textLine(" init")
}
```

That can get a bit repetitive. Plus, if you decide to change the color later, or maybe bold the command part, you'd have
to do it all over the place. Also, putting the logic behind a function can help you express your intention more clearly.

Let's refactor it!

```kotlin
fun RenderScope.shellCommand(command: String, arg: String) {
  cyan { text(command) }; textLine(" $arg")
}

section {
  shellCommand("cd", "/path/to/code")
  shellCommand("git", "init")
}
```

Much better!

On its surface, the concept of a `RenderScope` seems pretty straightforward, but the gotcha is that Kotter offers a few
separate areas that accept render blocks. In addition to the main rendering block, there are also `offscreen` and
`aside` blocks, which allow rendering to different targets (these are discussed earlier in the README in case you aren't
familiar with them here).

Occasionally, you may want to define an extension method that *only* applies to one of the three blocks (usually the
main one). In order to narrow down the place your helper method will appear in, you can use `MainRenderScope` (3a),
`OffscreenRenderScope` (3b), and/or `AsideRenderScope` (3c) as your method receiver instead of just `RenderScope`.

For example, the `input()` method doesn't make sense in an `offscreen` or `aside` context (as those aren't interactive).
Therefore, its definition looks like `fun MainRenderScope.input(...) { ... }`

**4 - `RunScope`**

```
   section {
     ...
┌─ }.run {
│    ...
└─ }
```

`RunScope` is used for `run` blocks. It's useful for extracting logic that deals with handling user input or
long-running tasks. Functions like `onKeyPressed`, `onInputChanged`, `addTimer` etc. are defined on top of this scope.

```kotlin
fun RunScope.exec(vararg command: String) {
  val process = Runtime.getRuntime().exec(*command)
  process.waitFor()
}

section {
    textLine("Please wait, cloning the repo...")
}.run {
  exec("git", "clone", "https://github.com/varabyte/kotter.git")
  /* ... */
}
```

**`SectionScope`**

To close off all this scope discussion, it's worth mentioning that a `SectionScope` interface exists. It is the base
interface to both `RenderScope` AND a `RunScope`, and using it can allow you to define the occasional helper method that
can be called from both of them.

#### ConcurrentScopedData

The one thing that all scopes have in common is they expose access to a session's `data` field. OK, but what is it?

`ConcurrentScopedData` is a thread-safe hashmap, where the keys are always of type `ConcurrentScopedData.Key<T>`, and
such keys are associated with a `ConcurrentScopedData.Lifecycle` (meaning that any data you register into the map will
always be released when some parent lifecycle ends, unless you remove it yourself manually first).

Kotter itself manages four lifecycles: `Session.Lifecycle`, `Section.Lifecycle`, `MainRenderScope.Lifecycle`, and
`Run.Lifecycle` (each associated with the scopes discussed above).

***Note:** No lifecycles are provided for `offscreen` or `aside` blocks at the moment. Feel free to open up an issue
with a use-case requiring additional lifecycles if you run into one.*

Keep in mind that the `MainRenderScope.Lifecycle` dies after a *single* render pass. Almost always you want to tie data
to `Section.Lifecycle`, as it survives across multiple runs.

Nothing prevents you from defining your own lifecycle - just be sure to call `data.start(...)` and `data.stop(...)` with
it.

```kotlin
  object MyLifecycle : ConcurrentScopedData.Lifecycle
  private val MySetting = MyLifecycle.createKey<Boolean>()

  try {
    data.start(MyLifecycle)
    data[MySetting] = true
  } finally {
    data.stop(MyLifecycle)
  }
```

Lifecycles can be defined as subordinate to other lifecycles, so if you create a lifecycle that is tied to the `Run`
lifecycle for example, then you don't need to explicitly call `stop` yourself (but you still need to call `start`).

```kotlin
  object MyLifecycle : ConcurrentScopedData.Lifecycle {
    override val parent = Run.Lifecycle
  }

  section { /* ... */ }.run {
    data.start(MyLifecycle) // Will be stopped when the run block finishes
  }
```


You can review the `ConcurrentScopedData` class for its full list of documented API calls, but the three common ways to
add values to it are:

* always overwrite: `data[key] = value`
* add if first time: `data.tryPut(key, value) // returns true if added, false otherwise`
* add if first time but always run some follow-up logic:<br>
  `data.putIfAbsent(key, provideInitialValue = { value }) { ... logic using value ... }`<br>
  * This is essentially a shortcut for calling `tryPut` and then getting the value, but doing so in a way that ensures
    no one else grabs the thread from you in between.

By having a session own and expose such a data structure, it makes it possible for anyone to write their own extension
methods on top of Kotter, using data as a way to manage long-lived state. For example, `input()`, which may get called
many times in a row as the section rerenders, can distinguish the first time it is called from later calls based on
whether some value is present in the data cache or not.

To close this section, we just wanted to say that it was very tempting at first to create a bunch of hardcoded functions
baked inside `Section`, `MainRenderScope`, etc., with access to some private state, but implementing everything through
`ConcurrentScopedData` plus extension methods ensured that we were using the same tools as users.

So go forth, and extend Kotter!

### 🧵 Thread Affinity

Sections are rendered sequentially on a single render thread. Anytime you see a `section { ... }`, no matter which
thread it is called from, a single thread ultimately handles it. However, if you use two threads to attempt to run one
section while another is already running, an exception is thrown.

The `run` block runs in place on the thread that called it. In this way, progress is prevented until the run logic
finishes running.

I made the decision to lock section rendering down so that:

* I don't have to worry about multiple sections `println`ing at the same time - who likes clobbered text?
* Kotter handles repainting by moving the terminal cursor around, which would fail horribly if multiple sections tried
doing this at the same time.
* Kotter embraces the idea of a dynamic, active section preceded by a bunch of static history. If two dynamic blocks
wanted to be active at the same time, what would that even mean?

In practice, I expect this decision won't be an issue for most users. Command line apps are expected to have a main flow
anyway -- ask the user a question, do some work, then ask another question, etc. It is expected that a user won't ever
even need to call `section` from more than one thread. It is hoped that the

```kotlin
session {
  section { /* ... render thread ... */ }.run { /* ... current thread ... */ }
  section { /* ... render thread ... */ }.run { /* ... current thread ... */ }
  section { /* ... render thread ... */ }.run { /* ... current thread ... */ }
}
```

pattern (just calling `section`s one after another on a single thread) is powerful enough for most (all?) cases.

### 🖥️  Virtual Terminal

It's not guaranteed that every user's command line setup supports ANSI. For example, debugging this project with
IntelliJ as well as running within Gradle are two such environments where functionality isn't available! According to
many online reports, some legacy terminals on Windows are also an offender here.

Kotter will attempt to detect if your console does not support the features it uses, and if not, it will open up a
virtual terminal instead. This fallback gives your application better cross-platform support.

To modify the logic to ALWAYS open the virtual terminal, you can set the `terminal` parameter in `session` like
this:

```kotlin
session(terminal = VirtualTerminal.create()) {
  section { /* ... */ }
  /* ... */
}
```

or you can chain multiple factory methods together using the `firstSuccess` method, which will try to start each
terminal type in turn:

```kotlin
session(
  terminal = listOf(
    { SystemTerminal() },
    { VirtualTerminal.create(title = "My App", terminalSize = Dimension(30, 30)) },
  ).firstSuccess()
) {
  /* ... */
}
```

### 🤷 Why Not Compose / Mosaic?

Kotter's API is inspired by Compose, which astute readers may have already noticed -- it has a core block which gets
rerun for you automatically as necessary without you having to worry about it, and special state variables which, when
modified, automatically "recompose" the current console block. Why not just use Compose directly?

In fact, this is exactly what [Jake Wharton's Mosaic](https://github.com/JakeWharton/mosaic) is doing. Actually, I tried
using it first but ultimately decided against it before deciding to write Kotter, for the following reasons:

* Compose is tightly tied to the current Kotlin compiler version, which means if you are targeting a particular
version of the Kotlin language, you can easily see the dreaded error message: `This version (x.y.z) of the Compose
Compiler requires Kotlin version a.b.c but you appear to be using Kotlin version d.e.f which is not known to be
compatible.`
  * Using Kotlin v1.3 or older for some reason? You're out of luck.
  * Want to upgrade Kotlin without updating Kotter? You're out of luck.
  * Want to update Kotter without also upgrading Kotlin? You might be out of luck.

* Compose is great for rendering a whole, interactive UI, but console printing is often two parts: the active part that
the user is interacting with, and the history, which is static. To support this with Compose, you'd need to manage the
history list yourself and keep appending to it, which would be a waste of render cycles and memory when you could just
lean on the console to do it. It was while thinking about an API that addressed this limitation that I envisioned
Kotter.
  * For a concrete example, see the [compiler demo](examples/compiler). A compiler might generate hundreds (or
    thousands!) of history lines. We definitely don't want to rerender all of those every frame.

* Compose encourages using a set of powerful layout primitives, namely `Box`, `Column`, and `Row`, with margins and
  shapes and layers. Command line apps don't really need this level of power, however.

* Compose has a lot of strengths built around, well, composing methods! But for a simple CLI library, being able to
  focus on simple render blocks that don't nest at all allowed a more pared down API to emerge.

* Compose does a lot of nice tricks due to the fact it is ultimately a compiler plugin, but it is interesting to see
  what the API could look like with no magic at all (although, admittedly, with some features sacrificed).

* Mosaic doesn't support input well yet (at the time of writing this README, but maybe this has changed in the future).
  For example, compare [Mosaic](https://github.com/JakeWharton/mosaic/blob/fd213711ce2b828a6436a61d6d345692222bdb95/samples/robot/src/main/kotlin/example/robot.kt#L45)
  to [Kotter](https://github.com/varabyte/kotter/blob/main/examples/mosaic/robot/src/main/kotlin/main.kt#L27).

#### Mosaic comparison

Mosaic and Kotter programs look very similar, but they are organized slightly differently. Instead of Compose, where you
have a single code block where values, layout, and logic are combined (with judicious use of `remember` and
`LaunchedEffect`), in Kotter you tend to have three separate areas for these concepts: before a section, a section
block, and a run block.

```kotlin
// Mosaic
runMosaic {
  var count by remember { mutableStateOf(0) }
  Text("The count is: $count")

  LaunchedEffect(null) {
    for (i in 1..20) {
      delay(250)
      count++
    }
  }
}

// Kotter
session {
  var count by liveVarOf(0)
  section {
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