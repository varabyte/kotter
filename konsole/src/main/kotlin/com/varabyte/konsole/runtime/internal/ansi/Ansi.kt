package com.varabyte.konsole.runtime.internal.ansi

import com.varabyte.konsole.runtime.internal.text.TextPtr
import com.varabyte.konsole.runtime.internal.text.readInt

/**
 * A collection of common ANSI codes and other related constants which power the features of
 * Konsole.
 *
 * See also:
 * https://en.wikipedia.org/wiki/ANSI_escape_code
 * https://www.lihaoyi.com/post/BuildyourownCommandLinewithANSIescapecodes.html
 */
internal object Ansi {
    // https://en.wikipedia.org/wiki/ANSI_escape_code#Control_characters
    object CtrlChars {
        const val EOF = '\u0004'
        const val BACKSPACE = '\u0008'
        const val TAB = '\u0009'
        const val ENTER = '\u000D'
        const val ESC = '\u001B'
        const val DELETE = '\u007F'
    }

    // https://en.wikipedia.org/wiki/ANSI_escape_code#Fe_Escape_sequences
    object EscSeq {
        const val CSI = '['
        // Hack alert: For a reason I don't understand yet, Windows uses 'O' and not '[' for a handful of its escape
        // sequence characters. 'O' normally represents "function shift" but I'm not finding great documentation about
        // it. For now, it seems to work OK if we just treat 'O' like '[' on Windows sometimes.
        private const val CSI_ALT = 'O'

        fun toCode(sequence: CharSequence): Csi.Code? {
            if (sequence.length < 3) return null
            if (sequence[0] != CtrlChars.ESC || (sequence[1] !in listOf(CSI, CSI_ALT))) return null
            val parts = Csi.Code.parts(TextPtr(sequence, 2)) ?: return null
            return Csi.Code(parts)
        }
    }

    // https://en.wikipedia.org/wiki/ANSI_escape_code#CSI_(Control_Sequence_Introducer)_sequences
    object Csi {
        /** The single byte identifier at the end of a code, e.g. 'm' in "ESC[31;1m" */
        abstract class Identifier(val code: Char) {
            companion object {
                private val identifierObjects = mutableMapOf<Char, Identifier>()
                fun fromCode(code: Char): Identifier? = identifierObjects[code]
                fun fromCode(code: Code): Identifier? = fromCode(code.parts.identifier)
            }

            init {
                @Suppress("LeakingThis") // We aren't using "this" in a dangerous way
                identifierObjects[code] = this
            }

            final override fun toString() = code.toString()
        }

        object Identifiers {
            object CURSOR_UP : Identifier('A')
            object CURSOR_DOWN : Identifier('B')
            object CURSOR_RIGHT : Identifier('C')
            object CURSOR_LEFT : Identifier('D')
            object CURSOR_PREV_LINE : Identifier('F')
            object CURSOR_POSITION : Identifier('H')

            object ERASE_LINE : Identifier('K')
            object SGR : Identifier('m')
            object KEYCODE : Identifier('~')
        }

        /** The full code for this command, e.g. the "31;1m" part of "ESC[31;1m" */
        open class Code(val parts: Parts) {
            constructor(value: String): this(
                parts(value) ?: throw IllegalArgumentException("Invalid CSI code: $value")
            )

            companion object {
                fun parts(text: CharSequence): Parts? {
                    return parts(TextPtr(text))
                }

                fun parts(textPtr: TextPtr): Parts? {
                    val numericCode = textPtr.readInt()
                    val optionalCodes = mutableListOf<Int>()
                    while (textPtr.currChar == ';') {
                        textPtr.increment()
                        optionalCodes.add(textPtr.readInt() ?: break)
                    }
                    val identifier = textPtr.currChar.takeIf { !it.isISOControl() } ?: return null
                    return Parts(numericCode, optionalCodes.takeIf { it.isNotEmpty() }, identifier)
                }
            }

            data class Parts(val numericCode: Int?, val optionalCodes: List<Int>?, val identifier: Char) {
                override fun toString() = "${if (numericCode != null) "$numericCode" else ""}${if (optionalCodes != null) ";${optionalCodes.joinToString(";")}" else ""}$identifier"
            }

            fun toFullEscapeCode(): String = "${CtrlChars.ESC}${EscSeq.CSI}${parts}"
            override fun equals(other: Any?): Boolean {
                return other is Code && other.parts == parts
            }

            override fun hashCode(): Int = parts.hashCode()
        }

        object Codes {
            object Cursor {
                object MOVE_TO_PREV_LINE : Code("1${Identifiers.CURSOR_PREV_LINE}")
                object MOVE_TO_LINE_START : Code("${Identifiers.CURSOR_POSITION}")
                // Some terminals use "0 F" to mean to go the end of the current line??
                object MOVE_TO_LINE_END : Code("${Identifiers.CURSOR_PREV_LINE}")
            }

            object Erase {
                object CURSOR_TO_LINE_END : Code("0${Identifiers.ERASE_LINE}")
            }

