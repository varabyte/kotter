package com.varabyte.kotter.runtime.internal.ansi

import com.varabyte.kotter.runtime.internal.text.TextPtr
import com.varabyte.kotter.runtime.internal.text.readInt
import com.varabyte.kotter.runtime.internal.text.readUntil
import com.varabyte.kotter.runtime.internal.text.substring
import java.net.URI

/**
 * A collection of common ANSI codes and other related constants which power the features of
 * Kotter.
 *
 * See also:
 * https://en.wikipedia.org/wiki/ANSI_escape_code
 * https://www.lihaoyi.com/post/BuildyourownCommandLinewithANSIescapecodes.html
 */
object Ansi {
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
        const val OSC = ']'
        // Hack alert: For a reason I don't understand yet, Windows uses 'O' and not '[' for a handful of its escape
        // sequence characters. 'O' normally represents "function shift" but I'm not finding great documentation about
        // it. For now, it seems to work OK if we just treat 'O' like '[' on Windows sometimes.
        private const val CSI_ALT = 'O'

        internal fun toCsiCode(sequence: CharSequence): Csi.Code? {
            if (sequence.length < 3) return null
            if (sequence[0] != CtrlChars.ESC || (sequence[1] !in listOf(CSI, CSI_ALT))) return null
            val parts = Csi.Code.parts(TextPtr(sequence, 2)) ?: return null
            return Csi.Code(parts)
        }
    }

    // https://en.wikipedia.org/wiki/ANSI_escape_code#CSI_(Control_Sequence_Introducer)_sequences
    object Csi {
        /** The single byte identifier at the end of a code, e.g. 'm' in "ESC[31;1m" */
        class Identifier(val code: Char) {
            companion object {
                private val identifierObjects = mutableMapOf<Char, Identifier>()
                fun fromCode(code: Char): Identifier? = identifierObjects[code]
                fun fromCode(code: Code): Identifier? = fromCode(code.parts.identifier)
            }

            init {
                identifierObjects[code] = this
            }

            override fun toString() = code.toString()
        }

        object Identifiers {
            val CURSOR_UP = Identifier('A')
            val CURSOR_DOWN = Identifier('B')
            val CURSOR_RIGHT = Identifier('C')
            val CURSOR_LEFT = Identifier('D')
            val CURSOR_PREV_LINE = Identifier('F')
            val CURSOR_POSITION = Identifier('H')

            val ERASE_LINE = Identifier('K')
            val SGR = Identifier('m')
            val KEYCODE = Identifier('~')
        }

        /** The full code for this command, e.g. the "31;1m" part of "ESC[31;1m" */
        class Code(val parts: Parts) {
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
                override fun toString() = buildString {
                    if (numericCode != null) append(numericCode.toString())
                    if (optionalCodes != null) append(optionalCodes.joinToString(";"))
                    append(identifier)
                }
            }

            fun toFullEscapeCode(): String = "${CtrlChars.ESC}${EscSeq.CSI}${parts}"
            override fun equals(other: Any?): Boolean {
                return other is Code && other.parts == parts
            }

            override fun hashCode(): Int = parts.hashCode()

            override fun toString() = toFullEscapeCode()
        }

        object Codes {
            object Cursor {
                val MOVE_TO_PREV_LINE = Code("1${Identifiers.CURSOR_PREV_LINE}")
                val MOVE_TO_LINE_START = Code("${Identifiers.CURSOR_POSITION}")
                // Some terminals use "0 F" to mean to go the end of the current line??
                val MOVE_TO_LINE_END = Code("${Identifiers.CURSOR_PREV_LINE}")
            }

            object Erase {
                val CURSOR_TO_LINE_END = Code("0${Identifiers.ERASE_LINE}")
            }

            // https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_(Select_Graphic_Rendition)_parameters
            object Sgr {
                val RESET = Code("0${Identifiers.SGR}")

                object Decorations {
                    val BOLD = Code("1${Identifiers.SGR}")
                    val UNDERLINE = Code("4${Identifiers.SGR}")
                    val STRIKETHROUGH = Code("9${Identifiers.SGR}")

                    val CLEAR_BOLD = Code("22${Identifiers.SGR}")
                    val CLEAR_UNDERLINE = Code("24${Identifiers.SGR}")
                    val CLEAR_STRIKETHROUGH = Code("29${Identifiers.SGR}")
                }

                object Colors {
                    val INVERT = Code("7${Identifiers.SGR}")
                    val CLEAR_INVERT = Code("27${Identifiers.SGR}")

                    const val FG_NUMERIC = 38
                    const val BG_NUMERIC = 48
                    const val LOOKUP_SUBCODE = 5
                    const val TRUECOLOR_SUBCODE = 2

                    object Fg {
                        val BLACK = Code("30${Identifiers.SGR}")
                        val RED = Code("31${Identifiers.SGR}")
                        val GREEN = Code("32${Identifiers.SGR}")
                        val YELLOW = Code("33${Identifiers.SGR}")
                        val BLUE = Code("34${Identifiers.SGR}")
                        val MAGENTA = Code("35${Identifiers.SGR}")
                        val CYAN = Code("36${Identifiers.SGR}")
                        val WHITE = Code("37${Identifiers.SGR}")

                        val BLACK_BRIGHT = Code("90${Identifiers.SGR}")
                        val RED_BRIGHT = Code("91${Identifiers.SGR}")
                        val GREEN_BRIGHT = Code("92${Identifiers.SGR}")
                        val YELLOW_BRIGHT = Code("93${Identifiers.SGR}")
                        val BLUE_BRIGHT = Code("94${Identifiers.SGR}")
                        val MAGENTA_BRIGHT = Code("95${Identifiers.SGR}")
                        val CYAN_BRIGHT = Code("96${Identifiers.SGR}")
                        val WHITE_BRIGHT = Code("97${Identifiers.SGR}")

                        val CLEAR = Code("39${Identifiers.SGR}")

                        fun lookup(index: Int) = Code("$FG_NUMERIC;$LOOKUP_SUBCODE;$index${Identifiers.SGR}")
                        fun truecolor(r: Int, g: Int, b: Int) = Code("$FG_NUMERIC;$TRUECOLOR_SUBCODE;$r;$g;$b${Identifiers.SGR}")
                    }

                    object Bg {
                        val BLACK = Code("40${Identifiers.SGR}")
                        val RED = Code("41${Identifiers.SGR}")
                        val GREEN = Code("42${Identifiers.SGR}")
                        val YELLOW = Code("43${Identifiers.SGR}")
                        val BLUE = Code("44${Identifiers.SGR}")
                        val MAGENTA = Code("45${Identifiers.SGR}")
                        val CYAN = Code("46${Identifiers.SGR}")
                        val WHITE = Code("47${Identifiers.SGR}")

                        val BLACK_BRIGHT = Code("100${Identifiers.SGR}")
                        val RED_BRIGHT = Code("101${Identifiers.SGR}")
                        val GREEN_BRIGHT = Code("102${Identifiers.SGR}")
                        val YELLOW_BRIGHT = Code("103${Identifiers.SGR}")
                        val BLUE_BRIGHT = Code("104${Identifiers.SGR}")
                        val MAGENTA_BRIGHT = Code("105${Identifiers.SGR}")
                        val CYAN_BRIGHT = Code("106${Identifiers.SGR}")
                        val WHITE_BRIGHT = Code("107${Identifiers.SGR}")

                        val CLEAR = Code("49${Identifiers.SGR}")

                        fun lookup(index: Int) = Code("$BG_NUMERIC;$LOOKUP_SUBCODE;$index${Identifiers.SGR}")
                        fun truecolor(r: Int, g: Int, b: Int) = Code("$BG_NUMERIC;$TRUECOLOR_SUBCODE;$r;$g;$b${Identifiers.SGR}")
                    }
                }
            }

            object Keys {
                val HOME = Code("1${Identifiers.KEYCODE}")
                val INSERT = Code("2${Identifiers.KEYCODE}")
                val DELETE = Code("3${Identifiers.KEYCODE}")
                val END = Code("4${Identifiers.KEYCODE}")
                val PG_UP = Code("5${Identifiers.KEYCODE}")
                val PG_DOWN = Code("6${Identifiers.KEYCODE}")

                val UP = Code("${Identifiers.CURSOR_UP}")
                val DOWN = Code("${Identifiers.CURSOR_DOWN}")
                val LEFT = Code("${Identifiers.CURSOR_LEFT}")
                val RIGHT = Code("${Identifiers.CURSOR_RIGHT}")
            }
        }
    }

    // https://en.wikipedia.org/wiki/ANSI_escape_code#OSC_(Operating_System_Command)_sequences
    object Osc {
        /** The single numeric identifier for an OSC code, e.g. 8 in ESC]8;;" */
        class Identifier(val code: Int) {
            companion object {
                private val identifierObjects = mutableMapOf<Int, Identifier>()
                fun fromCode(code: Int): Identifier? = identifierObjects[code]
                fun fromCode(code: Code): Identifier? = fromCode(code.parts.numericCode)
            }

            init {
                identifierObjects[code] = this
            }

            override fun toString() = code.toString()
        }

        object Identifiers {
            val ANCHOR = Identifier(8)
        }

        /** The code for this OSC command, e.g. the "8;(params);(uri)ESC\" part of "ESC]8;(params);(uri)ESC\" */
        class Code(val parts: Parts) {
            constructor(value: String): this(
                parts(value) ?: throw IllegalArgumentException("Invalid OSC code: $value")
            )

            companion object {
                fun parts(text: CharSequence): Parts? {
                    return parts(TextPtr(text))
                }

                fun parts(textPtr: TextPtr): Parts? {
                    val numericCode = textPtr.readInt() ?: return null
                    val oscText = TextPtr(textPtr.readUntil { textPtr.substring(2) == "${CtrlChars.ESC}\\" })
                    val params = buildList {
                        while (oscText.currChar == ';') {
                            oscText.increment()
                            add(oscText.readUntil { currChar == ';' })
                        }
                    }

                    // Consume the trailing part of the OSC code, if present
                    if (textPtr.substring(2) == "${CtrlChars.ESC}\\") {
                        textPtr.increment()
                        // We leave the textPtr on the last part of the code, e.g. the \, as it's expected the caller
                        // will increment one more time on their end to move to the next part
                    }

                    return Parts(numericCode, params)
                }
            }

            data class Parts(val numericCode: Int, val params: List<String>) {
                override fun toString() = buildString {
                    append(numericCode); append(';')
                    append(params.joinToString(";"))
                }
            }

            fun toFullEscapeCode(): String = "${CtrlChars.ESC}${EscSeq.OSC}${parts}${CtrlChars.ESC}\\"
            override fun equals(other: Any?): Boolean {
                return other is Code && other.parts == parts
            }

            override fun hashCode(): Int = parts.hashCode()

            override fun toString() = toFullEscapeCode()
        }

        object Codes {
            fun openLink(uri: URI, params: Map<String, String> = emptyMap()) =
                Code("${Identifiers.ANCHOR};${params.map { (k, v) -> "$k=$v" }.joinToString(":")};${uri}")

            val CLOSE_LINK = Code("${Identifiers.ANCHOR};;")
        }
    }
}