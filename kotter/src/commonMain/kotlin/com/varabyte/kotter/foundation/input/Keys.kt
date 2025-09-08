package com.varabyte.kotter.foundation.input

import com.varabyte.kotter.runtime.*

/**
 * Base class for all keypresses.
 *
 * See also: [Keys], [RunScope.onKeyPressed]
 */
interface Key

/**
 * Marks a key as one that triggers behavior (as opposed to being a simple typeable value).
 *
 * This has nothing to do specifically with the Ctrl key on the keyboard. For example, [Keys.ENTER], [Keys.BACKSPACE],
 * and [Keys.UP] are control keys.
 *
 * See also [Char.isISOControl]
 */
class IsoControlKey(private val name: String) : Key {
    override fun toString() = name
}

/**
 * Class for every key with a typeable value, e.g. 'a', '~', '7'
 *
 * This class is case-insensitive, meaning CharKey('q') will match CharKey('Q'). This allows us to have a DSL that
 * defaults to matching any instance of that key, e.g.:
 * ```
 * when (key) {
 *    // Handles 'q' and 'Q' both
 *    Keys.Q -> quit()
 * }
 * ```
 *
 * If you need case-specific handling, you can check the [char] value:
 * ```
 * when (key) {
 *    Keys.Q -> {
 *       if (key.char == 'Q') quit() else confirmThenQuit()
 *    }
 * }
 * ```
 */
class CharKey(val char: Char) : Key {
    @Deprecated("`code` has been renamed to `char`", ReplaceWith("char"))
    inline val code: Char get() = char

    override fun toString() = char.toString()

    fun isUpper() = char.isUpperCase()
    fun isLower() = char.isLowerCase()

    fun upper() = if (isUpper()) this else CharKey(char.uppercaseChar())
    fun lower() = if (isLower()) this else CharKey(char.lowercaseChar())

    override fun equals(other: Any?): Boolean {
        return other is CharKey && char.equals(other.char, ignoreCase = true)
    }

    override fun hashCode(): Int {
        return char.uppercaseChar().hashCode()
    }
}

private const val KEY_NAMING_CONVENTION_MESSAGE = "Name updated to reflect standard Kotlin naming conventions around singleton objects."
private const val UPPER_KEY_REMOVED_MESSAGE = "We are removing the `_UPPER` keys. The regular letter keys now match both lower and upper case key presses (i.e. `Keys.A` matches 'a' and 'A'). If you explicitly care about case-sensitive matching, use `isUpper()` or `isLower()`, e.g. `if (key == Keys.A_UPPER`) -> `if (key == Keys.A && key.isUpper())`"

// TODO(Bug #22): Add some way to check if two keys are the same (perhaps given a keyboard layout?)
//  e.g. 'a' and 'A' are the same underlying key
/** A collection of all keypresses supported by Kotter. */
object Keys {
    val Escape = IsoControlKey("Esc")
    val Enter = IsoControlKey("Enter")

    val Backspace = IsoControlKey("Backspace")
    val Delete = IsoControlKey("Delete")

    val Eof = IsoControlKey("EOF")

    val Up = IsoControlKey("Up")
    val Down = IsoControlKey("Down")
    val Left = IsoControlKey("Left")
    val Right = IsoControlKey("Right")

    val Home = IsoControlKey("Home")
    val End = IsoControlKey("End")
    val Insert = IsoControlKey("Insert")
    val PageUp = IsoControlKey("PgUp")
    val PageDown = IsoControlKey("PgDn")

    val Tab = IsoControlKey("Tab")

    val A = CharKey('a')
    val B = CharKey('b')
    val C = CharKey('c')
    val D = CharKey('d')
    val E = CharKey('e')
    val F = CharKey('f')
    val G = CharKey('g')
    val H = CharKey('h')
    val I = CharKey('i')
    val J = CharKey('j')
    val K = CharKey('k')
    val L = CharKey('l')
    val M = CharKey('m')
    val N = CharKey('n')
    val O = CharKey('o')
    val P = CharKey('p')
    val Q = CharKey('q')
    val R = CharKey('r')
    val S = CharKey('s')
    val T = CharKey('t')
    val U = CharKey('u')
    val V = CharKey('v')
    val W = CharKey('w')
    val X = CharKey('x')
    val Y = CharKey('y')
    val Z = CharKey('z')

    val Space = CharKey(' ')

    val Tick = CharKey('`')
    val Tilde = CharKey('~')

    val Digit0 = CharKey('0')
    val Digit1 = CharKey('1')
    val Digit2 = CharKey('2')
    val Digit3 = CharKey('3')
    val Digit4 = CharKey('4')
    val Digit5 = CharKey('5')
    val Digit6 = CharKey('6')
    val Digit7 = CharKey('7')
    val Digit8 = CharKey('8')
    val Digit9 = CharKey('9')

    val ExclamationMark = CharKey('!')
    val At = CharKey('@')
    val NumberSign = CharKey('#')
    val Pound get() = NumberSign
    val Dollar = CharKey('$')
    val Percent = CharKey('%')
    val Circumflex = CharKey('^')
    val Hat get() = Circumflex
    val Ampersand = CharKey('&')
    val Asterisk = CharKey('*')
    val Star get() = Asterisk
    val LeftParens = CharKey('(')
    val RightParens = CharKey(')')
    val Minus = CharKey('-')
    val Underscore = CharKey('_')
    val Plus = CharKey('+')
    val Equals = CharKey('=')

