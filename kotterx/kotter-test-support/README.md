# Kotter Test Support

Kotter applications are fun to write, sure, but how do you test them?

This library is a collection of utility classes and methods to help you do exactly that! This README aims to cover the
main ways you can use it to test your Kotter applications.

> [!NOTE]
> For `assertThat` calls used in the examples below, those come from my testing assertion
> library [Truthish](https://github.com/varabyte/truthish), but of course you can use any assertion approach you like.

## 👶 Basic Usage

### Test session

Kotter tests use the `testSession` utility method to create a Kotter session bound to an in-memory test terminal.

This terminal comes with a handful of powerful test methods which will be discussed throughout the rest of this
document.

The basic structure of every Kotter test is as follows:

```kotlin
@Test
fun basicKotterTestStructure() = testSession { terminal ->
    // Your test code goes here
}
```

The terminal is created for the test and destroyed afterward. You can write tests confidently knowing that each one
gets to work with its own isolated terminal.

### Terminal buffer

The most fundamental way to query a terminal's contents are by checking its `buffer` property directly. You can also use
the `lines()` extension method (which is the same buffer data but split on newlines).

```kotlin
section {
    textLine("This is a test...")
}.run()

assertThat(terminal.buffer).isEqualTo(
    "This is a test...\n" + Codes.Sgr.Reset
)

// Alternately:
assertThat(terminal.lines())
    .containsExactly(
        "This is a test...",
        Codes.Sgr.Reset.toString()
    ).inOrder()
```

Most users should not be checking `buffer` or `lines()` directly. They require you to be familiar with both ANSI escape
codes *and* how Kotter instructions generate them. Furthermore, the order of escape codes is not guaranteed to be stable
in future versions of the library.

However, they can be very useful when debugging why a test isn't working. You can use the `replaceControlCharacters`
utility method plus `println`s to essentially dump the state of the terminal:

```kotlin
section {
    textLine("This is a test...")
}.run()

println(terminal.buffer.replaceControlCharacters())
```

In the above case, this results in the following output
```
This·is·a·test...
\e[0m
```

> [!NOTE]
> If you don't call `replaceControlCharacters`, the `println` will process the escape codes, often swallowing them. This
> can be problematic when you're scratching your head at the test framework yelling at you that "Test string" is not
> equal to "Test string"! (If this does happen to you, it's likely because the strings are not equal due to differing
> escape codes).
>
> Additionally, as you can see, spaces are also replaced with `·` for clarity. This can help users debug the case where
> an equality check is failing due to trailing spaces.

> [!TIP]
> The `\e[0m` text above represents an ANSI escape code. You can read more
> about [CSI sequences](https://en.wikipedia.org/wiki/ANSI_escape_code#CSI_(Control_Sequence_Introducer)_sequences)
> and [SGR parameters](https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_(Select_Graphic_Rendition)_parameters) if
> you're curious to learn more about them, as these both are used heavily in Kotter.
>
> In this specific case, the `\e[` prefix indicates a CSI control sequence, while the `m` suffix
> indicates the preceding numeric value should be parsed as an SGR (Select Graphic Rendition) parameter. The number `0`
> here refers to the SGR "reset" command (as in "reset any graphical styles set up to this point"). Remember, Kotter
> sections always clear all styles upon exiting, so you'll see this particular escape code a lot if you start printing
> stuff out.

You can also apply `stripFormatting` to the buffer (or the `lines()` output), at which point, it could be a quick way to
assert expected output, for example in a test like this:

```kotlin
section {
    bold { textLine("Instructions") }
    text("Press "); cyan { text("ARROW KEYS") }; textLine(" to move")
    text("Press "); cyan { text("SPACE") }; textLine(" to fire")
    text("Press "); cyan { text("Q") }; textLine(" to quit")
}.run()

assertThat(terminal.lines().stripFormatting())
    .containsExactly(
        "Instructions",
        "Press ARROW KEYS to move",
        "Press SPACE to fire",
        "Press Q to quit",
        ""
    ).inOrder()
```

### Resolving rerenders

Kotter sections can run multiple times. In a normal Kotter application, each time a new render happens, the output of
the previous render gets wiped out and replaced with the new one.

In contrast, the test terminal's buffer is not aware of repaints at all. As you apply render after render, they all
accumulate into the buffer (unless you call `terminal.clear()` at some point).

However, tests are often only interested in the final state of the terminal after all the renders have been applied,
rather than concerning themselves with internal, temporary states.

You can call `resolveRerenders` to produce output that discards previous, stale renders. This method returns its output
as a list of separate lines (i.e. a `List<String>`):

```kotlin
var count by liveVarOf(0)
section {
    textLine("Final count: $count")
}.run {
    for (i in 0 until 3) {
        count++
        delay(1000)
    }
}

println(terminal.resolveRerenders().replaceControlCharacters().joinToString("\n"))
```

which, after a few seconds pass, prints out:

```
Final·count:·3
\e[0m
```

Similar to `terminal.buffer` and `terminal.lines()`, you are not expected to call this method directly yourself outside
of local debugging.

The next section will introduce a very useful utility method which calls `resolveRerenders` under the hood for you.

### Asserting the terminal's state

`assertMatches` lets you essentially declare a second Kotter section which will get compared with the original section.
This provides the perfect level of abstraction for most tests.

A concrete example should make this clear. Imagine we are testing a method that renders a progress bar given some
arguments:

```kotlin
import com.example.utils.progress.renderProgressBar

var percent by liveVarOf(0) 
section {
    text("Progress: ")
    renderProgressBar(barLength = 10, percent)
    textLine(" $percent%")
}.run {
    percent = 70
}

terminal.assertMatches {
    textLine("Progress: #######--- 70%")
}
```

### Blocking progress until a condition is met

Sometimes, you will want to verify intermediate render states instead of repainting over them.

To support this, we provide the ability to wait in the `run` block until some condition is met.

```kotlin
var blinkOn by liveVarOf(false)
section {
    if (blinkOn) invert()
    textLine("Blinking test.")
}.onFinishing {
    blinkOn = false
}.run {
    blockUntilRenderMatches(terminal) {
        textLine("Blinking test.")
    }

    blinkOn = !blinkOn
    blockUntilRenderMatches(terminal) {
        invert()
        textLine("Blinking test.")
    }
}

terminal.assertMatches {
    textLine("Blinking test.")
}
```

Without blocking, we wouldn't be able to assert, with confidence, that the blinking effect was on at the end and that
the `onFinishing` block was responsible for turning it off.

> [!IMPORTANT]
> In order to prevent blocking from freezing tests on a CI, the `blockUntilRenderMatches` and `blockUntilInputMatches`
> methods have a default timeout of 1 second. You can pass in a longer timeout, including `Duration.INFINITE`, on a
> case-by-case basis if you need this to last longer.
>
> Normally, Kotter operations should take no longer than a few milliseconds, and in our experience, 1 second has never
> resulted in a false negative.

## ⌨️ Testing input

### Sending keys

The lowest level method for simulating user input is the `sendKeys` method on the test terminal instance. (There is also
a `sendKey` method if you only want to send a single key).

The `sendKeys` method takes raw int values which represent the ASCII values of the keys that should be typed.

```kotlin
section {
    input()
}.runUntilInputEntered {
    // Send the ASCII values for "Hello, world!"
    terminal.sendKeys(
        72, 101, 108, 108, 111, 44, 32, 119, 111, 114, 108, 100, 33
    )
    terminal.sendKey(13) // ASCII code for the enter key
}

terminal.assertMatches {
    text("Hello, world! ") // "input" includes a trailing space for the cursor
}
```

> [!NOTE]
> We use `runUntilInputEntered` in the above case because otherwise the section might finish running and rendering
> before reading in all input, as handling input happens on a separate thread.

You will probably never use `sendKeys` directly yourself, as the other input methods are a bit more intuitive to use as
well as read (even if they just delegate to `sendKeys` under the hood).

### Sending control codes

Often, you want to send a control code, a special value which represents an arrow key or a delete operation. You can use
the `sendCode` method for this, which takes in one of the following values:

```kotlin
// Full path: Ansi.Csi.Codes.Sgr.Keys
object Keys {
    val Home: Code
    val Insert: Code
    val Delete: Code
    val End: Code
    val PgUp: Code
    val PgDown: Code

    val Up: Code
    val Down: Code
    val Left: Code
    val Right: Code
}
```

which you might use in a test like so:

```kotlin
section {
    /* ... */
}.runUntilSignal {
    onKeyPressed {
        if (key == Keys.Down) { signal() }
        /* ... other keys ... */
    }

    terminal.sendCode(Ansi.Csi.Codes.Keys.Down)
}
```

### Simulating typing

Finally, the most common input helper method is `type`, which takes in a string *or* a variable number of character
arguments and converts them to use `sendKeys` under the hood.

You can type ANSI control characters as well, which is a readable way to simulate the enter key. Bringing it all
together:

```kotlin
section {
    text("Hello, ")
    input()
}.runUntilInputEntered {
    terminal.type("world!")
    // alternately: terminal.type('w', 'o', 'r', 'l', 'd', '!')
    terminal.type(Ansi.CtrlChars.ENTER)
}
```

The full list of control characters are:

```kotlin
// Full path: Ansi.CtrlChars
object CtrlChars {
    const val EOF: Char
    const val BACKSPACE: Char
    const val TAB: Char
    const val ENTER: Char
    const val ESC: Char
    const val DELETE: Char
}
```

### Pressing keys

Perhaps the easiest way to simulate a key press is to use the convenience `terminal.press` method, which takes Kotter
`Key`s:

```kotlin
section { /* ... */ }.runUntilInputEntered {
    terminal.press(Keys.H, Keys.E, Keys.L)
    terminal.press(Keys.Right) // Autocomplete "hello"
    terminal.press(Keys.Enter)
}
```

This is probably the method most people will want to use for their tests -- there's no need to worry about typing vs
codes, or remembering if you should be using `Ansi.CtrlChars` or `Ansi.Csi.Codes.Sgr.Keys`.

> [!NOTE]
> Pressing `Key`s is technically an inverted approach, because Kotter `Key`s are really the final result of transforming
> raw ASCII values and sequence codes into a simple enum. They represent the terminating end of an input pipeline, in
> other words! However, as a mental model, most users of the Kotter library aren't don't really need to be aware of
> that.
>
> The `press` method, under the hood, actually figures out whether to call `type`, `sendCode`, or `sendKey` for you,
> based on the key you are pressing.

## ⏳ Testing timers

Real timers can be the bane of instant unit tests and the source of many a flaky test. As a result, test timers, which
allow you to pass time manually, are a common feature in testing libraries.

In a Kotter test, you can create a test timer calling the `data.useTestTimer` method inside a `run` block.

```kotlin
section {
   /* ... */ 
}.run {
    val timer = data.useTestTimer()
    timer.fastForward(10.minutes)
    /* ... */
}
```

> [!NOTE]
> Recall the `data` property comes from the Kotter Session. It's the very same property discussed here in
> the [Kotter documentation](https://github.com/varabyte/kotter#concurrentscopeddata).
>
> The `useTestTimer` method extends `data` because it registers itself into it as a side effect, bound to the
> lifecycle of the run block.

You should call this method as soon as possible, probably the very first line in your run block. If an actual timer is
triggered before you call `useTestTimer`, the call will result in a runtime exception.

In fact, because a `section` block render is kicked off instantly as soon as the run block starts, you are encouraged to
provide an early abort until the test timer is ready. This ensures that nothing in your section block will request a
timer without you realizing it. (Inputs and animations both do this, for example.)

```kotlin
var testTimerReady by liveVarOf(false)
section {
    if (!testTimerReady) return@section
    /* ... */
}.run {
    val timer = data.useTestTimer()
    testTimerReady = true

    timer.fastForward(10.minutes)
    /* ... */
}
```

It is pretty common to combine blocking methods and test timers together, as in the following example:

```kotlin
val spinningAnim = textAnimOf(listOf("⠸", "⠋", "⠙", "⠸", "⠴", "⠦"), Anim.ONE_FRAME_60FPS)

var testTimerReady by liveVarOf(false)
section {
    if (!testTimerReady) return@section
    text(spinningAnim)
    text(' ')
    text("Calculating...")
}.run {
    val timer = data.useTestTimer()
    testTimerReady = true

    timer.fastForward(Anim.ONE_FRAME_60FPS)
    blockUntilRenderMatches(terminal) {
        text("⠸ Calculating...")
    }

    timer.fastForward(Anim.ONE_FRAME_60FPS)
    blockUntilRenderMatches(terminal) {
        text("⠋ Calculating...")
    }

    /* ... etc. ... */
}
```

While in an actual test you would not likely need to test a Kotter animation (we've already done that extensively in
the official library!), it is nice to see that we can step the timer forward EXACTLY one frame at a time, which would be
impossible to do with a traditional system timer.

## Examples

### Using Kotter tests to learn from

Kotter itself leverages this library to test its own components. Feel
free [to browse its test sources](https://github.com/search?q=repo%3Avarabyte%2Fkotter+path%3A%2F%5Ekotter%5C%2Fsrc%5C%2FcommonTest%5C%2F%2F++testSession&type=code)
to see if you can find a pattern that you can apply to your own tests.

### A realistic scenario

Let's conclude this document with a reasonably realistic example.

Imagine we've created a widget that presents the users with a list of choices, and they can use the arrow keys plus
ENTER *or* press a number key to select an option.

The code for such a widget might look like this:

```kotlin
fun Session.promptChoices(message: String, choices: List<String>): String {
    var selectedIndex by liveVarOf(0)
    section {
        textLine(message)
        textLine()
        choices.forEachIndexed { index, choice ->
            if (index == selectedIndex) {
                text("> ")
            } else {
                text("  ")
            }
            textLine("${index + 1}) $choice")
        }
    }.runUntilSignal {
        onKeyPressed {
            when (key) {
                Keys.Up -> selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                Keys.Down -> selectedIndex = (selectedIndex + 1).coerceAtMost(choices.size - 1)
                Keys.Enter -> signal()
                Keys.Digit1, Keys.Digit2, Keys.Digit3, Keys.Digit4, Keys.Digit5, Keys.Digit6, Keys.Digit7, Keys.Digit8, Keys.Digit9 -> {
                    val digit = (key as CharKey).code.digitToInt()
                    val index = digit - 1
                    if (index < choices.size) {
                        selectedIndex = index
                        signal()
                    }
                }
            }
        }
    }
    return choices[selectedIndex]
}
```

Calling it would look like:

```kotlin
session {
    promptChoices(
        "Choose a color",
        listOf("Red", "Orange", "Yellow", "Green", "Blue", "Purple"),
    )
}

// Output:

// Choose a color
//
// > 1) Red
//   2) Orange
//   3) Yellow
//   4) Green
//   5) Blue
//   6) Purple
```

This works -- feel free to try it! But how do we test it?

The biggest problem in this case is we need to simulate user input. We don't want to do this until *after*
`onKeyPressed` is registered in the widget's run block.

For code like this, I can recommend two approaches:
* updating the signature to be testable
* breaking the widget down into pieces

#### Updating the signature to be testable

Let's add a callback which the user can use to respond to the widget being ready for user input, `onInputReady`:

```kotlin
internal fun Session.promptChoices(
    message: String,
    choices: List<String>,
    onInputReady: suspend () -> Unit, // New line
): String {
    var selectedIndex by liveVarOf(0)
    section {
        /* ... same as before ... */ 
    }.runUntilSignal {
        onKeyPressed {
            /* ... same as before ... */
        }
        onInputReady() // New line
    }
    return choices[selectedIndex]
}

fun Session.promptChoices(message: String, choices: List<String>) =
    promptChoices(message, choices, onInputReady = {})
```

> [!TIP]
> Above, we created an `internal` API for the test and a `public` API for the user.
> 
> Even though `onInputReady` would probably be a harmless event to expose to the user, it is still encouraged to hide
> it, in order to keep your APIs as minimal and simple as possible.

With this change, we are ready to test our widget:

```kotlin
@Test
fun `user can navigate to an answer using arrow keys`() {
    var answer = ""
    testSession { terminal ->
        answer = promptChoices(
            "Choose a color",
            listOf("Red", "Orange", "Yellow", "Green", "Blue", "Purple"),
            onInputReady = {
                terminal.press(Keys.Down)
                terminal.press(Keys.Down)
                terminal.press(Keys.Enter)

                // Or, if you prefer a one-liner:
                // press(Keys.Down, Keys.Down, Keys.Enter)
            }
        )
    }
    assertThat(answer).isEqualTo("Yellow")
}
```

#### Breaking the widget down into pieces

Another approach is to break the widget's render and run logic into separate methods:

```kotlin
internal fun MainRenderScope.renderChoices(
    message: String,
    choices: List<String>,
    selectedIndex: Int,
) {
    textLine(message)
    textLine()
    choices.forEachIndexed { index, choice ->
        if (index == selectedIndex) {
            text("> ")
        } else {
            text("  ")
        }
        textLine("${index + 1}) $choice")
    }
}

// This method fires `signal()` when the choice selection is confirmed.
// This should therefore be called within a `runUntilSignal` block.
internal fun RunScope.handleChoiceSelection(
    getSelectedIndex: () -> Int,
    maxIndex: Int,
    setSelectedIndex: (Int) -> Unit,
) {
    onKeyPressed {
        when (key) {
            Keys.Up -> setSelectedIndex((getSelectedIndex() - 1).coerceAtLeast(0))
            Keys.Down -> setSelectedIndex((getSelectedIndex() + 1).coerceAtMost(maxIndex - 1))
            Keys.Enter -> signal()
            Keys.Digit1, Keys.Digit2, Keys.Digit3, Keys.Digit4, Keys.Digit5, Keys.Digit6, Keys.Digit7, Keys.Digit8, Keys.Digit9 -> {
                val digit = (key as CharKey).code.digitToInt()
                val index = digit - 1
                if (index < maxIndex) {
                    setSelectedIndex(index)
                    signal()
                }
            }
        }
    }
}
```

At this point, the `promptChoices` method basically just delegates:

```kotlin
fun Session.promptChoices(message: String, choices: List<String>): String {
    var selectedIndex by liveVarOf(0)
    section {
        renderChoices(message, choices, selectedIndex)
    }.runUntilSignal {
        handleChoiceSelection(
            getSelectedIndex = { selectedIndex },
            maxIndex = choices.size,
            setSelectedIndex = { selectedIndex = it }
        )
    }
    return choices[selectedIndex]
}
```

And now, the test is straightforward, as we can just call the individual parts ourselves directly:

```kotlin
@Test
fun `user can navigate to an answer using arrow keys`() = testSession { terminal ->
    var selectedIndex by liveVarOf(0)
    val colorChoices = listOf("Red", "Orange", "Yellow", "Green", "Blue", "Purple")
    section {
        renderChoices("Choose a color", colorChoices, selectedIndex)
    }.runUntilSignal {
        handleChoiceSelection(
            getSelectedIndex = { selectedIndex },
            maxIndex = colorChoices.size,
            setSelectedIndex = { selectedIndex = it }
        )

        terminal.press(Keys.Down)
        terminal.press(Keys.Down)
        terminal.press(Keys.Enter)
    }

    assertThat(colorChoices[selectedIndex]).isEqualTo("Yellow")
}
```

Admittedly, this approach does feel a bit like you're duplicating the widget a little. It's also unfortunate that the
`handleChoicesSelection` method has to be called inside a `runUntilSignal` block, which can only be communicated by
documentation but not enforced by the code itself.

However, sometimes breaking Kotter logic up into smaller functions is the more natural way to organize the code anyway.
In that case, this sort of testing approach is a natural fit.

## 🏁 Conclusion

This document aimed to cover the main ways you can use the Kotter Test Support library to test your Kotter applications.

Please consider [raising a question](https://github.com/varabyte/kotter/discussions/categories/q-a) 
or [mentioning an idea](https://github.com/varabyte/kotter/discussions/categories/ideas) if you think there are ways
that this library or README could be improved and/or expanded upon.

Thank you!
