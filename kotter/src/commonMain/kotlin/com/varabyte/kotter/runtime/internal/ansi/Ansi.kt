package com.varabyte.kotter.runtime.internal.ansi

import com.varabyte.kotter.platform.net.*
import com.varabyte.kotter.runtime.internal.text.*

/**
 * A collection of common ANSI codes and other related constants which power the features of
 * Kotter.
 *
 * See also:
 * * https://en.wikipedia.org/wiki/ANSI_escape_code
 * * https://www.lihaoyi.com/post/BuildyourownCommandLinewithANSIescapecodes.html
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
            val CursorUp = Identifier('A')
            val CursorDown = Identifier('B')
            val CursorRight = Identifier('C')
            val CursorLeft = Identifier('D')
            val CursorPrevLine = Identifier('F')
            val CursorPosition = Identifier('H')

            val EraseLine = Identifier('K')
            val Sgr = Identifier('m')
            val Keycode = Identifier('~')
        }

        /** The full code for this command, e.g. the "31;1m" part of "ESC[31;1m" */
        class Code(val parts: Parts) {
            constructor(value: String) : this(
                parts(value) ?: throw IllegalArgumentException("Invalid CSI code: $value")
            )

            companion object {
                fun parts(text: CharSequence): Parts? {
                    return parts(TextPtr(text))
                }

                fun parts(textPtr: TextPtr): Parts? {
                    val numericCode = textPtr.tryReadInt()
                    val optionalCodes = mutableListOf<Int>()
                    while (textPtr.currChar == ';') {
                        textPtr.increment()
                        optionalCodes.add(textPtr.tryReadInt() ?: break)
                    }
                    val identifier = textPtr.currChar.takeIf { !it.isISOControl() } ?: return null
                    return Parts(numericCode, optionalCodes.takeIf { it.isNotEmpty() }, identifier)
                }
            }

            data class Parts(val numericCode: Int?, val optionalCodes: List<Int>?, val identifier: Char) {
                override fun toString() = buildString {
                    if (numericCode != null) append(numericCode.toString())
                    if (optionalCodes != null) {
                        append(';'); append(optionalCodes.joinToString(";"))
                    }
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
                val MoveToPrevLine = Code("1${Identifiers.CursorPrevLine}")
                val MoveToLineStart = Code("${Identifiers.CursorPosition}")

                // Some terminals use "0 F" to mean to go the end of the current line??
                val MoveToLineEnd = Code("${Identifiers.CursorPrevLine}")
            }

            object Erase {
                val CursorToLineEnd = Code("0${Identifiers.EraseLine}")
            }

            // https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_(Select_Graphic_Rendition)_parameters
            object Sgr {
                val Reset = Code("0${Identifiers.Sgr}")

                object Decorations {
                    val Bold = Code("1${Identifiers.Sgr}")
                    val Underline = Code("4${Identifiers.Sgr}")
                    val Strikethrough = Code("9${Identifiers.Sgr}")

                    val ClearBold = Code("22${Identifiers.Sgr}")
                    val ClearUnderline = Code("24${Identifiers.Sgr}")
                    val ClearStrikethrough = Code("29${Identifiers.Sgr}")
                }

                object Colors {
                    val Invert = Code("7${Identifiers.Sgr}")
                    val ClearInvert = Code("27${Identifiers.Sgr}")

                    const val FG_NUMERIC = 38
                    const val BG_NUMERIC = 48
                    const val LOOKUP_SUBCODE = 5 // See also: https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit
                    const val TRUECOLOR_SUBCODE = 2 // See also: https://en.wikipedia.org/wiki/ANSI_escape_code#24-bit

                    object Fg {
                        val Black = Code("30${Identifiers.Sgr}")
                        val Red = Code("31${Identifiers.Sgr}")
                        val Green = Code("32${Identifiers.Sgr}")
                        val Yellow = Code("33${Identifiers.Sgr}")
                        val Blue = Code("34${Identifiers.Sgr}")
                        val Magenta = Code("35${Identifiers.Sgr}")
                        val Cyan = Code("36${Identifiers.Sgr}")
                        val White = Code("37${Identifiers.Sgr}")

                        val BrightBlack = Code("90${Identifiers.Sgr}")
                        val BrightRed = Code("91${Identifiers.Sgr}")
                        val BrightGreen = Code("92${Identifiers.Sgr}")
                        val BrightYellow = Code("93${Identifiers.Sgr}")
                        val BrightBlue = Code("94${Identifiers.Sgr}")
                        val BrightMagenta = Code("95${Identifiers.Sgr}")
                        val BrightCyan = Code("96${Identifiers.Sgr}")
                        val BrightWhite = Code("97${Identifiers.Sgr}")

                        val Clear = Code("39${Identifiers.Sgr}")

                        fun lookup(index: Int) = Code("$FG_NUMERIC;$LOOKUP_SUBCODE;$index${Identifiers.Sgr}")
                        fun truecolor(r: Int, g: Int, b: Int) =
                            Code("$FG_NUMERIC;$TRUECOLOR_SUBCODE;$r;$g;$b${Identifiers.Sgr}")
                    }

                    object Bg {
                        val Black = Code("40${Identifiers.Sgr}")
                        val Red = Code("41${Identifiers.Sgr}")
                        val Green = Code("42${Identifiers.Sgr}")
                        val Yellow = Code("43${Identifiers.Sgr}")
                        val Blue = Code("44${Identifiers.Sgr}")
                        val Magenta = Code("45${Identifiers.Sgr}")
                        val Cyan = Code("46${Identifiers.Sgr}")
                        val White = Code("47${Identifiers.Sgr}")

                        val BrightBlack = Code("100${Identifiers.Sgr}")
                        val BrightRed = Code("101${Identifiers.Sgr}")
                        val BrightGreen = Code("102${Identifiers.Sgr}")
                        val BrightYellow = Code("103${Identifiers.Sgr}")
                        val BrightBlue = Code("104${Identifiers.Sgr}")
                        val BrightMagenta = Code("105${Identifiers.Sgr}")
                        val BrightCyan = Code("106${Identifiers.Sgr}")
                        val BrightWhite = Code("107${Identifiers.Sgr}")

                        val Clear = Code("49${Identifiers.Sgr}")

                        fun lookup(index: Int) = Code("$BG_NUMERIC;$LOOKUP_SUBCODE;$index${Identifiers.Sgr}")
                        fun truecolor(r: Int, g: Int, b: Int) =
                            Code("$BG_NUMERIC;$TRUECOLOR_SUBCODE;$r;$g;$b${Identifiers.Sgr}")
                    }
                }
            }

            object Keys {
                val Home = Code("1${Identifiers.Keycode}")
                val Insert = Code("2${Identifiers.Keycode}")
                val Delete = Code("3${Identifiers.Keycode}")
                val End = Code("4${Identifiers.Keycode}")
                val PgUp = Code("5${Identifiers.Keycode}")
                val PgDown = Code("6${Identifiers.Keycode}")

                val Up = Code("${Identifiers.CursorUp}")
                val Down = Code("${Identifiers.CursorDown}")
                val Left = Code("${Identifiers.CursorLeft}")
                val Right = Code("${Identifiers.CursorRight}")
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
            val Anchor = Identifier(8)
        }

        /** The code for this OSC command, e.g. the "8;(params);(uri)ESC\" part of "ESC]8;(params);(uri)ESC\" */
        class Code(val parts: Parts) {
            constructor(value: String) : this(
                parts(value) ?: throw IllegalArgumentException("Invalid OSC code: $value")
            )

            companion object {
                fun parts(text: CharSequence): Parts? {
                    return parts(TextPtr(text))
                }

                fun parts(textPtr: TextPtr): Parts? {
                    val numericCode = textPtr.tryReadInt() ?: return null
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
            fun openLink(uri: Uri, params: Map<String, String> = emptyMap()) =
                Code("${Identifiers.Anchor};${params.map { (k, v) -> "$k=$v" }.joinToString(":")};${uri}")

            val CloseLink = Code("${Identifiers.Anchor};;")
        }
    }
}
