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
}