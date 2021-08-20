package com.varabyte.konsole.core

import com.varabyte.konsole.ansi.AnsiCodes
import com.varabyte.konsole.core.internal.MutableKonsoleTextArea

class KonsoleState(internal val parent: KonsoleState? = null) {
    internal var fgColor: KonsoleCommand? = null
    internal var bgColor: KonsoleCommand? = null
    internal var underlined: KonsoleCommand? = null
    internal var bolded: KonsoleCommand? = null
    internal var italicized: KonsoleCommand? = null
    internal var inversed: KonsoleCommand? = null

    private val fgColorPrev: KonsoleCommand?
        get() = parent?.fgColor ?: parent?.fgColorPrev
    private val bgColorPrev: KonsoleCommand?
        get() = parent?.bgColor ?: parent?.bgColorPrev
    private val underlinedPrev: KonsoleCommand?
        get() = parent?.underlined ?: parent?.underlinedPrev
    private val boldedPrev: KonsoleCommand?
        get() = parent?.bolded ?: parent?.boldedPrev
    private val italicizedPrev: KonsoleCommand?
        get() = parent?.italicized ?: parent?.italicizedPrev
    private val inversedPrev: KonsoleCommand?
        get() = parent?.inversed ?: parent?.inversedPrev

    fun undo(textArea: MutableKonsoleTextArea) {
        textArea.append(AnsiCodes.RESET)
    }
}