package com.varabyte.konsole.core.input

interface Key

/**
 * Marks a key as in the control key range (nothing to do specifically with the Ctrl key on the keyboard)
 *
 * See also [Character.isISOControl]
 */
interface IsoControlKey : Key

/**
 * Class for every key with a typeable value, e.g. 'a', '~', '7'
 */
class CharKey(code: Char) : Key {
    val code = code.lowercaseChar()
    override fun equals(other: Any?): Boolean {
        return this === other || (other is CharKey && code == other.code)
    }
    override fun hashCode() = code.hashCode()
}

object Keys {
    object ESC : IsoControlKey
    object ENTER : IsoControlKey

    object BACKSPACE : IsoControlKey
    object DELETE : IsoControlKey

    object UP : IsoControlKey
    object DOWN : IsoControlKey
    object LEFT : IsoControlKey
    object RIGHT : IsoControlKey

    object HOME : IsoControlKey
    object END : IsoControlKey

    val A = CharKey('A')
    val B = CharKey('B')
    val C = CharKey('C')
    val D = CharKey('D')
    val E = CharKey('E')
    val F = CharKey('F')
    val G = CharKey('G')
    val H = CharKey('H')
    val I = CharKey('I')
    val J = CharKey('J')
    val K = CharKey('K')
    val L = CharKey('L')
    val M = CharKey('M')
    val N = CharKey('N')
    val O = CharKey('O')
    val P = CharKey('P')
    val Q = CharKey('Q')
    val R = CharKey('R')
    val S = CharKey('S')
    val T = CharKey('T')
    val U = CharKey('U')
    val V = CharKey('V')
    val W = CharKey('W')
    val X = CharKey('X')
    val Y = CharKey('Y')
    val Z = CharKey('Z')

    val SPACE = CharKey(' ')

}