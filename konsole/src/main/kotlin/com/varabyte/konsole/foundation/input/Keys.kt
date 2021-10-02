package com.varabyte.konsole.foundation.input

interface Key

/**
 * Marks a key as in the control key range (nothing to do specifically with the Ctrl key on the keyboard)
 *
 * See also [Character.isISOControl]
 */
interface IsoControlKey : Key

/**
 * Class for every key with a typeable value, e.g. 'a', '~', '7'
 *
 * Note that 'a' and 'A' are different CharKeys!
 */
data class CharKey(val code: Char) : Key

// TODO(Bug #22): Add some way to check if two keys are the same (perhaps given a keyboard layout?)
//  e.g. 'a' and 'A' are the same underlying key

object Keys {
    object ESC : IsoControlKey
    object ENTER : IsoControlKey

    object BACKSPACE : IsoControlKey
    object DELETE : IsoControlKey

    object EOF: IsoControlKey

    object UP : IsoControlKey
    object DOWN : IsoControlKey
    object LEFT : IsoControlKey
    object RIGHT : IsoControlKey

    object HOME : IsoControlKey
    object END : IsoControlKey
    object INSERT : IsoControlKey
    object PAGE_UP : IsoControlKey
    object PAGE_DOWN : IsoControlKey

    object TAB : IsoControlKey

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

    val A_UPPER = CharKey('A')
    val B_UPPER = CharKey('B')
    val C_UPPER = CharKey('C')
    val D_UPPER = CharKey('D')
    val E_UPPER = CharKey('E')
    val F_UPPER = CharKey('F')
    val G_UPPER = CharKey('G')
    val H_UPPER = CharKey('H')
    val I_UPPER = CharKey('I')
    val J_UPPER = CharKey('J')
    val K_UPPER = CharKey('K')
    val L_UPPER = CharKey('L')
    val M_UPPER = CharKey('M')
    val N_UPPER = CharKey('N')
    val O_UPPER = CharKey('O')
    val P_UPPER = CharKey('P')
    val Q_UPPER = CharKey('Q')
    val R_UPPER = CharKey('R')
    val S_UPPER = CharKey('S')
    val T_UPPER = CharKey('T')
    val U_UPPER = CharKey('U')
    val V_UPPER = CharKey('V')
    val W_UPPER = CharKey('W')
    val X_UPPER = CharKey('X')
    val Y_UPPER = CharKey('Y')
    val Z_UPPER = CharKey('Z')

    val SPACE = CharKey(' ')

    val TICK = CharKey('`')
    val TILDE = CharKey('~')

    val DIGIT_0 = CharKey('0')
    val DIGIT_1 = CharKey('1')
    val DIGIT_2 = CharKey('2')
    val DIGIT_3 = CharKey('3')
    val DIGIT_4 = CharKey('4')
    val DIGIT_5 = CharKey('5')
    val DIGIT_6 = CharKey('6')
    val DIGIT_7 = CharKey('7')
    val DIGIT_8 = CharKey('8')
    val DIGIT_9 = CharKey('9')

    val EXCLAMATION_MARK = CharKey('!')
    val AT = CharKey('@')
    val NUMBER_SIGN = CharKey('#')
    val POUND get() = NUMBER_SIGN
    val DOLLAR = CharKey('$')
    val PERCENT = CharKey('%')
    val CIRCUMFLEX = CharKey('^')
    val HAT get() = CIRCUMFLEX
    val AMPERSAND = CharKey('&')
    val ASTERISK = CharKey('*')
    val STAR get() = ASTERISK
    val LEFT_PARENS = CharKey('(')
    val RIGHT_PARENS = CharKey(')')
    val MINUS = CharKey('-')
    val UNDERSCORE = CharKey('_')
    val PLUS = CharKey('+')
    val EQUALS = CharKey('=')

    val LEFT_BRACE = CharKey('{')
    val RIGHT_BRACE = CharKey('}')
    val LEFT_BRACKET = CharKey('[')
    val RIGHT_BRACKET = CharKey(']')
    val BACKSLASH = CharKey('\\')
    val VERTICAL_BAR = CharKey('|')
    val SLASH = CharKey('/')
    val QUESTION_MARK = CharKey('?')
    val PIPE get() = VERTICAL_BAR
    val SEMICOLON = CharKey(';')
    val COLON = CharKey(':')
    val QUOTE = CharKey('\'')
    val DOUBLE_QUOTE = CharKey('"')
    val COMMA = CharKey(',')
    val PERIOD = CharKey('.')
    val GREATER = CharKey('>')
    val LESS = CharKey('<')
}