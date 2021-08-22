package com.varabyte.konsole.core

import com.varabyte.konsole.text.RESET_COMMAND

/**
 * Keep track of all state related commands which should be reapplied to the current block if the ansi terminal resets
 * itself.
 *
 * Unfortunately, when you need to reset a single value (say, foreground color), the ANSI standard doesn't provide a
 * scapel - instead, it provides a nuke (clear EVERYTHING). Since Konsole embraces a hierarchical, nested API, e.g.
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
class KonsoleBlockState internal constructor(internal val parent: KonsoleBlockState? = null) {
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
    internal var italicized: KonsoleCommand? = null
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

    private val fgColorRecursive: KonsoleCommand? get() = fgColor ?: parent?.fgColorRecursive
    private val bgColorRecursive: KonsoleCommand? get() = bgColor ?: parent?.bgColorRecursive
    private val underlinedRecursive: KonsoleCommand? get() = underlined ?: parent?.underlinedRecursive
    private val boldedRecursive: KonsoleCommand? get() = bolded ?: parent?.boldedRecursive
    private val italicizedRecursive: KonsoleCommand? get() = italicized ?: parent?.italicizedRecursive
    private val struckThroughRecursive: KonsoleCommand? get() = struckThrough ?: parent?.struckThroughRecursive
    private val invertedRecursive: KonsoleCommand? get() = inverted ?: parent?.invertedRecursive

    internal fun clear() {
        fgColor = null
        bgColor = null
        underlined = null
        bolded = null
        italicized = null
        inverted = null
    }

    internal fun applyTo(block: KonsoleBlock, force: Boolean = false) {
        if (isDirty || force) {
            block.applyCommand(RESET_COMMAND)
            (fgColorRecursive)?.let { block.applyCommand(it) }
            (bgColorRecursive)?.let { block.applyCommand(it) }
            (underlinedRecursive)?.let { block.applyCommand(it) }
            (boldedRecursive)?.let { block.applyCommand(it) }
            (italicizedRecursive)?.let { block.applyCommand(it) }
            (struckThroughRecursive)?.let { block.applyCommand(it) }
            (invertedRecursive)?.let { block.applyCommand(it) }
        }

    }
}