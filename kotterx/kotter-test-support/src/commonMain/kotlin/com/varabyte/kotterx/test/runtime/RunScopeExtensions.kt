package com.varabyte.kotterx.test.runtime

import com.varabyte.kotter.platform.concurrent.locks.*
import com.varabyte.kotter.runtime.*
import com.varabyte.kotter.runtime.render.*
import com.varabyte.kotter.runtime.terminal.*
import com.varabyte.kotterx.test.terminal.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking

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
 */
fun RunScope.blockUntilRenderWhen(condition: () -> Boolean) {
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

    runBlocking { latch.await() }
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
 *     blockUntilRenderWhenMatches {
 *       red { textLine("expected line 1") }
 *       green { textLine("expected line 2") }
 *     }
 *   }
 * }
 * ```
 *
 * It can be easier to use this version as a way to avoid writing ANSI codes directly into expected text values.
 */
fun RunScope.blockUntilRenderMatches(terminal: TestTerminal, expected: RenderScope.() -> Unit) {
    val expected = TestTerminal.consoleOutputFor(expected)
    blockUntilRenderWhen { terminal.resolveRerenders() == expected }
}
