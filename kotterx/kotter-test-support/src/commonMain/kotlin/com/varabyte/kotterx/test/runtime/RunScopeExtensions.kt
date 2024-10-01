package com.varabyte.kotterx.test.runtime

import com.varabyte.kotter.platform.concurrent.locks.*
import com.varabyte.kotter.runtime.*
import com.varabyte.kotter.runtime.render.*
import com.varabyte.kotter.runtime.terminal.mock.*
import com.varabyte.kotterx.test.terminal.*
import com.varabyte.kotterx.util.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Block the current run block until a render has occurred where the passed in [condition] is true.
 *
 * This can be useful to do because, due to the threading nature of Kotter (e.g. one thread gets notified, which
 * notifies another, which causes a render to happen on a third), occasionally it's not possible to predict if logic
 * will all execute in one pass or multiple passes. In that case, if you at least know that renders will eventually
 * settle on some consistent output, you can use this method to wait for that.
 *
 * A very useful pattern is to test the terminal's state:
 *
 * ```
 * testSession { terminal ->
 *   section { ... }.run {
 *     // do some stuff and then...
 *     blockUntilRenderWhen {
 *       terminal.resolveRerenders() == listOf(
 *         "expected line 1",
 *         "expected line 2",
 *         "",
 *       )
 *     }
 *   }
 * }
 * ```
 *
 * @param timeout A timeout to set for this block before it throws a [TimeoutCancellationException]. If null, this
 *   defaults to a 1 second. The thought here is that we're never waiting for some 10 second real-time animation to
 *   finish, but rather we're waiting for the Kotter engine to resolve its current event loop, which should barely take
 *   milliseconds. You can pass in [Duration.INFINITE] if you want to disable this.
 */
fun RunScope.blockUntilRenderWhen(timeout: Duration? = null, condition: () -> Boolean) {
    val latch = CompletableDeferred<Unit>()

    // Prevent the section from starting a render pass until we're sure we've registered our callback
    // Or, if a render is in progress, this will wait for it to finish first.
    data.lock.write {
        if (condition()) return

        section.onRendered {
            if (condition()) {
                removeListener = true
                latch.complete(Unit)
            }
        }
    }

    runBlocking {
        try {
            withTimeout(timeout ?: 1.seconds) { latch.await() }
        } catch (ex: TimeoutCancellationException) {
            // rethrow the timeout cancellation exception so that it doesn't get silently swallowed by coroutine
            // machinery.
            throw IllegalStateException(ex)
        }
    }
}

/**
 * Like [blockUntilRenderWhen] but using a more readable [RenderScope] syntax.
 *
 * For example:
 *
 * ```
 * testSession { terminal ->
 *   section { ... }.run {
 *     // do some stuff and then...
 *     blockUntilRenderMatches(terminal) {
 *       red { textLine("expected line 1") }
 *       green { textLine("expected line 2") }
 *     }
 *   }
 * }
 * ```
 *
 * This method will print out a helpful error message if the render doesn't match at the time of the timeout running
 * out.
 */
fun RunScope.blockUntilRenderMatches(
    terminal: MockTerminal,
    timeout: Duration? = null,
    expected: RenderScope.() -> Unit
) {
    val expectedOutput = consoleLinesFor(expected)
    try {
        blockUntilRenderWhen(timeout) {
            terminal.resolveRerenders() == expectedOutput
        }
    } catch (ex: TimeoutCancellationException) {
        // This will fail but at least it will give us an informative error message
        terminal.assertMatches { expected() }
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}
