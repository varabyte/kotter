package com.varabyte.konsole.ansi

/**
 * A collection of common ANSI codes which power the features of Konsole
 *
 * See also:
 * https://en.wikipedia.org/wiki/ANSI_escape_code
 * https://www.lihaoyi.com/post/BuildyourownCommandLinewithANSIescapecodes.html
 */
object AnsiCodes {
    val BACKSPACE = "\u0008"
    val ESC = "\u001B["

    val RESET = "${ESC}0m"

    object Colors {
        val INVERSE = "${ESC}7m"

        object Fg {
            val BLACK = "${ESC}30m"
            val RED = "${ESC}31m"
            val GREEN = "${ESC}32m"
            val YELLOW = "${ESC}33m"
            val BLUE = "${ESC}34m"
            val MAGENTA = "${ESC}35m"
            val CYAN = "${ESC}36m"
            val WHITE = "${ESC}37m"

            val BLACK_BRIGHT = "${ESC}30;1m"
            val RED_BRIGHT = "${ESC}31;1m"
            val GREEN_BRIGHT = "${ESC}32;1m"
            val YELLOW_BRIGHT = "${ESC}33;1m"
            val BLUE_BRIGHT = "${ESC}34;1m"
            val MAGENTA_BRIGHT = "${ESC}35;1m"
            val CYAN_BRIGHT = "${ESC}36;1m"
            val WHITE_BRIGHT = "${ESC}37;1m"
        }

        object Bg {
            val BLACK = "${ESC}40m"
            val RED = "${ESC}41m"
            val GREEN = "${ESC}42m"
            val YELLOW = "${ESC}43m"
            val BLUE = "${ESC}44m"
            val MAGENTA = "${ESC}45m"
            val CYAN = "${ESC}46m"
            val WHITE = "${ESC}47m"

            val BLACK_BRIGHT = "${ESC}40;1m"
            val RED_BRIGHT = "${ESC}41;1m"
            val GREEN_BRIGHT = "${ESC}42;1m"
            val YELLOW_BRIGHT = "${ESC}43;1m"
            val BLUE_BRIGHT = "${ESC}44;1m"
            val MAGENTA_BRIGHT = "${ESC}45;1m"
            val CYAN_BRIGHT = "${ESC}46;1m"
            val WHITE_BRIGHT = "${ESC}47;1m"
        }
    }

    object Decorations {
        val BOLD = "${ESC}1m"
        val ITALIC = "${ESC}3m"
        val UNDERLINE = "${ESC}4m"
        val STRIKETHROUGH = "${ESC}9m"
    }
}
