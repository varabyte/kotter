![version: 0.9.4](https://img.shields.io/badge/kotter-v0.9.4-blue)
<a href="https://discord.gg/5NZ2GKV5Cs">
  <img alt="Varabyte Discord" src="https://img.shields.io/discord/886036660767305799.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2" />
</a>
[![Follow @bitspittle](https://img.shields.io/twitter/follow/bitspittle.svg?style=social)](https://twitter.com/intent/follow?screen_name=bitspittle)

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

*See also: [the game of life](examples/life), [snake](examples/snake), [sliding tiles](examples/sliding), and [doom fire](examples/doomfire) implemented in Kotter!*

---

Kotter (a **KOT**lin **TER**minal library) aims to be a relatively thin, declarative, Kotlin-idiomatic API that provides
useful functionality for writing delightful console applications. It strives to keep things simple, providing a solution
a bit more opinionated than making raw `println` calls but way less featured than something like _Java Curses_.

Specifically, this library helps with:

* Setting colors and text decorations (e.g. underline, bold)
* Handling user input
* Creating timers and animations
* Seamlessly repainting terminal text when values change

## Gradle

### Dependency

The artifact for this project is hosted in our own artifact repository (*), so to include Kotter in your project, modify
your Gradle build file as follows:

```groovy
repositories {
  /* ... */
  maven { url 'https://us-central1-maven.pkg.dev/varabyte-repos/public' }
}

dependencies {
  /* ... */
  implementation 'com.varabyte.kotter:kotter:0.9.4'
}
```

(* To be hosted in `mavenCentral` eventually)

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

***Note:** If your terminal does not support features needed by Kotter, then this still may end up running inside a
virtual terminal.*

## Usage

### Basics

The following is equivalent to `println("Hello, World")`. In this simple case, it's definitely overkill!

```kotlin
session {
  section { textLine("Hello, World") }.run()
}
```

`section { ... }` defines a `Section` which, on its own, is inert. It needs to be run to output text to the
console. Above, we use the `run` method to trigger this. The method blocks until the render (i.e. text printing to the
console) is finished (which, for console text, probably won't be very long).

`session { ... }` sets the outer scope for your whole program (e.g. it specifies the lifetime of some data). While we're
just calling it with default arguments here, you can also pass in parameters that apply to the entire application.
A Kotter `session` can contain one or more `section`s.

While the above simple case is a bit verbose for what it's doing, Kotter starts to show its strength when doing
background work (or other async tasks like waiting for user input) during which time the block may update several times.
We'll see many examples throughout this document later.

### Text Effects

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

or, if you only want the color effect to live for a limited time, you can use scoped helper versions that handle
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

If the user's terminal supports truecolor mode, you can specify rgb (or hsv) values directly:

```kotlin
section {
  rgb(0xFFFF00) { textLine("Yellow!") }
  hsv(35, 1.0, 1.0) { textLine("Orange!") }
}.run()
```

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

***Note:** Italics functionality is not currently exposed, as it is not a standard feature and is inconsistently
supported across terminals.*

### State and scopedState

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

Within a section, you can also use the `scopedState` method. This creates a new scope within which any state will be
automatically discarded after it ends.

```kotlin
section {
  scopedState {
    red()
    blue(BG)
    underline()
    text("Underlined red on blue")
  }
  text("Text without color or decorations")
}.run()
```

***Note:** This is what the scoped text effect methods (like `red { ... }`) are doing for you under the hood, actually.*

### Dynamic sections

The `section` block is designed to be run one _or more_ times. That is, you can write logic inside it which may not get
executed on the first run but will be on a followup run.

Here, we pass in a callback to the `run` method which updates a value referenced by the `section` block (the `result`
integer). This example will run the section twice - once when `run` is first called and again when it calls
`rerender`:

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

The `run` callback automatically runs on a background thread for you (as a suspend function, so you can call other
suspend methods from within it).

Unlike using `run` without a callback (i.e. simply `run()`), here your program will be blocked until the callback has
finished (or, if it has triggered a rerender, until the last rerender finishes after your callback is done).

#### LiveVar

As you can see above, the `run` callback uses a `rerender` method, which you can call to request another render pass.

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

***Note:** The `liveVarOf` method is actually scoped to the `session` block. For many remaining examples, we'll elide
the `session` boilerplate, but that doesn't mean you can omit it in your own program!*

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

Here's another example, showing how you can use `run` for something like a progress bar:

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

#### LiveList

Similar to `LiveVar`, a `LiveList` is a reactive primitive which, when modified by having elements added to or
removed from it, causes a rerender to happen automatically. You don't need to use the `by` keyword with `LiveList`.
Instead, within a `session`, use the `liveListOf` method:

```kotlin
val fileWalker = FileWalker(".") // This class doesn't exist but just pretend for this example...
val fileMatches = liveListOf<String>()
section {
  textLine("Matches found so far: ")
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
  /* ... */
}
```

The `LiveList` class is thread safe, but you can still run into trouble if you access multiple values on the list one
after the other, as a lock is released between each check. It's always possible that modifying the first property will
kick off a new render which will start before the additional values are set, in other words.

To handle this, you can use the `LiveList#withWriteLock` method:

```kotlin
val fileWalker = FileWalker(".")
val last10Matches = liveListOf<String>()
section {
  ...
}.run {
  fileWalker.findFiles("*.txt") { file ->
    last10Matches.withWriteLock {
      add(file.name)
      if (size > 10) { removeAt(0) }
    }
  }
  /* ... */

}
```

The general rule of thumb is: use `withWriteLock` if you want to access or modify more than one property from the list
at the same time within your `run` block.

Note that you don't have to worry about locking within a `section { ... }` block. Data access is already locked for you
in that context.

#### Signals and waiting

A common pattern is for the `run` block to wait for some sort of signal before finishing, e.g. in response to some
event. You could always use a general threading trick for this, such as a `CountDownLatch` or a
`CompletableDeffered<Unit>` to stop the block from finishing until you're ready:

```kotlin
val fileDownloader = FileDownloader("...")
fileDownloader.start()
section {
  /* ... */
}.run {
  val finished = CompletableDeffered<Unit>()
  fileDownloader.onFinished += { finished.complete(Unit) }
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
  waitForSignal()
}
```

These methods are enough in most cases. Note that if you call `signal` before you reach `waitForSignal`, then
`waitForSignal` will just pass through without stopping.

There's also a convenience `runUntilSignal` method you can use, within which you don't need to call `waitForSignal` yourself, since
this case is so common:

```kotlin
val fileDownloader = FileDownloader("...")
section {
  /* ... */
}.runUntilSignal {
  fileDownloader.onFinished += { signal() }
}
```

### User input

#### Typed input

Kotter consumes keypresses, so as the user types into the console, nothing will show up unless you intentionally print
it. You can easily do this using the `input` method, which handles listening to kepresses and adding text into your
section at that location:

```kotlin
section {
  // `input` is a method that appends the user's input typed so far in this
  // Once your section references it, the block is automatically rerendered when its value changes.
  text("Please enter your name: "); input()
}.run { /* ... */ }
```

Note that the input method automatically adds a cursor for you. This also handles keys like LEFT/RIGHT and HOME/END,
moving the cursor back and forth between the bounds of the input string.

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
  }
  /* ... */
}
```

You can also use `onInputEntered`. This will be triggered whenever the user presses the ENTER key.

```kotlin
var name = ""
section {
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
section {
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

For convenience, there's also a `runUntilKeyPressed` method you can use to help with patterns like the above.

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

### Timers

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

It's possible your block will exit while things are in a bad state due to running timers, so you can use the
`onFinishing` callback to handle this:

```kotlin
var blinkOn by liveVarOf(false)
section {
  /* ... */
}.onFinishing {
  blinkOn = false
}.run {
  addTimer(Duration.ofMillis(250), repeat = true) { blinkOn = !blinkOn }
  /* ... */
}
```

***Note:** Unlike other callbacks, `onFinishing` is registered directly against the underlying `section`, because it is
actually triggered AFTER the run pass is finished but before the block is torn down.*

`onFinishing` will only run after all timers are stopped, so you don't have to worry about setting a value that an
errant timer will clobber later.

### Animations

You can easily create custom animations, by calling `animOf`:

```kotlin
var finished = false
val spinnerAnim = animOf(listOf("\\", "|", "/", "-"), Duration.ofMillis(125))
val thinkingAnim = animOf(listOf("", ".", "..", "..."), Duration.ofMillis(500))
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

#### Animation templates

If you have an animation that you want to share in a bunch of places, you can create a template for it and instantiate
instances from the template. `Anim.Template` takes exactly the same arguments as the `animOf` method.

This may be useful if you have a single animation that you want to run in many places at the same time but all slightly
off from one another. For example, if you were processing 10 threads at a time, you may want the spinner for each thread
to start spinning whenever its thread activates:

```kotlin
val SPINNER_TEMPATE = Anim.Template(listOf("\\", "|", "/", "-"), Duration.ofMillis(250))

val spinners = (1..10).map { animOf(SPINNER_TEMPLATE) }
/* ... */
```

### Offscreen

Occasionally, when you want to render some marked up text, you'll wish you could measure it first, for example allowing
you to pad both sides of each line with spaces to center everything, or putting the right count of "=" characters above
and below a block of text to give it a sort of header effect. But by the time you've rendered something out, then it's
too late!

`offscreen` to the rescue. You can think of `offscreen` as a temporary buffer to render to, after which you can both
query it and control when it actually renders to the screen.

`offscreen` returns a buffer, which is a read-only view of the content. You can query its raw text or line lengths,
for example. To render it, you need to call `offscreen.createRenderer` and then use `renderer.renderNextRow` to render
out each line at a time.

```kotlin
section {
  // NOTE: This is just example code. As is, it isn't useful in practice, as it just immediately renders the offscreen
  // buffer onscreen, but it showcases all the moving parts.
  val buffer = offscreen { ... }
  val renderer = buffer.createRenderer()
  while (renderer.hasNextRow()) { renderer.renderNextRow() }
}
```

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
}
```

will render:

![Offscreen local state example](https://github.com/varabyte/media/raw/main/kotter/images/offscreen-local-state.png)

The driving motivation for adding offscreen buffers was to be able to easily add borders around any block of text, so
when this functionality went in, we also added the `bordered` method ([link to code](https://github.com/varabyte/kotter/blob/main/kotter/src/main/kotlin/com/varabyte/kotterx/decorations/BorderSupport.kt)).
You can check the implementation yourself to see how it delegates to `offscreen`, padding each row with the right
number of spaces so that the border sides all line up.

### Aside

In order to understand aside blocks, you should start to think of Kotter output as two parts -- some static history, and
a dynamic, active area at the bottom. The static history will never change, while the active area will be written and
cleared and rewritten over and over and over again as needed.

In general, a section is active *until* it is finished running, at which point it becomes static history, and the next
section becomes active. You can almost think about *consuming* an active section, which freezes it after one final
render, at which point it becomes static.

For example, it's a common pattern to get static instructions out of the way first, in its own section, so we don't
waste time rerendering them over and over in the main block:

```kotlin
session {
  section {
    textLine("Press arrow keys to move")
    textLine("Press R to restart")
    textLine("Press Q to quit")
    textLine();
  }.run()

  section {
    ... constantly rerendered lines ...
  }.runUntilKeyPressed(Keys.Q) { ... }
}
```

Occasionally, however, you want to generate static history *while* a block is still active. For example, imagine an old
text adventure game, with a blinking input cursor waiting for you to type in some command, with a growing backlog of
history text streaming off the top of the screen as you play the game.

To accomplish this, you could put a `section` within a while loop, and then when the user enters a command, immediately
change the section to render the output of that command before finishing it and then starting up the next `section`.

Or, you could run `aside` inside a `run` block each time a command is entered.

Assume you have a `state: GameState` model which contains the logic for parsing these commands and updating the current
text adventure room. Your program could look something like this:

```kotlin
session {
  section {
    textLine("Type commands to navigate the world. Type \"exit\" to quit.")
    textLine()
    textLine(state.currentText)
    textLine()
  }.run()

  var commandCount = 0 // Used as an id in input() so it clears itself each time it gets incremented
  section {
    text("Command > "); input(id = commandCount)
  }.runUntilSignal {
    onInputEntered {
      when (input) {
        "exit" -> signal()
        else -> {
          ++commandCount
          state.handleCommand(input)
          aside {
            black(isBright = true) { textLine("You typed: $input") }
            textLine()
            textLine(state.currentText)
            textLine()
          }
        }
      }
    }
  }
}
```

producing output that might look something like:

```
Type commands to navigate the world. Type "exit" to quit.

You find yourself arriving, tired but excited, at the gates of Kotterton. You
thought you were at the end of a long and meandering adventure, but it turns
out you are just at the beginning.

You typed: look

Ahead of you, you see two large, wooden gates, locked and barring entry into
the city. However, there is a small open window inset into one of the stone
walls next to the gates, with a guard's face looking out of it. There is a
stick on the grassy floor.

You typed: pick up stick

You grab the stick and put it into your backpack. The guard almost gives you
a quizzical look but then decides to yawn instead and continues to stare ahead.

You typed: look

Ahead of you, you see two large, wooden gates, locked and barring entry into
the city. However, there is a small open window inset into one of the stone
walls next to the gates, with a guard's face looking out of it.

Command > talk to guard█
```

In such a program, you can think of it as having an active block only one
line tall (very efficient!) with an ever-growing history of static text,
appended to by the `aside` block.

## Advanced

### "Extending" Kotter

Kotter aims to provide all the primitives you need to write dynamic, interactive console applications, such as
`textLine`, `input`, `offscreen`, `aside`, `onKeyPressed`, etc.

But we may have missed _your_ use case, or maybe you just want to refactor out some logic to share across `section`s.
This is totally doable, but it requires discussing the framework in a bit more detail. We'll tackle that in this part.

For reference, you should also look at the [extend](examples/extend) sample project, which was written to demonstrate
the concepts that will be discussed here.

#### Scopes

Before continuing, it will be important to familiarize with the breakdown of a Kotter application. The following may
look a bit complex at first glance but don't worry as the remaining subsections will break it down:

```
┌───────── | session {
│ ┌─┬───── |   section {
│ │ │      |      ...
│ │ 3a┌─── |      offscreen {
│ │ │ 3b   |         ...
│ │ │ └─── |      }
│ │ └───── |   }.onFinished {
1 2        |      ...
│ │ ┌───── |   }.run {
│ │ │      |      ...
│ │ 4 ┌─── |      aside {
│ │ │ 3c   |         ...
│ │ │ └─── |      }
│ └─┴───── |   }
└───────── | }
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
a little later. However, it's worth understanding that `data` lives inside a session, and every other way you access it
is really pointing back to this singular one.

`Session` is the scope you need when calling `liveVarOf` or `liveListOf`, or even declaring a `section`, so you may find
yourself creating an extension method on top of the `Session` scope to reference such methods:

```kotlin
fun Session.firstSection() {}
  var name by liveVarOf("")
  var age by liveVarOf(18)
  section { ... }.run { ... }
}

fun Session.secondSection() { ... }
fun Session.thirdSection() { ... }

... later ...

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

Unlike `Session`, you shouldn't ever need to add an extension method on top of a `Section`, because a section is mainly
just a class for managing two sub-parts - the render logic (which runs on the main thread) and the run logic (which runs
on a background thread).

It is the render and run parts that are particularly interesting and most likely that users will want to extend.

**3 (a, b, c) - `RenderScope`**

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


This scope represents a single render pass. This is the scope that owns `textLine`, `red`, `green`, `bold`, `underline`,
and other text rendering methods.

This can be a useful scope for extracting out a common text rendering pattern. For example, let's say you wanted to
display a bunch of terminal commands and their arguments, and you want to highlight the command a particular color,
e.g. cyan.

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
fun RenderScope.command(command: String, arg: String) {
  cyan { text(command) }; textLine(" $arg")
}

section {
  command("cd", "/path/to/code")
  command("git", "init")
}
```

Much better!

On its surface, the concept of a `RenderScope` seems pretty straightforward, but the gotcha is that Kotter offers a few
separate areas that accept render blocks. In addition to the main rendering block, there are also `offscreen` and
`aside` blocks, which allow rendering to different targets (these are discussed earlier in the README in case you aren't
familiar with them here).

Occasionally, you may want to define an extension method that *only* applies to one of the three blocks (usually the
main one). In order to narrow down the place your helper method will appear in, you can use `MainRenderScope` (3a),
`OffscreenRenderScope` (3b), and/or `AsideRenderScope` (3c) as your method receiver.

For example, the `input()` method doesn't make sense in an `offscreen` or `aside` context (as those aren't interactive).
Therefore, its definition looks like `fun MainRenderScope.input(...) { ... }`

Finally, `OffscreenRenderScope` and `AsideRenderScope` both inherit from `OneShotRenderScope`, since they are guaranteed
by design to only ever run a single time, unlike `MainRenderScope` which is potentially dynamic and interactive.
`OneShotRenderScope` might be useful if for some reason you wanted to define an extension method that excludes the main render scope.

**4 - `RunScope`**

```kotlin
   section {
     ...
┌─ }.run {
│    ...
└─ }
```

`RunScope` is used for `run` blocks. It's useful for extracting logic that deals with handling user input or running
long-running background tasks. Functions like `onKeyPressed`, `onInputChanged`, `addTimer` etc. are defined on top of
this scope.

For example, here is logic for consuming the output of a command being executed within a `run` block, where the output
is sent to an `aside` block.

```kotlin
private fun RunScope.handleConsoleOutput(line: String, isError: Boolean) {
  aside {
    if (isError) red() else black(isBright = true)
    textLine(line)
  }
}

private fun consumeStream(stream: InputStream, isError: Boolean, onLineRead: (String, Boolean) -> Unit) {
  val isr = InputStreamReader(stream)
  val br = BufferedReader(isr)
  while (true) {
    val line = br.readLine() ?: break
    onLineRead(line, isError)
  }
}

fun RunScope.exec(vararg command: String): Process {
  val process = Runtime.getRuntime().exec(*command)
  CoroutineScope(Dispatchers.IO).launch { consumeStream(process.inputStream, isError = false, ::handleConsoleOutput) }
  CoroutineScope(Dispatchers.IO).launch { consumeStream(process.errorStream, isError = true, ::handleConsoleOutput) }
  return process
}

section {
    textLine("Please wait, cloning the repo...")
}.run {
  val process = exec("git", "clone", "https://github.com/varabyte/kotter.git")
  ...
}
```

**`SectionScope`**

To close off all this scope discussion, it's worth mentioning that a `SectionScope` interface exists. It is the base
interface to both `RenderScope` AND a `RunScope`, and using it can allow you to define the occasional helper method that
can be called from both of them.

#### ConcurrentScopedData

The one thing that all scopes have in common is they expose access to a session's `data: ConcurrentScopedData` field.
OK, but what is it?

`ConcurrentScopedData` is a thread-safe hashmap, where the keys are always of type `ConcurrentScopedData.Key<T>`, and
such keys are associated with a lifecycle (meaning that any data you register into the map will always be released when
some parent lifecycle ends, unless you remove it yourself manually first).

Kotter itself manages four main lifecycles: `Session.Lifecycle`, `Section.Lifecycle`, `MainRenderScope.Lifecycle`, and
`Run.Lifecycle` (each associated with the scopes discussed above).

***Note:** No lifecycles are provided for `offscreen` or `aside` blocks at the moment because you could probably just
use the `MainRenderScope.Lifecycle` or `Run.Lifecycle` lifecycles for those cases. Feel free to open up an issue with a
use-case requiring additional lifecycles if you run into one.*

Keep in mind that the `MainRenderScope` dies after a *single* render pass. Often you want to use the
`Section.Lifecycle`, as it survives across multiple runs.

Nothing prevents you from defining your own lifecycle - just be sure to call `data.start` and `data.stop` on it.
Lifecycles can be defined as subordinate to other lifecycles, so if you create a lifecycle that is tied to the `Run`
lifecycle for example, then you don't need to explicitly call `stop` yourself.

You can review the `ConcurrentScopedData` class for its full list of documented API calls, but the three common ways to
add values to it are:

* always overwrite: `data[key] = value`
* add if first time: `data.tryPut(key, value) // returns true if added, false otherwise`
* add if first time but always run some follow-up logic:<br>
  `data.putIfAbsent(key, provideInitialValue = { value }) { ... logic using value ... }`<br>
  * This is essentially a shortcut for calling `tryPut` and then getting the value, but doing so in a way that ensures
    no one else grabs the thread from you in between.

We're almost done here. At this point, all you need is a key. Creating one is easy - just tie it to a lifecycle and the
type of data it represents. Putting it all together:

```kotlin
class MyFeatureState(...)
val MyFeatureKey = Section.Lifecycle.createKey<MyFeatureState>()

fun RenderScope.myFeature() {
  data.putIfAbsent(MyFeatureKey, { MyFeatureState(...) }) {
    // "this" is MyFeatureState
    // This block gets run every time `myFeature` is called, but
    // `MyFeatureState` is only created the first time
  }
}
```

To close this section, we just wanted to say that it was very tempting at first to create a bunch of hardcoded functions
baked inside `Section`, `MainRenderScope`, etc., with access to some private state, but implementing everything through
`ConcurrentScopedData` plus extension methods ensured that we had the same tools and constraints as any user would.

So go forth, and extend Kotter!

### Thread Affinity

Setting aside the fact that the `run` block runs in a background thread, sections themselves are rendered sequentially
on a single thread. Anytime you make a call to run a section, no matter which thread it is called from, a single thread
ultimately handles it. At the same time, if you attempt to run one section while another is already running, an
exception is thrown.

I made this decision so that:

* I don't have to worry about multiple sections `println`ing at the same time - who likes clobbered text?
* Kotter handles repainting by moving the terminal cursor around, which would fail horribly if multiple sections tried
doing this at the same time.
* Kotter embraces the idea of a dynamic, active section preceded by a bunch of static history. If two dynamic blocks
wanted to be active at the same time, what would that even mean?

In practice, I expect this decision won't be an issue for most users. Command line apps are expected to have a main flow
anyway -- ask the user a question, do some work, then ask another question, etc. It is expected that a user won't ever
even need to call `section` from more than one thread. It is hoped that the
`section { ... main thread ... }.run { ... background thread ... }` pattern is powerful enough for most (all?) cases.

### Virtual Terminal

It's not guaranteed that every user's command line setup supports ANSI codes. For example, debugging this project with
IntelliJ as well as running within Gradle are two such environments where functionality isn't available! According to
many online reports, Windows is also a big offender here.

Kotter will attempt to detect if your console does not support the features it uses, and if not, it will open up a
virtual terminal. This fallback gives your application better cross-platform support.

To modify the logic to ALWAYS open the virtual terminal, you can set the `terminal` parameter in `session` like
this:

```kotlin
session(terminal = VirtualTerminal.create()) {
  section { /* ... */ }
  /* ... */
}
```

or you can chain multiple factory methods together using the `runUntilSuccess` method, which will try to start each
terminal type in turn. If you want to mimic the current behavior where you try to run a system terminal first and fall
back to a virtual terminal later, but perhaps you want to customize the virtual terminal with different parameters,
you can write code like so:

```kotlin
session(
  terminal = listOf(
    { SystemTerminal() },
    { VirtualTerminal.create(title = "My App", terminalSize = Dimension(30, 30)) },
  ).runUntilSuccess()
) {
  /* ... */
}
```

### Why Not Compose / Mosaic?

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
  * I suspect this issue with Compose will improve over time, but for the present, it still seems like a non-Compose
  approach could be useful to many.

* Compose is great for rendering a whole, interactive UI, but console printing is often two parts: the active part that
the user is interacting with, and the history, which is static. To support this with Compose, you'd need to manage the
history list yourself and keep appending to it, and it was while thinking about an API that addressed this limitation
that I envisioned Kotter.
  * For a concrete example, see the [compiler demo](examples/compiler).

* Compose encourages using a set of powerful layout primitives, namely `Box`, `Column`, and `Row`, with margins and
  shapes and layers. Command line apps don't really need this level of power, however.

* Compose has a lot of strengths built around, well, composing methods! And to enable this, it makes heavy use of
  features like `remember` blocks, which you can call inside a composable method and it gets treated in a special way.
  But for a simple CLI library, being able to focus on render blocks that don't nest too deeply and not worrying as much
  about performance allowed a more pared down API to emerge.

* Compose does a lot of nice tricks due to the fact it is ultimately a compiler plugin, but it is nice to see what the
  API would kind of look like with no magic at all (although, admittedly, with some features sacrificed).

* Mosaic doesn't support input well yet (at the time of writing this README, maybe this has changed in the future).
  For example, compare [Mosaic](https://github.com/JakeWharton/mosaic/blob/fd213711ce2b828a6436a61d6d345692222bdb95/samples/robot/src/main/kotlin/example/robot.kt#L45)
  to [Kotter](https://github.com/varabyte/kotter/blob/main/examples/mosaic/robot/src/main/kotlin/main.kt#L27).

#### Mosaic comparison

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