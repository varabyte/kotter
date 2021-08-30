package com.varabyte.konsole.runtime

import com.varabyte.konsole.runtime.internal.KonsoleCommand
import com.varabyte.konsole.runtime.internal.ansi.commands.RESET_COMMAND

/**
 * Keep track of all state related commands which should be reapplied to the current block if the ansi terminal resets
 * itself.
 *
 * Unfortunately, when you need to reset a single value (say, foreground color), the ANSI standard doesn't provide a
 * scalpel - instead, it provides a nuke (clear EVERYTHING). Since Konsole embraces a hierarchical, nested API, e.g.
 *
 * ```
 * white(BG) {
 *   red {
 *     underline {
 *       text("Red underlined text on white")
 *     }
 *     text("Red text on white")
 *   }
 * }
 * ```
 *
 * In order to support resetting just a subset of text styles, we need to maintain a copy of the state ourselves. In
 * order to, say, remove a foreground color setting, what we're really doing is nuking everything and building the whole
 * state back up again.
 */
class KonsoleState internal constructor(internal val parent: KonsoleState? = null) {
    internal var isDirty = false
        private set

    internal var fgColor: KonsoleCommand? = null
        set(value) {
            if (field != value) {
                field = value
                isDirty = true
            }
        }
    internal var bgColor: KonsoleCommand? = null
        set(value) {
            if (field != value) {
                field = value
                isDirty = true
            }
        }
    internal var underlined: KonsoleCommand? = null
        set(value) {
            if (field != value) {
                field = value
                isDirty = true
            }
        }
    internal var bolded: KonsoleCommand? = null
        set(value) {
            if (field != value) {
                field = value
                isDirty = true
            }
        }
    internal var struckThrough: KonsoleCommand? = null
        set(value) {
            if (field != value) {
                field = value
                isDirty = true
            }
        }
    internal var inverted: KonsoleCommand? = null
        set(value) {
            if (field != value) {
                field = value
                isDirty = true
            }
        }

    internal val fgColorRecursive: KonsoleCommand? get() = fgColor ?: parent?.fgColorRecursive
    internal val bgColorRecursive: KonsoleCommand? get() = bgColor ?: parent?.bgColorRecursive
    internal val underlinedRecursive: KonsoleCommand? get() = underlined ?: parent?.underlinedRecursive
    internal val boldedRecursive: KonsoleCommand? get() = bolded ?: parent?.boldedRecursive
    internal val struckThroughRecursive: KonsoleCommand? get() = struckThrough ?: parent?.struckThroughRecursive
    internal val invertedRecursive: KonsoleCommand? get() = inverted ?: parent?.invertedRecursive

    internal fun clear() {
        fgColor = null
        bgColor = null
        underlined = null
        bolded = null
        struckThrough = null
        inverted = null
    }

    internal fun applyTo(block: KonsoleBlock, force: Boolean = false) {
        if (isDirty || force) {
            block.appendCommand(RESET_COMMAND)
            (fgColorRecursive)?.let { block.appendCommand(it) }
            (bgColorRecursive)?.let { block.appendCommand(it) }
            (underlinedRecursive)?.let { block.appendCommand(it) }
            (boldedRecursive)?.let { block.appendCommand(it) }
            (struckThroughRecursive)?.let { block.appendCommand(it) }
            (invertedRecursive)?.let { block.appendCommand(it) }
        }

    }
}