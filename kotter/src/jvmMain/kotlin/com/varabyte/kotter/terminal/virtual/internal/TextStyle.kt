package com.varabyte.kotter.terminal.virtual.internal

import java.awt.Color as AwtColor

internal sealed interface TextAttribute {
    fun applyInto(style: MutableTextStyle)
    class FgColor(val value: AwtColor) : TextAttribute {
        override fun applyInto(style: MutableTextStyle) { style.fgColor = value }
    }
    class BgColor(val value: AwtColor) : TextAttribute {
        override fun applyInto(style: MutableTextStyle) { style.bgColor = value }
    }
    class Bold(val value: Boolean) : TextAttribute {
        override fun applyInto(style: MutableTextStyle) { style.isBold = value }
    }
    class Invert(val value: Boolean) : TextAttribute {
        override fun applyInto(style: MutableTextStyle) { style.isInvert = value }
    }
    class Strikethrough(val value: Boolean) : TextAttribute {
        override fun applyInto(style: MutableTextStyle) { style.isStrikethrough = value }
    }
    class Underline(val value: Boolean) : TextAttribute {
        override fun applyInto(style: MutableTextStyle) { style.isUnderline = value }
    }
    object Clear : TextAttribute {
        override fun applyInto(style: MutableTextStyle) { style.clear() }
    }
}

internal interface TextStyle {
    val fgColor: AwtColor
    val bgColor: AwtColor
    val isBold: Boolean
    val isInvert: Boolean
    val isStrikethrough: Boolean
    val isUnderline: Boolean

    fun copy(
        fgColor: AwtColor? = null,
        bgColor: AwtColor? = null,
        isBold: Boolean? = null,
        isInvert: Boolean? = null,
        isStrikethrough: Boolean? = null,
        isUnderlined: Boolean? = null
    ): TextStyle
}

/** [TextStyle.fgColor] but affected by [TextStyle.isInvert] */
internal val TextStyle.activeFgColor get() = if (isInvert) bgColor else fgColor
/** [TextStyle.bgColor] but affected by [TextStyle.isInvert] */
internal val TextStyle.activeBgColor get() = if (isInvert) fgColor else bgColor

internal class MutableTextStyle(val defaultFgColor: AwtColor, val defaultBgColor: AwtColor) : TextStyle {
    override var fgColor: AwtColor = defaultFgColor
    override var bgColor: AwtColor = defaultBgColor
    override var isBold: Boolean = false
    override var isInvert: Boolean = false
    override var isStrikethrough: Boolean = false
    override var isUnderline: Boolean = false

    fun setFrom(other: TextStyle) {
        fgColor = other.fgColor
        bgColor = other.bgColor
        isBold = other.isBold
        isInvert = other.isInvert
        isStrikethrough = other.isStrikethrough
        isUnderline = other.isUnderline
    }

    override fun copy(
        fgColor: AwtColor?,
        bgColor: AwtColor?,
        isBold: Boolean?,
        isInvert: Boolean?,
        isStrikethrough: Boolean?,
        isUnderlined: Boolean?
    ): MutableTextStyle {
        val self = this
        return MutableTextStyle(defaultFgColor, defaultBgColor).apply {
            this.fgColor = fgColor ?: self.fgColor
            this.bgColor = bgColor ?: self.bgColor
            this.isBold = isBold ?: self.isBold
            this.isInvert = isInvert ?: self.isInvert
            this.isStrikethrough = isStrikethrough ?: self.isStrikethrough
            this.isUnderline = isUnderlined ?: self.isUnderline
        }
    }

    fun clear() = setFrom(MutableTextStyle(defaultFgColor, defaultBgColor))

    override fun equals(other: Any?): Boolean {
        return other is MutableTextStyle
                && defaultFgColor == other.defaultFgColor
                && defaultBgColor == other.defaultBgColor
                && fgColor == other.fgColor
                && bgColor == other.bgColor
                && isBold == other.isBold
                && isInvert == other.isInvert
                && isStrikethrough == other.isStrikethrough
                && isUnderline == other.isUnderline
    }

    override fun hashCode(): Int {
        return arrayOf(
            defaultFgColor,
            defaultBgColor,
            fgColor,
            bgColor,
            isBold,
            isInvert,
            isStrikethrough,
            isUnderline
        ).contentHashCode()
    }
}
