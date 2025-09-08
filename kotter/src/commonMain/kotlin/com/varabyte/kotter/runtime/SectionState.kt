package com.varabyte.kotter.runtime

import com.varabyte.kotter.runtime.internal.*
import com.varabyte.kotter.runtime.internal.ansi.commands.*
import com.varabyte.kotter.runtime.render.*

/**
 * Keep track of all text states applied so far against the current section.
 *
 * For example:
 *
 * ```
 * white(BG) {                                // record white BG
 *   red {                                    // record red FG
 *     underline {                            // record underline
 *       text("Red underlined text on white")
 *     }                                      // clear underline
 *     text("Red text on white")
 *   }                                        // clear read FG
 * }                                          // clear white BG
 * ```
 *
 * As Kotter is hierarchical in nature, we accomplish this by maintaining a stack of states (where the top of the
 * stack is discarded when a section scope is closed).
 */
internal class SectionState(val parent: SectionState? = null) {
    /**
     * A collection of relevant ANSI styles.
     *
     * @property parentStyles If provided, it means this style should fall back to its parent's value when unset.
     */
    internal class Styles(val parentStyles: Styles? = null) {
        var fgColor: TerminalCommand? = parentStyles?.fgColor
        var bgColor: TerminalCommand? = parentStyles?.bgColor
        var underlined: TerminalCommand? = parentStyles?.underlined
        var bolded: TerminalCommand? = parentStyles?.bolded
        var struckThrough: TerminalCommand? = parentStyles?.struckThrough
        var inverted: TerminalCommand? = parentStyles?.inverted
    }

    /** Styles which are actively applied, and any text rendered right now would use them. */
    val applied: Styles = parent?.applied ?: Styles()

    /**
     * The current style based on commands received so far in the current state scope.
     *
     * They are worth being deferred in case they change before new text is ultimately received.
     */
    val deferred: Styles = Styles(parent?.deferred)

    /**
     * Apply the current state into the target renderer.
     *
     * Any text rendered after will be using the styles maintained by this class instance.
     */
    fun applyTo(renderer: Renderer<*>) {
        if (deferred.fgColor?.text !== applied.fgColor?.text) {
            applied.fgColor = deferred.fgColor
            renderer.appendCommand(applied.fgColor ?: ColorCommands.Fg.Clear)
        }
        if (deferred.bgColor?.text !== applied.bgColor?.text) {
            applied.bgColor = deferred.bgColor
            renderer.appendCommand(applied.bgColor ?: ColorCommands.Bg.Clear)
        }
        if (deferred.underlined?.text !== applied.underlined?.text) {
            applied.underlined = deferred.underlined
            renderer.appendCommand(applied.underlined ?: DecorationCommands.ClearUnderline)
        }
        if (deferred.bolded?.text !== applied.bolded?.text) {
            applied.bolded = deferred.bolded
            renderer.appendCommand(applied.bolded ?: DecorationCommands.ClearBold)
        }
        if (deferred.struckThrough?.text !== applied.struckThrough?.text) {
            applied.struckThrough = deferred.struckThrough
            renderer.appendCommand(applied.struckThrough ?: DecorationCommands.ClearStrikethrough)
        }
        if (deferred.inverted?.text !== applied.inverted?.text) {
            applied.inverted = deferred.inverted
            renderer.appendCommand(applied.inverted ?: ColorCommands.ClearInvert)
        }
    }
}
