![version: 1.2.1](https://img.shields.io/badge/kotter-v1.2.1-blue)
![kotter tests](https://github.com/varabyte/kotter/actions/workflows/gradle-test.yml/badge.svg?branch=main)
![kotter coverage](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/bitspittle/1fab2b6fd23489bdea3f5d1f38e4dcb2/raw/kotter-coverage-badge.json)
<a href="https://varabyte.github.io/kotter">
![kotter docs](https://img.shields.io/badge/docs-grey?logo=readthedocs)
</a>
<br>
![kotlin compatibility jvm](https://img.shields.io/badge/kotlin_[jvm]-1.7+-lightgray?logo=kotlin)
![kotlin compatibility k/n](https://img.shields.io/badge/kotlin_[native]-1.9+-lightgray?logo=kotlin)
<br>
<a href="https://discord.gg/5NZ2GKV5Cs">
  <img alt="Varabyte Discord" src="https://img.shields.io/discord/886036660767305799.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2" />
</a>
[![Bluesky](https://img.shields.io/badge/Bluesky-0285FF?logo=bluesky&logoColor=fff)](https://bsky.app/profile/bitspittle.bsky.social)

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

Kotter is multiplatform, supporting JVM and native targets.

The next sections deal with setting Kotter up, but you may wish to jump straight
[to the usage section ▼](#-usage) to immediately start learning about this library.

## 🐘 Gradle

### 🎯 Dependency

Kotter supports JVM and native targets.

> [!TIP]
> If you're not sure what you want, start with a JVM project. That target is far easier to distribute. It also means
> your project will have access to a very broad ecosystem of Kotlin and Java libraries.
>
> In case it affects your decision, you can read more about [distributing Kotter applications ▼](#-distributing-your-application)
> later in this document.

#### JVM

```kotlin
// build.gradle.kts (kotlin script)
plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.varabyte.kotter:kotter-jvm:1.2.1")
    testImplementation("com.varabyte.kotterx:kotter-test-support-jvm:1.2.1")
}
```

#### Multiplatform

Multiplatform can be useful if you want to distribute binaries to users without requiring they have Java installed on
their machine.

```kotlin
// build.gradle.kts (kotlin script)
plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    // Choose the targets you care about.
    // Note: You will need the right machine to build each one; otherwise, the target is disabled automatically
    listOf(
        linuxX64(), // Linux
        mingwX64(), // Windows
        macosArm64(), // Mac M1
        macosX64(), // Mac Legacy
    ).forEach { nativeTarget ->
        nativeTarget.apply {
            binaries {
                executable {
                    entryPoint = "main"
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.varabyte.kotter:kotter:1.2.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("com.varabyte.kotterx:kotter-test-support:1.2.1")
            }
        }
    }
}
```

> [!NOTE]
> Building native binaries is a little tricky, as you may need different host machines in order to build the various
> binaries. For example, [here is Kotter's CI workflow](https://github.com/varabyte/kotter/blob/main/.github/workflows/publish.yml)
> which runs on both Linux and Mac targets to build platform-specific Kotter artifacts.

#### Testing snapshots

Most users won't ever need to run a Kotter snapshot, so feel free to skip over this section! However, occasionally, bug
fixes and new features will be available for testing for a short period before they are released.

If you ever file a bug with Kotter and are asked to test a fix using a snapshot, you must add an entry for the sonatype
snapshots repository to your `repositories` block in order to allow Gradle to find it:

```diff
// build.gradle.kts

repositories {
  mavenCentral()
+ maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
+   mavenContent { includeGroup("com.varabyte.kotter") }
+ }
}
```

### 🚥 Running examples

If you've cloned this repository, examples are located under the [examples](examples) folder.

#### JVM

Most of the examples (except `examples/native`) target the JVM. To try one of them, you can navigate into it on the
command line and run it via Gradle.

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

> [!WARNING]
> If your terminal does not support features needed by Kotter, which could happen on legacy machines for example, then
> this still may end up running inside a virtual terminal.

#### Multiplatform

Unlike the JVM target, native targets do not have a virtual terminal fallback. So be sure you **do not** use any of the
Gradle run tasks (e.g. `runDebugExecutabule...`). This will also fail if you try to run your program through the IDE via
the green "play" arrow.

Instead, you should link your executable and then run it directly.

For example, on Linux:

```bash
$ cd examples/native
$ ../../gradlew linkDebugExecutableLinuxX64
$ ./build/bin/linuxX64/debugExecutable/native.kexe
```

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

> [!NOTE]
> If truecolor is not supported, terminals may attempt to emulate it by falling back to a nearby color, which may look
> decent! However, to be safe, you may want to avoid rendering smooth gradient color changes, as they may come out
> clumped for some users.

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

> [!NOTE]
> Italics functionality is not currently exposed, as it is not a standard feature and is inconsistently supported across
> terminals.

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

> [!NOTE]
> Scoped text effect methods (like `red { ... }`) work by calling `scopedState` for you under the hood.

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

> [!IMPORTANT]
> The `liveVarOf` method is provided by the `session` block. For many remaining examples, we'll elide the
> `session` boilerplate, but that doesn't mean you can omit it in your own program!

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

> [!NOTE]
> You don't have to worry about locking within a `section { ... }` block. Data access is already locked for you in that
> context.

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

These methods are enough in most cases.

> [!NOTE]
> If you call `signal` before you reach `waitForSignal`, then `waitForSignal` will just pass through without stopping.

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
    // although often `rejectInput()` specifies your intention more clearly
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
    //            ^^^^^^^^^^^^^^^^^^^^
    // Don't return the whole word; just the part that comes after the user's input so far.
  }
}
```

Kotter provides a very useful implementation out of the box, called `Completions`, which lets you specify a list of
values that will be autocompleted as long as the user's input matches one of them.

```kotlin
section {
  text("Continue? "); input(Completions("yes", "no"))
}.run()
```

Order matters! If nothing is typed, the first completion will be suggested. If multiple values match, the one earliest
in the list will be suggested.

#### Altering the input's appearance

There are two callbacks you can pass into `input` to affect how it looks, `viewMap` (for altering the character that
appears) and `customFormat` (for applying rendering effects). Both callbacks operate character by character, exposed in
the callback as `ch`. You can also query the whole text string in (`text`) along with the current index (`index`) in
case those help you with the current logic.

##### `viewMap`

`viewMap` intercepts an incoming character and outputs a new character which will get rendered in its place. This is a
visual change only! The `onInputEntered` callback will still be triggered with the original input without the view
mapping applied.

It is commonly used to mask something like a password field:

```kotlin
var password = ""
section {
  text("Password: "); input(viewMap = { '*' })
}.runUntilInputEntered {
  onInputEntered { password = input }
}

// "password" will be set to the actual password; user will only ever see "*"s
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-password.gif)

##### `customFormat`

`customFormat` lets you change the color or text effects of the input. Within the callback, you can call methods
`color(...)`, `bold()`, `underline()`, and `strikethrough()` to apply each effect, respectively.

If you want to highlight valid characters and/or emphasize invalid characters in your input, `customFormat` is the way
to go:

```kotlin
section {
  text("PIN: "); input(customFormat = { if (ch.isDigit()) green() else red() })
}.runUntilInputEntered {
  onInputEntered { if (input.any { !it.isDigit() }) rejectInput() }
}
```

![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-pin.gif)

#### Multiline Input

Occasionally, you may want to allow users to type long-form text, with newlines. `input` is quite useful but it
intentionally doesn't allow newlines. That's because pressing ENTER fires the `onInputEntered` event.

If you need longer form input, you can reach to `multilineInput`. Users must terminate their input by sending an EOF
signal (generated by pressing CTRL-D), since ENTER is now used for newlines.

> [!NOTE]
> Unfortunately, SHIFT+ENTER, although it is commonly used for handling newlines in most modern editors, is unavailable
> to us as consoles don't reveal meta-key (e.g. CTRL, SHIFT) states. As console application developers, we're
> essentially blind to them. CTRL-D is the traditional way to close input streams in many CLIs, because the system
> translates those keystrokes into an EOF signal, which is all the applications see.

Multiline inputs allow the user to navigate text typed in by pressing the arrow, home, end, page up, and page down keys.
In order to support this feature, multiline inputs always start on a new line and any text following it will also appear
on a new line after the input block.

```kotlin
section {
  black(isBright = true) { text("Send a text message (press CTRL-D when finished)") }
  multilineInput()
}.runUntilInputEntered {
  onInputEntered { sendMessage(input.trim()) }
}
```
![Code sample in action](https://github.com/varabyte/media/raw/main/kotter/screencasts/kotter-multiline.gif)

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
  addTimer(500.milliseconds) {
    println("500ms passed!")
    signal()
  }
}
```

You can create a repeating timer by passing in `repeat = true` to the method. And if you want to stop it from repeating
at some point, set `repeat = false` inside the timer block when it is triggered:

```kotlin
val BLINK_TOTAL_LEN = 5.seconds
val BLINK_LEN = 250.milliseconds
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
  addTimer(250.milliseconds, repeat = true) { blinkOn = !blinkOn }
  /* ... */
}
```

> [!IMPORTANT]
> Unlike all the other events we discussed earlier, `onFinishing` is registered directly against the underlying
> `section` and not inside the `run` block, because it is actually triggered AFTER the run pass is finished but before
> the block is torn down.

`onFinishing` will only run after all timers are stopped, so you don't have to worry about setting a value that an
errant timer will clobber later.

### 🎥 Animations

Animations make a huge difference for how the user experiences your application, so Kotter strives to make it trivial to
add them into your program.

#### Text Animation

You can easily create quick animations by calling `textAnimOf`:

```kotlin
var finished = false
val spinnerAnim = textAnimOf(listOf("\\", "|", "/", "-"), 125.milliseconds)
val thinkingAnim = textAnimOf(listOf("", ".", "..", "..."), 500.milliseconds)
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
val SPINNER_TEMPATE = TextAnim.Template(listOf("\\", "|", "/", "-"), 250.milliseconds)

val spinners = (1..10).map { textAnimOf(SPINNER_TEMPLATE) }
/* ... */
```

#### Render animations

If you need a bit more power than text animations, you can use a render animation instead. You create one with a
callback that is given a frame index and access to the current render scope. You can interpret the frame index however
you want and use the render scope to call any of Kotter's text rendering methods that you need.

Declare a render animation using the `renderAnimOf` method and then invoke the result inside your render block:

```kotlin
val exampleAnim = renderAnimOf(numFrames = 5, 250.milliseconds) { i -> /* ... */ }
section {
  // Call your render animation passing in the section block (i.e. `this`) as a parameter
  exampleAnim(this)
  /* ... */
}
```

For example, let's say we want to rotate through a list of colors and apply those to some text. Text animations only
deal with raw text and don't have access to text effects like colors and styles. Therefore, we need to use a render
animation instead, giving us access to the `color(Color)` method:

```kotlin
// Note: `Color` is a Kotter enum that enumerates all the standard colors it supports

val colorAnim = renderAnimOf(Color.values().size, 250.milliseconds) { i ->
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
  40.milliseconds,
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
too late to measure it!

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

> [!NOTE]
> Although you usually won't need to, you can create multiple renderers per offscreen buffer, each which manages its own
> state for what row to render out next.

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

![Bordered example](https://github.com/varabyte/media/raw/main/kotter/images/border-example.png)

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
val searchingAnim = textAnimOf(listOf("", ".", "..", "..."), 500.milliseconds)
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

Asides are very useful if you have some long-running process that generates text as a side effect. You could imagine
a compiler spitting out warnings and errors as it continues to process more code, or a test runner reporting failures
as it continues to run more tests. In fact, Kotter provides a [fake compiler example](examples/compiler) that you can
reference.

### 🪟 Grids

Kotter provides support for creating arbitrarily sized grids with multiple rows and columns.

With Kotter's approach to grids, you specify the number of columns explicitly; rows are auto added as you declare new
grid cells.

```kotlin
section {
  // A grid with two columns, each with space for 6 characters
  grid(Cols(6, 6), characters = GridCharacters.CURVED) {
    cell { // Auto set to row=0, col=0
      textLine("Cell1")
    }
    cell { // Auto set to row=0, col=1
      textLine("Cell2")
    }

    // Third cell in a grid with two columns creates a new row
    cell { // Auto set to row=1, col=0
      textLine("Cell3")
    }
    cell { // Auto set to row=1, col=1
      textLine("Cell4")
    }

    // You can explicitly set the row and column if you want. Rows and columns
    // are 0-indexed.
    cell(row = 2, col = 1) { // Jump over cell row=2,col=0
      textLine("Cell6")
    }
  }
}.run()
```

![Simple grid example](https://github.com/varabyte/media/raw/main/kotter/images/kotter-grid-simple.png)

> [!TIP]
> There is also a `Cols.uniform` method for when you want to create multiple columns of the same width. For example,
> instead of `Cols(6, 6)` which we used above, you could also call `Cols.uniform(2, width = 6)`. It is a bit more
> verbose but may express intention more clearly.

You can check out the [grid example](examples/grid/src/main/kotlin/main.kt) for a more comprehensive example.

#### Fit- and star-sized columns

Fixed width columns are useful, but Kotter also provides even more functionality via fit- and star-sized columns.

A **fit-sized column** will check all cells it contains and choose a width that fits all of them.

![Fit grid example](https://github.com/varabyte/media/raw/main/kotter/images/kotter-grid-fit-sized.png)

A **star-sized column** will be sized dynamically based on how much space is remaining (more on this in a bit). If you
have multiple star-sized columns, then space will be divided between them based on their ratio with each other.

For example, if you have one star-sized column set to "2" and another set to "1", then the first column will be twice as
wide as the second. If you have two star-sized columns both set to "2" then they will share the remaining space equally.

![Star grid example](https://github.com/varabyte/media/raw/main/kotter/images/kotter-grid-star-sized.png)

To determine "remaining space", the `grid` method accepts a `targetWidth` parameter. If you don't have any star-sized
columns, the `targetWidth` value does nothing. If you do, then the grid will subtract all fixed and fit width values
from it and share any remaining space between the star-sized columns.

For a trivial example, say you have a two-column grid with `targetWidth` set to 10. The first column is fixed to 4, and
the second column is set to star-sized. The star-sized column will then receive 6 characters of space.

If you do not set the `targetWidth` at all, then all star-sized columns will shrink to size 1.

#### Building column specifications

Earlier, we used `Cols(6, 6)`, a convenience constructor that accepts only integer values indicating fixed column
widths. But for more control, you can construct the `Cols` class using a builder block:

```kotlin
grid(Cols { fit(); fixed(10); star() }, targetWidth = 80) {
  /* ... */
}
```

#### Column properties

In addition to their base value, columns have a few properties you can set: *minimum value*, *maximum value*, and
*justification*.

Here's an example of setting all three properties:

```kotlin
grid(
  Cols {
    fit(maxValue = 10)
    fixed(10, justification = Justification.CENTER)
    star(minValue = 5)
  },
  targetWidth = 80
) {
  /* ... */
}
```

The above means that the first column will be fit-sized but will never exceed 10 characters. The second column is fixed
to 10 characters, and its contents will be centered. The final column is star-sized, but it will never be less than 5
characters.

#### Spanning multiple rows and columns

You can declare that a cell should span multiple rows and/or columns by setting the `rowSpan` and `colSpan` parameters.
If either `rowSpan` or `colSpan` are not specified, then they default to 1.

> [!CAUTION]
> If `colSpan` is set to a value that would cause the cell to go out of bounds of the number of columns in this grid, an
> exception is thrown.

A few examples should help illustrate this:

```kotlin
// Spanning multiple columns

grid(Cols(3, 3, 3), characters = GridCharacters.CURVED) {
  cell(row = 2) // Force three rows to be created
  cell(row = 0, col = 0, colSpan = 3)
}
```

![Col spanning grid example](https://github.com/varabyte/media/raw/main/kotter/images/kotter-grid-col-span.png)

```kotlin
// Spanning multiple rows

grid(Cols(3, 3, 3), characters = GridCharacters.CURVED) {
  cell(row = 0, col = 0, rowSpan = 3)
}
```

![Row spanning grid example](https://github.com/varabyte/media/raw/main/kotter/images/kotter-grid-row-span.png)

```kotlin
// Spanning both

grid(Cols(3, 3, 3, 3), characters = GridCharacters.CURVED) {
  cell(row = 3) // Force four rows to be created
  cell(row = 1, col = 1, rowSpan = 2, colSpan = 2)
}
```

![Spanning both rows and cols grid example](https://github.com/varabyte/media/raw/main/kotter/images/kotter-grid-both-span.png)

> [!CAUTION]
> A cell spanning columns will inherit its justification from its left-most cell. Additionally, any cell that spans
> multiple columns will not be included in any fit-size calculations.

#### Auto-layout of cells

When you declare a `cell` block without specifying a row or column, it will automatically be placed in the next empty
slot after the last cell that was declared. This goes from left-to-right, top-to-bottom.

For example:

```kotlin
grid(Cols(1, 1, 1)) {
  cell(row = 1) { text("1") } // declared cell at row=1, col=0
  cell(row = 0) { text("2") } // declared cell at row=0, col=0
  cell { text("3") } // next empty slot is row=0, col=1
  cell { text("4") }
  cell { text("5") }
}

// +-+-+-+
// |2|3|4|
// +-+-+-+
// |1|5| |
// +-+-+-+
```

While the above example feels forced (it should be pretty rare to intentionally register cells out of order), the way
cells flow is intuitive when used in conjunction with row spans:

```kotlin
grid(Cols(1, 1, 1)) {
  cell(rowSpan = 2) { text("1") } // declared cell at row=0, col=0
  cell { text("2") }
  cell { text("3") }
  cell { text("4") }
}

// +-+-+-+
// |1|2|3|
// | +-+-+
// | |4| |
// +-+-+-+
```

Finally, here's an example of how cells flow following a cell spanning multiple columns:

```kotlin
grid(Cols(1, 1, 1)) {
  cell(colSpan = 2) { text("1") }
  cell { text("2") }
  cell { text("3") }
}

// +---+-+
// |1  |2|
// +-+-+-+
// |3| | |
// +-+-+-+
```

### 🪝 Shutdown Hook

Terminal applications can be forcefully interrupted if the user presses CTRL-C. Some apps may want to handle this.

While you can register hooks for such an event directly with the underlying platform, Kotter offers an additional
API for this: `addShutdownHook`.

You can register a handler insider your run block, like so:

```kotlin
section { /* ... */ }.run {
  addShutdownHook { /* this will get called only if the user presses ctrl-c */ }
}
```

Unlike a shutdown hook registered with the system, Kotter's managed shutdown hook will additionally try to give the
render block one more chance to run before the program exits. You can take advantage of this to send a message to the
user:

```kotlin
var emergencyShutdown by liveVarOf(false)
section {
  /* ... */
  if (emergencyShutdown) {
    yellow {
      textLine("This program is exiting NOW because the user pressed CTRL-C.")
      textLine("We sent a request to shut down the server but could not confirm it was received.")
      textLine("Consider running `./stop-server.sh` later to make sure it actually stopped.")
    }
  }
}.run {
  addShutdownHook {
    sendServerShutdownRequestAsync()
    emergencyShutdown = true
  }
}
```

It's important that you never run any long-running logic inside a shutdown hook. If your program continues to run for
too long after an interrupt request, the system may just halt your program anyway.

Finally, you should not rely on shutdown hooks actually getting run. They don't get triggered if the system exits
normally, the program crashes, or if the process gets aggressively halted by the OS (perhaps because things were taking
too long to shut down, or maybe the user issued a kill command from the terminal).

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

The `section { ... }` block receives a `RenderScope` and the `run { ... }` block receives a `RunScope`. These are
discussed next.

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

As the `run` method is a suspend function, you may declare your `RunScope` extending methods suspend as well:

```kotlin
suspend fun RunScope.doLongRunningTask() { /* ... */ }

section {
    textLine("Please wait...")
}.run {
    doLongRunningTask()
}
```

**`SectionScope`**

To close off all this scope discussion, it's worth mentioning that a `SectionScope` interface exists. It is the base
interface to both `RenderScope` AND a `RunScope`, and using it can allow you to define the occasional helper method that
can be called from both of them.

It's not expected that most users will ever use this, but it can be a way to write a common getter that both the render
block and run block can use (perhaps for data that is also set by the run block elsewhere).

#### ConcurrentScopedData

The one thing that all scopes have in common is they expose access to a session's `data` field. OK, but what is it?

`ConcurrentScopedData` is a thread-safe hashmap, where the keys are always of type `ConcurrentScopedData.Key<T>`, and
such keys are associated with a `ConcurrentScopedData.Lifecycle` (meaning that any data you register into the map will
always be released when some parent lifecycle ends, unless you remove it yourself manually first).

Kotter itself manages four lifecycles: `Session.Lifecycle`, `Section.Lifecycle`, `MainRenderScope.Lifecycle`, and
`Run.Lifecycle` (each associated with the scopes discussed above).

> [!NOTE]
> No lifecycles are provided for `offscreen` or `aside` blocks at the moment. Feel free to open up an issue with a
> use-case requiring additional lifecycles if you run into one.

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
    /* ... */
  } finally {
    data.stop(MyLifecycle) // Side effect: Removes MySetting from data
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
  * This is essentially a shortcut for calling `tryPut` and then getting the value, but doing so in a lock-safe manner
    that ensures no one else grabs the thread from you in between.

By having a session own and expose such a data structure, it makes it possible for anyone to write their own extension
methods on top of Kotter, using data as a way to manage long-lived state. For example, `input()`, which may get called
many times in a row as the section rerenders, can distinguish the first time it is called from later calls based on
whether some value is present in the data cache or not.

To close this section, we just wanted to say that it was very tempting at first to create a bunch of hardcoded functions
baked inside `Section`, `MainRenderScope`, etc., with access to some private state, but implementing everything through
`ConcurrentScopedData` plus extension methods ensured that we were using the same tools as users.

So go forth, and extend Kotter!

### 🧪 Testing

Kotter includes a separate library that provides useful testing utilities, called `kotter-test-support`. You can review
the [Gradle section▲](#-gradle) from earlier to see how to include it in your project.

The library comes with its [own README](kotterx/kotter-test-support/README.md) which goes into more detail about how
to write Kotter unit tests.

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

> [!IMPORTANT]
> The virtual terminal is only supported for JVM targets. Kotlin/Native targets don't implement this feature.

It's not guaranteed that your program will be run in an interactive way, or even that you won't be called in a legacy
terminal (e.g. on Windows) that doesn't support ANSI virtual codes.

For example, debugging this project with IntelliJ as well as running within Gradle are two such environments where
interactivity isn't available! Since in that case, IntelliJ/Gradle are already consuming the interactivity themselves,
and running your program in a more limited environment.

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

### 🔙 Fallback in Non-Interactive Terminals

Kotter is designed to *only* run within a rich, interactive terminal environment, such as a console with ANSI support
or inside a virtual terminal.

However, there may be cases that Kotter won't be able to run. For example:

1. You're trying to run Kotter in an environment that is both non-interactive AND doesn't have any graphical system
   enabled (like ssh-ing to some remote machine).
2. You are in a non-interactive environment, and you constructed your session in a way that excludes using a
   `VirtualTerminal`.
3. You are in a non-interactive environment, and you are using Kotlin/Native (which does not provide a `VirtualTerminal`
   implementation).

If you run a console app in such a non-interactive environment, calls like `println` and `readLine` still work, but any
attempts to move the cursor, erase previous lines, and many other ANSI commands which Kotter builds on top of are not
supported.

Such cases are increasingly rare on modern machines, so you may just decide to ignore them and crash!

However, if you're very determined, you could consider writing some parts of your program twice, once with Kotter using
all its fancy bells and whistles, and a second time providing a much simpler presentation, limited to printing
information and asking the occasional question.

To accomplish this, you can use the following code structure:

```kotlin
private fun Session.runKotterLogic() { /* ... */ }
private fun runFallbackLogic() { /* ... */ }

fun main() {
    var kotterStarted = false
    try {
        session {
            kotterStarted = true
            runKotterLogic()
        }
    } catch (ex: Exception) {
        if (!kotterStarted) {
            runFallbackLogic()
        } else {
            // This exception came from after startup, when the user was
            // interacting with Kotter. Crashing with an informative stack
            // is probably the best thing we can do at this point.
            throw ex
        }
    }
}
```

For a concrete example, imagine you are writing a file downloading app. You can have the Kotter version show
animated progress bars, but if you end up in the fallback zone, you can simply print "10%... 20%... 30%..." instead.
Both sections could delegate to some downloader class that did all the heavy lifting -- you should
absolutely share as much non-UI code as you can!

### 📦 Distributing Your Application

You finished your Kotter application. Congratulations!! 🎉

Now what? How do you get your amazing program to your users?

Let's explore a few options.

#### Package your JVM application as a zip (or tar) file

**Pros:**
* Trivial to do.
* Easy to share.
* Access to the whole JVM ecosystem.

**Cons:**
* User must extract files on their machine, a step they aren't used to worrying about.
* User must have Java installed.

If your application targets the JVM, you can easily build zip and tar files of your project using the `assembleDist`
task:

```bash
$ cd yourkotterapp
$ ./assembleDist
$ cd build/distributions
$ ls
yourkotterapp-version.tar  yourkotterapp-version.zip
```

You can ask your user to download either file, extract it, and then run the program under the bin folder:

```bash
$ unzip yourkotterapp-version.zip
$ ./yourkotterapp-version/bin/yourkotterapp
```

---

#### Publish your JVM application to a package manager

**Pros:**
* Trivial for the user to install (although this may install Java as a side effect).
* Access to the whole JVM ecosystem.

**Cons:**
* Everyone has their favorite package manager, and you won't be able to satisfy everyone.
* Some package managers take a lot of effort to set up.
* Some users won't want to use a package manager, so you'll need to include other options anyway.

For a concrete example, let's consider Homebrew, a very popular package manager. They're one of the easier ones to
support -- you can create a custom repo that declares a manifest ([example here](https://github.com/varabyte/homebrew-tap/blob/main/Formula/kobweb.rb)).
Once set up, a user can install / update your software simply by running: `brew install yourkotterapp`

Notice how that manifest declares JDK11 as a dependency. You'll need to figure out how to do that with each package
manager you decide to support.

> [!NOTE]
> I have a project that uses [JReleaser](https://jreleaser.org/guide/latest/reference/packagers/index.html)
> to publish my program to several package managers with a single Gradle publish task. If you decide to try JReleaser in
> your own project, you can review
> [my jreleaser block in this build.gradle.kts](https://github.com/varabyte/kobweb/blob/a8910bf5168a3e27be88ab49fce8b0a86322caac/cli/kobweb/build.gradle.kts#L49).

---

#### Build a Kotlin/Native target

**Pros:**
* No Java required on the user's machine.
* Potentially small final binary size.
* Relatively easy to set up.

**Cons:**
* Will require multiple host machines if you want to build binaries for all platform targets.
* No access to the broader JVM ecosystem.
* Native targets don't provide a virtual terminal.

Configuring a Kotter/Native application is relatively easy but outside the scope of this document. Start with
[the official docs](https://kotlinlang.org/docs/native-overview.html) and review the
[native example](https://github.com/varabyte/kotter/tree/main/examples/native) for guidance.

Once your Kotlin/Native project is set up, you can build it using `./gradlew linkDebugExecutable[Host]` (or
`./gradlew linkReleaseExecutable[Host]`) which puts a binary under `./build/bin/[host]/debugExecutable/native.exe`
(or `native.kexe` on Linux).

Unless you already have Mac, Windows, and Linux machines at home, you may want to use a CI to build binaries for you.
How to do this is also outside the scope of this document, but I personally use GitHub Actions to handle this, creating
a workflow that runs on several machines, publishing artifacts based on which runner is active. You can read more about
[GitHub CI strategies here](https://docs.github.com/en/actions/using-jobs/using-a-matrix-for-your-jobs).

For example, you could set up a workflow to link various binaries into executables whenever a new commit is pushed to
the main branch. You could then use the [upload artifact](https://github.com/actions/upload-artifact) action to push
binaries to [a location you can download from](https://github.com/actions/upload-artifact#where-does-the-upload-go)
later.

> [!NOTE]
> For reference, you may wish to refer to [Kotter's publishing workflow script.](https://github.com/varabyte/kotter/blob/main/.github/workflows/publish.yml)
> It doesn't use the upload action, but you can see how we run multiple target hosts in order to build all the different
> flavors of artifacts.

Once built, you can share your binaries with your users, either from cloud storage somewhere or by publishing it via a
package manager (as [discussed above ▲](#publish-your-jvm-application-to-a-package-manager)).

---

#### Use `jlink / jpackage` to convert your JVM project into a binary

**Pros:**
* No Java required on the user's machine.
* Access to the whole JVM ecosystem.

**Cons:**
* Requires JDK14 or newer.
* Will require multiple host machines if you want to build binaries for all platform targets.
* May be complicated to set up.
* May require your CI have access to two different JDKs, one for compiling your code and one for running the jpackage
  step, in case you are intentionally compiling your code with an older JDK version.

> [!WARNING]
> This section is incomplete as I have not found time to try out these tools yet. However, any readers familiar with
> them are welcome to [contact me](mailto:bitspittle@gmail.com) with information so that I can update this section.

`jlink`, introduced in Java 9, allows you to assemble modules into a custom runtime image, which can significantly
reduce the final size of a runtime you'd want to ship (as you'd be excluding a bunch of standard library code you don't
need). [Official docs here](https://docs.oracle.com/javase/9/tools/jlink.htm).

`jpackage`, introduced in Java 14, allows you to bundle a JVM program plus a runtime (e.g. produced by `jlink`) into
a final binary + installer, one per platform. [Official docs here](https://docs.oracle.com/en/java/javase/14/jpackage/index.html).

JReleaser, discussed in a previous section, exposes support for [jlink](https://jreleaser.org/guide/early-access/reference/assemble/jlink.html)
and [jpackage](https://jreleaser.org/guide/early-access/reference/assemble/jpackage.html) configurations.

If you don't need access to the JVM library ecosystem, using [Kotlin/Native ▲](#build-a-kotlinnative-target) is probably
easier.

---

#### Build your JVM application with GraalVM Native Image

**Pros:**
* No Java required on the user's machine.
* Great flexibility. User can use the entire JVM library ecosystem while still producing a binary that can run anywhere.
* Potentially small final binary size (after using UPX).

**Cons:**
* Will require multiple host machines if you want to build binaries for all platform targets.
* GraalVM can be annoying to install.
* GraalVM can be very fussy.
* Can be frustrating to chase down runtime exceptions caused by a misconfigured compile.

> [!WARNING]
> This section is incomplete as my own experimentation fell a bit short with it. However, GraalVM is a very promising
> technology, so if anyone reading this knows how to get this solution to work, please [contact me](mailto:bitspittle@gmail.com)
> and I can update this section.

[GraalVM](https://www.graalvm.org/) is a high-performance JDK distribution, while
[GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/) is an ahead-of-time compiler for
Java programs. What this means to you is that you can build a Java application jar and then target it with native image
to convert it into a standalone binary.

You may be able to further minify your GraalVM output with [UPX](https://upx.github.io/), which may be able to shrink
your final binary size dramatically.

> [!IMPORTANT]
> If you decide to try using GraalVM on your project, you should strongly consider excluding the virtual terminal by
> overriding the default `terminal` parameter when creating a session:
>
> ```kotlin
> session(terminal = SystemTerminal.create()) { /* ... */ }
> ```
>
> This allows GraalVM to strip out all Swing code, which is otherwise very tricky to configure.

---

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

### 🧶 How About Mordant?

[Mordant](https://github.com/ajalt/mordant) is another Kotlin console API. And honestly, it's awesome. It's worth a
look!

Mordant provides an API where you instantiate a `Terminal` object and issue commands to it directly. It's very clean!
The library also provides markdown support and builders for complex tables, which are really nice features that don't
currently exist in Kotter. It has a few other opinionated components, such as an animated progress bar and an input
prompter that requires the answer be one of a few choices.

If you are mostly rendering output text, Mordant may honestly result in more streamlined code. It also handles falling
back better if you run it inside a terminal that does not support interactive mode. (Kotter will open a virtual terminal
if it can, or crash if it can't).

You may still prefer using Kotter for cases where you plan to have a lot of interactive elements, such as several
animations running side by side in parallel, or if you want keypress handling, or if you want to want the ability to
manage timers, or if you want to show interactive prompts with rich auto-completion behavior.

Additionally, Kotter's split between the `section` block and `run` blocks benefit more and more in increasingly complex
scenarios. It can be nice to have a clear separation of the rendering logic from background computation logic.

Still, for concreteness, let's take a few examples from Mordant's README and show them side-by-side with equivalent
Kotter implementations:

**Multiple styles**
```kotlin
// Mordant
val t = Terminal()
t.println("${red("red")} ${white("white")} and ${blue("blue")}")

// Kotter
section {
    red { text("red") }; text(' ')
    white { text("white")}; text(" and ")
    blue { textLine("blue") }
}.run()
```

**Nest styles**
```kotlin
// Mordant
t.println(white("You ${(blue on yellow)("can ${(black + strikethrough)("nest")} styles")} arbitrarily"))

// Kotter
section {
    white {
        text("You ")
        blue { yellow(BG) {
            text("can ")
            black { strikethrough {
                text("nest")
            }}
            text(" styles")
        }}
        textLine(" arbitrarily")
    }
}.run()
```

**Reuse styles**
```kotlin
// Mordant
val style = (bold + white + underline)
t.println(style("You can save styles"))
t.println(style("to reuse"))

// Kotter
fun RenderScope.withStyle(block: () -> Unit) {
  bold { white { underline { block() }}}
}
section {
  withStyle { textLine("You can refactor styles") }
  withStyle { textLine("to reuse") }
}.run()
```

**Animating a horizontal bar**
```kotlin
// Mordant
val t = Terminal()
val a = t.textAnimation<Int> { frame ->
  (1..50).joinToString("") {
    val hue = (frame + it) * 3 % 360
    t.colors.hsv(hue, 1, 1)("━")
  }
}

t.cursor.hide(showOnExit = true)
repeat(120) {
  a.update(it)
  Thread.sleep(25)
}

// Kotter
val barAnim = renderAnimOf(numFrames = 120, 25.milliseconds) { frame ->
    for (i in 1..50) {
        val hue = ((frame + i) * 3) % 360
        hsv(hue, 1f, 1f) { text("━") }
    }
}
section {
    barAnim(this)
}.runFor(3.seconds)
```

**Prompting for input**
```kotlin
// Mordant
val t = Terminal()
val response = t.prompt("Choose a size", choices=listOf("small", "large"))
t.println("You chose: $response")

// Kotter
val choices = listOf("small", "large")
var choice: String = ""
section {
    text("Choose a size [${choices.joinToString()}]: "); input(Completions(*choices))
}.runUntilInputEntered {
    onInputEntered {
        if (input !in choices) rejectInput() else choice = input
    }
}
section {
    text("You chose: $choice")
}.run()
```

The above samples definitely look really nice in Mordant, and if those cases capture the main sort of functionality you
were planning to use in your own app, Mordant may be the better API for your project.

Meanwhile, for examples that respond to user input like [snake](examples/snake), or which do a lot of clearing /
repainting like [doomfire](examples/doomfire), or which query for input in the middle of a bunch of other text like
[wordle](examples/wordle), Kotter may be the better choice in those cases.

And finally, it's possible to use Kotter and Mordant together. For example, referring back to the
[fallback section above ▲](#-fallback-in-non-interactive-terminals), you can use Mordant in the fallback block, since
it provides a friendlier API than raw `println`/`readLine` calls.
