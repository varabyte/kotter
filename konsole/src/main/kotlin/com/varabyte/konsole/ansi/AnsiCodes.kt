package com.varabyte.konsole.ansi

import com.varabyte.konsole.ansi.AnsiCodes.ControlCharacters.ESC
import com.varabyte.konsole.ansi.AnsiCodes.EscapeSequences.CSI

/**
 * A collection of common ANSI codes which power the features of Konsole
 *
 * See also:
 * https://en.wikipedia.org/wiki/ANSI_escape_code
 * https://www.lihaoyi.com/post/BuildyourownCommandLinewithANSIescapecodes.html
 */
object AnsiCodes {
    // https://en.wikipedia.org/wiki/ANSI_escape_code#Control_characters
    object ControlCharacters {
        val BACKSPACE = '\u0008'
        val ESC = '\u001B'
    }

    // https://en.wikipedia.org/wiki/ANSI_escape_code#Fe_Escape_sequences
    object EscapeSequences {
        val CSI = "$ESC["
    }

    // https://en.wikipedia.org/wiki/ANSI_escape_code#CSI_(Control_Sequence_Introducer)_sequences
    object Csi {
        // TODO: Cursor navigation
    }

    // https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_(Select_Graphic_Rendition)_parameters
    object Sgr {
        val RESET = "${CSI}0m"

        object Decorations {
            val BOLD = "${CSI}1m"
            val ITALIC = "${CSI}3m"
            val UNDERLINE = "${CSI}4m"
            val STRIKETHROUGH = "${CSI}9m"
        }

        object Colors {
            val INVERSE = "${CSI}7m"

            object Fg {
                val BLACK = "${CSI}30m"
                val RED = "${CSI}31m"
                val GREEN = "${CSI}32m"
                val YELLOW = "${CSI}33m"
                val BLUE = "${CSI}34m"
                val MAGENTA = "${CSI}35m"
                val CYAN = "${CSI}36m"
                val WHITE = "${CSI}37m"

                val BLACK_BRIGHT = "${CSI}30;1m"
                val RED_BRIGHT = "${CSI}31;1m"
                val GREEN_BRIGHT = "${CSI}32;1m"
                val YELLOW_BRIGHT = "${CSI}33;1m"
                val BLUE_BRIGHT = "${CSI}34;1m"
                val MAGENTA_BRIGHT = "${CSI}35;1m"
                val CYAN_BRIGHT = "${CSI}36;1m"
                val WHITE_BRIGHT = "${CSI}37;1m"
            }

            object Bg {
                val BLACK = "${CSI}40m"
                val RED = "${CSI}41m"
                val GREEN = "${CSI}42m"
                val YELLOW = "${CSI}43m"
                val BLUE = "${CSI}44m"
                val MAGENTA = "${CSI}45m"
                val CYAN = "${CSI}46m"
                val WHITE = "${CSI}47m"

                val BLACK_BRIGHT = "${CSI}40;1m"
                val RED_BRIGHT = "${CSI}41;1m"
                val GREEN_BRIGHT = "${CSI}42;1m"
                val YELLOW_BRIGHT = "${CSI}43;1m"
                val BLUE_BRIGHT = "${CSI}44;1m"
                val MAGENTA_BRIGHT = "${CSI}45;1m"
                val CYAN_BRIGHT = "${CSI}46;1m"
                val WHITE_BRIGHT = "${CSI}47;1m"
            }
        }
    }
}
