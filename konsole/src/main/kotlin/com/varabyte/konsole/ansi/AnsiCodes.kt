package com.varabyte.konsole.ansi

/**
 * A collection of common ANSI codes which power the features of Konsole
 *
 * See also:
 * https://en.wikipedia.org/wiki/ANSI_escape_code
 * https://www.lihaoyi.com/post/BuildyourownCommandLinewithANSIescapecodes.html
 */
object AnsiCodes {
    // https://en.wikipedia.org/wiki/ANSI_escape_code#Control_characters
    object Ctrl {
        val BACKSPACE = '\u0008'
        val ESC = '\u001B'
    }

    // https://en.wikipedia.org/wiki/ANSI_escape_code#Fe_Escape_sequences
    object EscSeq {
        val CSI = '['
    }

    // https://en.wikipedia.org/wiki/ANSI_escape_code#CSI_(Control_Sequence_Introducer)_sequences
    // TODO: Cursor navigation

    // https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_(Select_Graphic_Rendition)_parameters
    object Sgr {
        sealed class SgrCode(val value: String) {
            fun toFullEscapeCode(): String = "${Ctrl.ESC}${EscSeq.CSI}${value}"
        }
        object RESET : SgrCode("0m")

        object Decorations {
            object BOLD : SgrCode("1m")
            object ITALIC : SgrCode("3m")
            object UNDERLINE : SgrCode("4m")
            object STRIKETHROUGH : SgrCode("9m")
        }

        object Colors {
            object INVERSE : SgrCode("7m")

            object Fg {
                object BLACK : SgrCode("30m")
                object RED : SgrCode("31m")
                object GREEN : SgrCode("32m")
                object YELLOW : SgrCode("33m")
                object BLUE : SgrCode("34m")
                object MAGENTA : SgrCode("35m")
                object CYAN : SgrCode("36m")
                object WHITE : SgrCode("37m")

                object BLACK_BRIGHT : SgrCode("30;1m")
                object RED_BRIGHT : SgrCode("31;1m")
                object GREEN_BRIGHT : SgrCode("32;1m")
                object YELLOW_BRIGHT : SgrCode("33;1m")
                object BLUE_BRIGHT : SgrCode("34;1m")
                object MAGENTA_BRIGHT : SgrCode("35;1m")
                object CYAN_BRIGHT : SgrCode("36;1m")
                object WHITE_BRIGHT : SgrCode("37;1m")
            }

            object Bg {
                object BLACK : SgrCode("40m")
                object RED : SgrCode("41m")
                object GREEN : SgrCode("42m")
                object YELLOW : SgrCode("43m")
                object BLUE : SgrCode("44m")
                object MAGENTA : SgrCode("45m")
                object CYAN : SgrCode("46m")
                object WHITE : SgrCode("47m")

                object BLACK_BRIGHT : SgrCode("40;1m")
                object RED_BRIGHT : SgrCode("41;1m")
                object GREEN_BRIGHT : SgrCode("42;1m")
                object YELLOW_BRIGHT : SgrCode("43;1m")
                object BLUE_BRIGHT : SgrCode("44;1m")
                object MAGENTA_BRIGHT : SgrCode("45;1m")
                object CYAN_BRIGHT : SgrCode("46;1m")
                object WHITE_BRIGHT : SgrCode("47;1m")
            }
        }
    }
}
