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
    object Csi {
        abstract class Code(val value: String) {
            final fun toFullEscapeCode(): String = "${Ctrl.ESC}${EscSeq.CSI}${value}"
        }

        // https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_(Select_Graphic_Rendition)_parameters
        object Sgr {
            abstract class Code(value: String): Csi.Code(value)

            object RESET : Code("0m")

            object Decorations {
                object BOLD : Code("1m")
                object ITALIC : Code("3m")
                object UNDERLINE : Code("4m")
                object STRIKETHROUGH : Code("9m")
            }

            object Colors {
                object INVERSE : Code("7m")

                object Fg {
                    object BLACK : Code("30m")
                    object RED : Code("31m")
                    object GREEN : Code("32m")
                    object YELLOW : Code("33m")
                    object BLUE : Code("34m")
                    object MAGENTA : Code("35m")
                    object CYAN : Code("36m")
                    object WHITE : Code("37m")

                    object BLACK_BRIGHT : Code("30;1m")
                    object RED_BRIGHT : Code("31;1m")
                    object GREEN_BRIGHT : Code("32;1m")
                    object YELLOW_BRIGHT : Code("33;1m")
                    object BLUE_BRIGHT : Code("34;1m")
                    object MAGENTA_BRIGHT : Code("35;1m")
                    object CYAN_BRIGHT : Code("36;1m")
                    object WHITE_BRIGHT : Code("37;1m")
                }

                object Bg {
                    object BLACK : Code("40m")
                    object RED : Code("41m")
                    object GREEN : Code("42m")
                    object YELLOW : Code("43m")
                    object BLUE : Code("44m")
                    object MAGENTA : Code("45m")
                    object CYAN : Code("46m")
                    object WHITE : Code("47m")

                    object BLACK_BRIGHT : Code("40;1m")
                    object RED_BRIGHT : Code("41;1m")
                    object GREEN_BRIGHT : Code("42;1m")
                    object YELLOW_BRIGHT : Code("43;1m")
                    object BLUE_BRIGHT : Code("44;1m")
                    object MAGENTA_BRIGHT : Code("45;1m")
                    object CYAN_BRIGHT : Code("46;1m")
                    object WHITE_BRIGHT : Code("47;1m")
                }
            }
        }
    }
}