            // https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_(Select_Graphic_Rendition)_parameters
            object Sgr {
                object RESET : Code("0${Identifiers.SGR}")

                object Decorations {
                    object BOLD : Code("1${Identifiers.SGR}")
                    object UNDERLINE : Code("4${Identifiers.SGR}")
                    object STRIKETHROUGH : Code("9${Identifiers.SGR}")

                    object CLEAR_BOLD : Code("22${Identifiers.SGR}")
                    object CLEAR_UNDERLINE : Code("24${Identifiers.SGR}")
                    object CLEAR_STRIKETHROUGH : Code("29${Identifiers.SGR}")
                }

                object Colors {
                    object INVERT : Code("7${Identifiers.SGR}")
                    object CLEAR_INVERT : Code("27${Identifiers.SGR}")

                    const val FG_NUMERIC = 38
                    const val BG_NUMERIC = 48
                    const val LOOKUP_SUBCODE = 5
                    const val TRUECOLOR_SUBCODE = 2

                    object Fg {
                        object BLACK : Code("30${Identifiers.SGR}")
                        object RED : Code("31${Identifiers.SGR}")
                        object GREEN : Code("32${Identifiers.SGR}")
                        object YELLOW : Code("33${Identifiers.SGR}")
                        object BLUE : Code("34${Identifiers.SGR}")
                        object MAGENTA : Code("35${Identifiers.SGR}")
                        object CYAN : Code("36${Identifiers.SGR}")
                        object WHITE : Code("37${Identifiers.SGR}")

                        object BLACK_BRIGHT : Code("90${Identifiers.SGR}")
                        object RED_BRIGHT : Code("91${Identifiers.SGR}")
                        object GREEN_BRIGHT : Code("92${Identifiers.SGR}")
                        object YELLOW_BRIGHT : Code("93${Identifiers.SGR}")
                        object BLUE_BRIGHT : Code("94${Identifiers.SGR}")
                        object MAGENTA_BRIGHT : Code("95${Identifiers.SGR}")
                        object CYAN_BRIGHT : Code("96${Identifiers.SGR}")
                        object WHITE_BRIGHT : Code("97${Identifiers.SGR}")

                        object CLEAR : Code("39${Identifiers.SGR}")

                        fun lookup(index: Int) = Code("$FG_NUMERIC;$LOOKUP_SUBCODE;$index${Identifiers.SGR}")
                        fun truecolor(r: Int, g: Int, b: Int) = Code("$FG_NUMERIC;$TRUECOLOR_SUBCODE;$r;$g;$b${Identifiers.SGR}")
                    }

                    object Bg {
                        object BLACK : Code("40${Identifiers.SGR}")
                        object RED : Code("41${Identifiers.SGR}")
                        object GREEN : Code("42${Identifiers.SGR}")
                        object YELLOW : Code("43${Identifiers.SGR}")
                        object BLUE : Code("44${Identifiers.SGR}")
                        object MAGENTA : Code("45${Identifiers.SGR}")
                        object CYAN : Code("46${Identifiers.SGR}")
                        object WHITE : Code("47${Identifiers.SGR}")

                        object BLACK_BRIGHT : Code("100${Identifiers.SGR}")
                        object RED_BRIGHT : Code("101${Identifiers.SGR}")
                        object GREEN_BRIGHT : Code("102${Identifiers.SGR}")
                        object YELLOW_BRIGHT : Code("103${Identifiers.SGR}")
                        object BLUE_BRIGHT : Code("104${Identifiers.SGR}")
                        object MAGENTA_BRIGHT : Code("105${Identifiers.SGR}")
                        object CYAN_BRIGHT : Code("106${Identifiers.SGR}")
                        object WHITE_BRIGHT : Code("107${Identifiers.SGR}")

                        object CLEAR : Code("49${Identifiers.SGR}")

                        fun lookup(index: Int) = Code("$BG_NUMERIC;$LOOKUP_SUBCODE;$index${Identifiers.SGR}")
                        fun truecolor(r: Int, g: Int, b: Int) = Code("$BG_NUMERIC;$TRUECOLOR_SUBCODE;$r;$g;$b${Identifiers.SGR}")
                    }
                }
            }

            object Keys {
                object HOME : Code("1${Identifiers.KEYCODE}")
                object INSERT : Code("2${Identifiers.KEYCODE}")
                object DELETE : Code("3${Identifiers.KEYCODE}")
                object END : Code("4${Identifiers.KEYCODE}")
                object PG_UP : Code("5${Identifiers.KEYCODE}")
                object PG_DOWN : Code("6${Identifiers.KEYCODE}")

                object UP : Code("${Identifiers.CURSOR_UP}")
                object DOWN : Code("${Identifiers.CURSOR_DOWN}")
                object LEFT : Code("${Identifiers.CURSOR_LEFT}")
                object RIGHT : Code("${Identifiers.CURSOR_RIGHT}")
            }
        }
    }
}