    val LeftBrace = CharKey('{')
    val RightBrace = CharKey('}')
    val LeftBracket = CharKey('[')
    val RightBracket = CharKey(']')
    val Backslash = CharKey('\\')
    val VerticalBar = CharKey('|')
    val Slash = CharKey('/')
    val QuestionMark = CharKey('?')
    val Pipe get() = VerticalBar
    val Semicolon = CharKey(';')
    val Colon = CharKey(':')
    val Quote = CharKey('\'')
    val DoubleQuote = CharKey('"')
    val Comma = CharKey(',')
    val Period = CharKey('.')
    val Greater = CharKey('>')
    val Less = CharKey('<')

    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Escape"))
    val ESC get() = Escape
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Enter"))
    val ENTER get() = Enter

    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Backspace"))
    val BACKSPACE get() = Backspace
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Delete"))
    val DELETE get() = Delete

    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Eof"))
    val EOF get() = Eof

    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Up"))
    val UP get() = Up
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Down"))
    val DOWN get() = Down
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Left"))
    val LEFT get() = Left
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Right"))
    val RIGHT get() = Right

    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Home"))
    val HOME get() = Home
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("End"))
    val END get() = End
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Insert"))
    val INSERT get() = Insert
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("PageUp"))
    val PAGE_UP get() = PageUp
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("PageDown"))
    val PAGE_DOWN get() = PageDown

    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Tab"))
    val TAB get() = Tab

    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Space"))
    val SPACE get() = Space

    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Tick"))
    val TICK get() = Tick
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Tilde"))
    val TILDE get() = Tilde

    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Digit0"))
    val DIGIT_0 get() = Digit0
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Digit1"))
    val DIGIT_1 get() = Digit1
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Digit2"))
    val DIGIT_2 get() = Digit2
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Digit3"))
    val DIGIT_3 get() = Digit3
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Digit4"))
    val DIGIT_4 get() = Digit4
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Digit5"))
    val DIGIT_5 get() = Digit5
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Digit6"))
    val DIGIT_6 get() = Digit6
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Digit7"))
    val DIGIT_7 get() = Digit7
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Digit8"))
    val DIGIT_8 get() = Digit8
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Digit9"))
    val DIGIT_9 get() = Digit9

    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("ExclamationMark"))
    val EXCLAMATION_MARK get() = ExclamationMark
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("At"))
    val AT get() = At
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("NumberSign"))
    val NUMBER_SIGN get() = NumberSign
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Pound"))
    val POUND get() = Pound
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Dollar"))
    val DOLLAR get() = Dollar
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Percent"))
    val PERCENT get() = Percent
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Circumflex"))
    val CIRCUMFLEX get() = Circumflex
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Hat"))
    val HAT get() = Hat
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Ampersand"))
    val AMPERSAND get() = Ampersand
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Asterisk"))
    val ASTERISK get() = Asterisk
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Star"))
    val STAR get() = Star
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("LeftParens"))
    val LEFT_PARENS get() = LeftParens
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("RightParens"))
    val RIGHT_PARENS get() = RightParens
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Minus"))
    val MINUS get() = Minus
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Underscore"))
    val UNDERSCORE get() = Underscore
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Plus"))
    val PLUS get() = Plus
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Equals"))
    val EQUALS get() = Equals

    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("LeftBrace"))
    val LEFT_BRACE get() = LeftBrace
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("RightBrace"))
    val RIGHT_BRACE get() = RightBrace
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("LeftBracket"))
    val LEFT_BRACKET get() = LeftBracket
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("RightBracket"))
    val RIGHT_BRACKET get() = RightBracket
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Backslash"))
    val BACKSLASH get() = Backslash
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("VerticalBar"))
    val VERTICAL_BAR get() = VerticalBar
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Slash"))
    val SLASH get() = Slash
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("QuestionMark"))
    val QUESTION_MARK get() = QuestionMark
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Pipe"))
    val PIPE get() = Pipe
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Semicolon"))
    val SEMICOLON get() = Semicolon
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Colon"))
    val COLON get() = Colon
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Quote"))
    val QUOTE get() = Quote
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Double_quote"))
    val DOUBLE_QUOTE get() = DoubleQuote
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Comma"))
    val COMMA get() = Comma
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Period"))
    val PERIOD get() = Period
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Greater"))
    val GREATER get() = Greater
    @Deprecated(KEY_NAMING_CONVENTION_MESSAGE, ReplaceWith("Less"))
    val LESS get() = Less

    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val A_UPPER get() = A.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val B_UPPER get() = B.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val C_UPPER get() = C.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val D_UPPER get() = D.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val E_UPPER get() = E.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val F_UPPER get() = F.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val G_UPPER get() = G.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val H_UPPER get() = H.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val I_UPPER get() = I.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val J_UPPER get() = J.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val K_UPPER get() = K.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val L_UPPER get() = L.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val M_UPPER get() = M.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val N_UPPER get() = N.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val O_UPPER get() = O.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val P_UPPER get() = P.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val Q_UPPER get() = Q.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val R_UPPER get() = R.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val S_UPPER get() = S.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val T_UPPER get() = T.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val U_UPPER get() = U.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val V_UPPER get() = V.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val W_UPPER get() = W.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val X_UPPER get() = X.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val Y_UPPER get() = Y.upper()
    @Deprecated(UPPER_KEY_REMOVED_MESSAGE)
    val Z_UPPER get() = Z.upper()
}
