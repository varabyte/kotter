package com.varabyte.konsole.runtime

import com.varabyte.konsole.runtime.internal.KonsoleCommand
import com.varabyte.konsole.runtime.internal.ansi.commands.*
import com.varabyte.konsole.runtime.internal.ansi.commands.BG_CLEAR_COMMAND
import com.varabyte.konsole.runtime.internal.ansi.commands.CLEAR_BOLD_COMMAND
import com.varabyte.konsole.runtime.internal.ansi.commands.CLEAR_UNDERLINE_COMMAND
import com.varabyte.konsole.runtime.internal.ansi.commands.FG_CLEAR_COMMAND

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
    internal var fgColor: KonsoleCommand? = parent?.fgColor
        set(value) { field = value ?: parent?.fgColor }
    internal var bgColor: KonsoleCommand? = parent?.bgColor
        set(value) { field = value ?: parent?.bgColor }
    internal var underlined: KonsoleCommand? = parent?.underlined
        set(value) { field = value ?: parent?.underlined }
    internal var bolded: KonsoleCommand? = parent?.bolded
        set(value) { field = value ?: parent?.bolded }
    internal var struckThrough: KonsoleCommand? = parent?.struckThrough
        set(value) { field = value ?: parent?.struckThrough }
    internal var inverted: KonsoleCommand? = parent?.inverted
        set(value) { field = value ?: parent?.inverted }

    internal val isFgColorSet get() = fgColor !== parent?.fgColor
    internal val isBgColorSet get() = bgColor !== parent?.bgColor
    internal val isUnderlinedSet get() = underlined !== parent?.underlined
    internal val isBoldedSet get() = bolded !== parent?.bolded
    internal val isStruckThroughSet get() = struckThrough !== parent?.struckThrough
    internal val isInvertedSet get() = inverted !== parent?.inverted

    /**
     * Given the current state of a block, issue the commands that would undo it.
     *
     * This is slightly tricky because you can't simply undo a value that's been set, but you need to restore a value
     * from a parent state if it's set.
     */
    internal fun undoOn(block: KonsoleBlock) {
        fgColor.takeIf { it != parent?.fgColor }?.let { block.appendCommand(parent?.fgColor ?: FG_CLEAR_COMMAND) }
        bgColor.takeIf { it != parent?.bgColor }?.let { block.appendCommand(parent?.bgColor ?: BG_CLEAR_COMMAND) }
        underlined.takeIf { it != parent?.underlined }?.let { block.appendCommand(parent?.underlined ?: CLEAR_UNDERLINE_COMMAND) }
        bolded.takeIf { it != parent?.bolded }?.let { block.appendCommand(parent?.bolded ?: CLEAR_BOLD_COMMAND) }
        struckThrough.takeIf { it != parent?.struckThrough }?.let { block.appendCommand(parent?.struckThrough ?: CLEAR_STRIKETHROUGH_COMMAND) }
        inverted.takeIf { it != parent?.inverted }?.let { block.appendCommand(parent?.inverted ?: CLEAR_INVERT_COMMAND) }
    }
}