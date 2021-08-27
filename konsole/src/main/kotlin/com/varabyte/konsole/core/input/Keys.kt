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
data class CharKey(val code: Char) : Key
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

    val SPACE = CharKey(' ')
}