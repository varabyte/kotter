package com.varabyte.kotter.runtime.terminal

/**
 * A class which provides text measurement utilities.
 */
class TextMetrics {
    /**
     * Return the length of this character when rendered in a terminal.
     *
     * The value will be 1 for many characters (like all English characters), could be 2 (for full-width characters like
     * Chinese characters), or could be 0 (for characters like combining characters / control characters).
     *
     * If you are calling this method on a string that may contain complex Unicode values, like emojis composed of
     * multiple characters, you are encouraged to use the other [renderWidthOf] method that acts on a text string.
     */
    fun renderWidthOf(c: Char): Int{
        return wcwidth(c.code).coerceAtLeast(0)
    }

    /**
     * Return the length of a String when rendered in a terminal.
     *
     * This is not as simple as just summing up the values of [renderWidthOf] with all characters, as some graphemes
     * may calculate their widths differently when surrounded by others for context.
     *
     * @param range An optional range you can use to restrict the calculation to a subset of the text, which can help
     * you avoid doing an unnecessary substring extraction in some cases.
     */
    fun renderWidthOf(str: CharSequence, range: IntRange = str.indices): Int {
        var totalWidth = 0
        var i = range.first
        val endInclusive = range.last
        while (i <= endInclusive) {
            val graphemeLen = graphemeLengthAt(str, i)
            totalWidth += calculateClusterWidth(str, i, graphemeLen)
            i += graphemeLen
        }
        return totalWidth
    }

    /**
     * Given an index into a string, return how many underlying characters the grapheme at that index consists of.
     *
     * A grapheme represents a single character rendered onscreen, but in some cases may be composed of multiple
     * characters in a sequence (particularly, emojis).
     *
     * A vast majority of characters will just return 1 here, assuming they fit entirely inside a Unicode 16 value
     * (which Kotlin uses).
     */
    // See also: `charCountForGraphemeClusterLegacy` in jline
    fun graphemeLengthAt(str: CharSequence, index: Int): Int {
        val len = str.length
        if (index >= len) return 0

        val cp = codePointAt(str, index)
        var pos = index + charCount(cp)

        // Regional indicator pairs form a single grapheme cluster (flag)
        if (isRegionalIndicator(cp) && pos < len) {
            val nextCp = codePointAt(str, pos)
            if (isRegionalIndicator(nextCp)) {
                pos += charCount(nextCp)
            }
            return pos - index
        }

        // Consume grapheme cluster extensions
        while (pos < len) {
            val ncp = codePointAt(str, pos)
            if (ncp == 0x200D) { // Zero Width Joiner
                val zwjSize = charCount(ncp)
                if (pos + zwjSize < len) {
                    pos += zwjSize
                    pos += charCount(codePointAt(str, pos))
                } else break
            } else if (wcwidth(ncp) == 0 && ncp >= 0x20) {
                // Zero-width extending characters: combining marks,
                // variation selectors (FE0E/FE0F), skin tone modifiers,
                // tag characters, etc.
                pos += charCount(ncp)
            } else break
        }
        return pos - index
    }
}

/**
 * Convenience version of `renderWidthOf` using a form people are more used to for substrings.
 *
 * Note that, like [CharSequence.substring], [startIndex] is inclusive and [endIndex] is exclusive.
 */
fun TextMetrics.renderWidthOf(str: CharSequence, startIndex: Int, endIndex: Int): Int =
    renderWidthOf(str, startIndex until endIndex)

/**
 * Take characters from [text] up until they can no longer fit into a space of [maxWidth].
 *
 * @param ellipsis If provided AND if the text doesn't fit, then make sure the string returned ends with this.
 */
fun TextMetrics.truncateToWidth(text: CharSequence, maxWidth: Int, ellipsis: String? = null): String {
    fun truncate(text: CharSequence, maxWidth: Int): String {
        var currWidth = 0
        var currIndex = 0
        while (currIndex < text.length) {
            val graphemeLen = graphemeLengthAt(text, currIndex)
            if (graphemeLen <= 0) break
            val width = renderWidthOf(text, currIndex until (currIndex + graphemeLen))
            if (currWidth + width > maxWidth) break
            currWidth += width
            currIndex += graphemeLen
        }
        return text.substring(0, currIndex)
    }

    if (text.isEmpty()) return ""

    val totalWidth = renderWidthOf(text)
    if (totalWidth <= maxWidth) return text.toString()

    if (ellipsis == null) return truncate(text, maxWidth)
    val ellipsisWidth = renderWidthOf(ellipsis)
    val maxWidthMinusEllipsis = maxWidth - ellipsisWidth

    // Only the ellipses itself can fit (and maybe not even all of it)
    if (maxWidthMinusEllipsis <= 0) return truncate(ellipsis, maxWidth)

    return truncate(text, maxWidthMinusEllipsis) + ellipsis
}

// Much of the code below forked from org.jline.utils.WCWidth.
// See also: https://github.com/jline/jline3/blob/master/terminal/src/main/java/org/jline/utils/WCWidth.java
// I would have used it directly (it is a dependency of this project!) but this codebase is multiplatform so I needed a
// common version freed from the constraints of a JVM-only project.

// region Adapted from JLine

private class Interval(val first: Int, val last: Int)

/* The following two functions define the column width of an ISO 10646
 * character as follows:
 *
 *    - The null character (U+0000) has a column width of 0.
 *
 *    - Other C0/C1 control characters and DEL will lead to a return
 *      value of -1.
 *
 *    - Non-spacing and enclosing combining characters (general
 *      category code Mn or Me in the Unicode database) have a
 *      column width of 0.
 *
 *    - SOFT HYPHEN (U+00AD) has a column width of 1.
 *
 *    - Other format characters (general category code Cf in the Unicode
 *      database) and ZERO WIDTH SPACE (U+200B) have a column width of 0.
 *
 *    - Hangul Jamo medial vowels and final consonants (U+1160-U+11FF)
 *      have a column width of 0.
 *
 *    - Spacing characters in the East Asian Wide (W) or East Asian
 *      Full-width (F) category as defined in Unicode Technical
 *      Report #11 have a column width of 2.
 *
 *    - All remaining characters (including all printable
 *      ISO 8859-1 and WGL4 characters, Unicode control characters,
 *      etc.) have a column width of 1.
 *
 * This implementation assumes that wchar_t characters are encoded
 * in ISO 10646.
 */
internal fun wcwidth(ucs: Int): Int {
    /* test for 8-bit control characters */
    if (ucs == 0) return 0
    if (ucs < 32 || ucs in 0x7f..0xa0) return -1

    return if (bisearch(ucs, combining)) {
        0
    } else if (bisearch(ucs, wide)) {
        2
    } else {
        /* if we arrive here, ucs is not a combining or C0/C1 control character */
        1
    }


}

/* Sorted list of non-overlapping intervals of non-spacing characters.
     * Generated from Unicode 16.0 UnicodeData.txt:
     *   categories Mn (Nonspacing Mark) + Me (Enclosing Mark) + Cf (Format)
     *   minus U+00AD (Soft Hyphen)
     *   plus U+1160-11FF (Hangul Jamo medial vowels and final consonants)
     *   plus U+200B (Zero Width Space)
     *   plus U+1F3FB-1F3FF (Emoji skin tone modifiers)
     * 2367 codepoints in 369 intervals */
private var combining: Array<Interval> = arrayOf(
    Interval(0x0300, 0x036F),
    Interval(0x0483, 0x0489),
    Interval(0x0591, 0x05BD),
    Interval(0x05BF, 0x05BF),
    Interval(0x05C1, 0x05C2),
    Interval(0x05C4, 0x05C5),
    Interval(0x05C7, 0x05C7),
    Interval(0x0600, 0x0605),
    Interval(0x0610, 0x061A),
    Interval(0x061C, 0x061C),
    Interval(0x064B, 0x065F),
    Interval(0x0670, 0x0670),
    Interval(0x06D6, 0x06DD),
    Interval(0x06DF, 0x06E4),
    Interval(0x06E7, 0x06E8),
    Interval(0x06EA, 0x06ED),
    Interval(0x070F, 0x070F),
    Interval(0x0711, 0x0711),
    Interval(0x0730, 0x074A),
    Interval(0x07A6, 0x07B0),
    Interval(0x07EB, 0x07F3),
    Interval(0x07FD, 0x07FD),
    Interval(0x0816, 0x0819),
    Interval(0x081B, 0x0823),
    Interval(0x0825, 0x0827),
    Interval(0x0829, 0x082D),
    Interval(0x0859, 0x085B),
    Interval(0x0890, 0x0891),
    Interval(0x0897, 0x089F),
    Interval(0x08CA, 0x0902),
    Interval(0x093A, 0x093A),
    Interval(0x093C, 0x093C),
    Interval(0x0941, 0x0948),
    Interval(0x094D, 0x094D),
    Interval(0x0951, 0x0957),
    Interval(0x0962, 0x0963),
    Interval(0x0981, 0x0981),
    Interval(0x09BC, 0x09BC),
    Interval(0x09C1, 0x09C4),
    Interval(0x09CD, 0x09CD),
    Interval(0x09E2, 0x09E3),
    Interval(0x09FE, 0x09FE),
    Interval(0x0A01, 0x0A02),
    Interval(0x0A3C, 0x0A3C),
    Interval(0x0A41, 0x0A42),
    Interval(0x0A47, 0x0A48),
    Interval(0x0A4B, 0x0A4D),
    Interval(0x0A51, 0x0A51),
    Interval(0x0A70, 0x0A71),
    Interval(0x0A75, 0x0A75),
    Interval(0x0A81, 0x0A82),
    Interval(0x0ABC, 0x0ABC),
    Interval(0x0AC1, 0x0AC5),
    Interval(0x0AC7, 0x0AC8),
    Interval(0x0ACD, 0x0ACD),
    Interval(0x0AE2, 0x0AE3),
    Interval(0x0AFA, 0x0AFF),
    Interval(0x0B01, 0x0B01),
    Interval(0x0B3C, 0x0B3C),
    Interval(0x0B3F, 0x0B3F),
    Interval(0x0B41, 0x0B44),
    Interval(0x0B4D, 0x0B4D),
    Interval(0x0B55, 0x0B56),
    Interval(0x0B62, 0x0B63),
    Interval(0x0B82, 0x0B82),
    Interval(0x0BC0, 0x0BC0),
    Interval(0x0BCD, 0x0BCD),
    Interval(0x0C00, 0x0C00),
    Interval(0x0C04, 0x0C04),
    Interval(0x0C3C, 0x0C3C),
    Interval(0x0C3E, 0x0C40),
    Interval(0x0C46, 0x0C48),
    Interval(0x0C4A, 0x0C4D),
    Interval(0x0C55, 0x0C56),
    Interval(0x0C62, 0x0C63),
    Interval(0x0C81, 0x0C81),
    Interval(0x0CBC, 0x0CBC),
    Interval(0x0CBF, 0x0CBF),
    Interval(0x0CC6, 0x0CC6),
    Interval(0x0CCC, 0x0CCD),
    Interval(0x0CE2, 0x0CE3),
    Interval(0x0D00, 0x0D01),
    Interval(0x0D3B, 0x0D3C),
    Interval(0x0D41, 0x0D44),
    Interval(0x0D4D, 0x0D4D),
    Interval(0x0D62, 0x0D63),
    Interval(0x0D81, 0x0D81),
    Interval(0x0DCA, 0x0DCA),
    Interval(0x0DD2, 0x0DD4),
    Interval(0x0DD6, 0x0DD6),
    Interval(0x0E31, 0x0E31),
    Interval(0x0E34, 0x0E3A),
    Interval(0x0E47, 0x0E4E),
    Interval(0x0EB1, 0x0EB1),
    Interval(0x0EB4, 0x0EBC),
    Interval(0x0EC8, 0x0ECE),
    Interval(0x0F18, 0x0F19),
    Interval(0x0F35, 0x0F35),
    Interval(0x0F37, 0x0F37),
    Interval(0x0F39, 0x0F39),
    Interval(0x0F71, 0x0F7E),
    Interval(0x0F80, 0x0F84),
    Interval(0x0F86, 0x0F87),
    Interval(0x0F8D, 0x0F97),
    Interval(0x0F99, 0x0FBC),
    Interval(0x0FC6, 0x0FC6),
    Interval(0x102D, 0x1030),
    Interval(0x1032, 0x1037),
    Interval(0x1039, 0x103A),
    Interval(0x103D, 0x103E),
    Interval(0x1058, 0x1059),
    Interval(0x105E, 0x1060),
    Interval(0x1071, 0x1074),
    Interval(0x1082, 0x1082),
    Interval(0x1085, 0x1086),
    Interval(0x108D, 0x108D),
    Interval(0x109D, 0x109D),
    Interval(0x1160, 0x11FF),
    Interval(0x135D, 0x135F),
    Interval(0x1712, 0x1714),
    Interval(0x1732, 0x1733),
    Interval(0x1752, 0x1753),
    Interval(0x1772, 0x1773),
    Interval(0x17B4, 0x17B5),
    Interval(0x17B7, 0x17BD),
    Interval(0x17C6, 0x17C6),
    Interval(0x17C9, 0x17D3),
    Interval(0x17DD, 0x17DD),
    Interval(0x180B, 0x180F),
    Interval(0x1885, 0x1886),
    Interval(0x18A9, 0x18A9),
    Interval(0x1920, 0x1922),
    Interval(0x1927, 0x1928),
    Interval(0x1932, 0x1932),
    Interval(0x1939, 0x193B),
    Interval(0x1A17, 0x1A18),
    Interval(0x1A1B, 0x1A1B),
    Interval(0x1A56, 0x1A56),
    Interval(0x1A58, 0x1A5E),
    Interval(0x1A60, 0x1A60),
    Interval(0x1A62, 0x1A62),
    Interval(0x1A65, 0x1A6C),
    Interval(0x1A73, 0x1A7C),
    Interval(0x1A7F, 0x1A7F),
    Interval(0x1AB0, 0x1ACE),
    Interval(0x1B00, 0x1B03),
    Interval(0x1B34, 0x1B34),
    Interval(0x1B36, 0x1B3A),
    Interval(0x1B3C, 0x1B3C),
    Interval(0x1B42, 0x1B42),
    Interval(0x1B6B, 0x1B73),
    Interval(0x1B80, 0x1B81),
    Interval(0x1BA2, 0x1BA5),
    Interval(0x1BA8, 0x1BA9),
    Interval(0x1BAB, 0x1BAD),
    Interval(0x1BE6, 0x1BE6),
    Interval(0x1BE8, 0x1BE9),
    Interval(0x1BED, 0x1BED),
    Interval(0x1BEF, 0x1BF1),
    Interval(0x1C2C, 0x1C33),
    Interval(0x1C36, 0x1C37),
    Interval(0x1CD0, 0x1CD2),
    Interval(0x1CD4, 0x1CE0),
    Interval(0x1CE2, 0x1CE8),
    Interval(0x1CED, 0x1CED),
    Interval(0x1CF4, 0x1CF4),
    Interval(0x1CF8, 0x1CF9),
    Interval(0x1DC0, 0x1DFF),
    Interval(0x200B, 0x200F),
    Interval(0x202A, 0x202E),
    Interval(0x2060, 0x2064),
    Interval(0x2066, 0x206F),
    Interval(0x20D0, 0x20F0),
    Interval(0x2CEF, 0x2CF1),
    Interval(0x2D7F, 0x2D7F),
    Interval(0x2DE0, 0x2DFF),
    Interval(0x302A, 0x302D),
    Interval(0x3099, 0x309A),
    Interval(0xA66F, 0xA672),
    Interval(0xA674, 0xA67D),
    Interval(0xA69E, 0xA69F),
    Interval(0xA6F0, 0xA6F1),
    Interval(0xA802, 0xA802),
    Interval(0xA806, 0xA806),
    Interval(0xA80B, 0xA80B),
    Interval(0xA825, 0xA826),
    Interval(0xA82C, 0xA82C),
    Interval(0xA8C4, 0xA8C5),
    Interval(0xA8E0, 0xA8F1),
    Interval(0xA8FF, 0xA8FF),
    Interval(0xA926, 0xA92D),
    Interval(0xA947, 0xA951),
    Interval(0xA980, 0xA982),
    Interval(0xA9B3, 0xA9B3),
    Interval(0xA9B6, 0xA9B9),
    Interval(0xA9BC, 0xA9BD),
    Interval(0xA9E5, 0xA9E5),
    Interval(0xAA29, 0xAA2E),
    Interval(0xAA31, 0xAA32),
    Interval(0xAA35, 0xAA36),
    Interval(0xAA43, 0xAA43),
    Interval(0xAA4C, 0xAA4C),
    Interval(0xAA7C, 0xAA7C),
    Interval(0xAAB0, 0xAAB0),
    Interval(0xAAB2, 0xAAB4),
    Interval(0xAAB7, 0xAAB8),
    Interval(0xAABE, 0xAABF),
    Interval(0xAAC1, 0xAAC1),
    Interval(0xAAEC, 0xAAED),
    Interval(0xAAF6, 0xAAF6),
    Interval(0xABE5, 0xABE5),
    Interval(0xABE8, 0xABE8),
    Interval(0xABED, 0xABED),
    Interval(0xFB1E, 0xFB1E),
    Interval(0xFE00, 0xFE0F),
    Interval(0xFE20, 0xFE2F),
    Interval(0xFEFF, 0xFEFF),
    Interval(0xFFF9, 0xFFFB),
    Interval(0x101FD, 0x101FD),
    Interval(0x102E0, 0x102E0),
    Interval(0x10376, 0x1037A),
    Interval(0x10A01, 0x10A03),
    Interval(0x10A05, 0x10A06),
    Interval(0x10A0C, 0x10A0F),
    Interval(0x10A38, 0x10A3A),
    Interval(0x10A3F, 0x10A3F),
    Interval(0x10AE5, 0x10AE6),
    Interval(0x10D24, 0x10D27),
    Interval(0x10D69, 0x10D6D),
    Interval(0x10EAB, 0x10EAC),
    Interval(0x10EFC, 0x10EFF),
    Interval(0x10F46, 0x10F50),
    Interval(0x10F82, 0x10F85),
    Interval(0x11001, 0x11001),
    Interval(0x11038, 0x11046),
    Interval(0x11070, 0x11070),
    Interval(0x11073, 0x11074),
    Interval(0x1107F, 0x11081),
    Interval(0x110B3, 0x110B6),
    Interval(0x110B9, 0x110BA),
    Interval(0x110BD, 0x110BD),
    Interval(0x110C2, 0x110C2),
    Interval(0x110CD, 0x110CD),
    Interval(0x11100, 0x11102),
    Interval(0x11127, 0x1112B),
    Interval(0x1112D, 0x11134),
    Interval(0x11173, 0x11173),
    Interval(0x11180, 0x11181),
    Interval(0x111B6, 0x111BE),
    Interval(0x111C9, 0x111CC),
    Interval(0x111CF, 0x111CF),
    Interval(0x1122F, 0x11231),
    Interval(0x11234, 0x11234),
    Interval(0x11236, 0x11237),
    Interval(0x1123E, 0x1123E),
    Interval(0x11241, 0x11241),
    Interval(0x112DF, 0x112DF),
    Interval(0x112E3, 0x112EA),
    Interval(0x11300, 0x11301),
    Interval(0x1133B, 0x1133C),
    Interval(0x11340, 0x11340),
    Interval(0x11366, 0x1136C),
    Interval(0x11370, 0x11374),
    Interval(0x113BB, 0x113C0),
    Interval(0x113CE, 0x113CE),
    Interval(0x113D0, 0x113D0),
    Interval(0x113D2, 0x113D2),
    Interval(0x113E1, 0x113E2),
    Interval(0x11438, 0x1143F),
    Interval(0x11442, 0x11444),
    Interval(0x11446, 0x11446),
    Interval(0x1145E, 0x1145E),
    Interval(0x114B3, 0x114B8),
    Interval(0x114BA, 0x114BA),
    Interval(0x114BF, 0x114C0),
    Interval(0x114C2, 0x114C3),
    Interval(0x115B2, 0x115B5),
    Interval(0x115BC, 0x115BD),
    Interval(0x115BF, 0x115C0),
    Interval(0x115DC, 0x115DD),
    Interval(0x11633, 0x1163A),
    Interval(0x1163D, 0x1163D),
    Interval(0x1163F, 0x11640),
    Interval(0x116AB, 0x116AB),
    Interval(0x116AD, 0x116AD),
    Interval(0x116B0, 0x116B5),
    Interval(0x116B7, 0x116B7),
    Interval(0x1171D, 0x1171D),
    Interval(0x1171F, 0x1171F),
    Interval(0x11722, 0x11725),
    Interval(0x11727, 0x1172B),
    Interval(0x1182F, 0x11837),
    Interval(0x11839, 0x1183A),
    Interval(0x1193B, 0x1193C),
    Interval(0x1193E, 0x1193E),
    Interval(0x11943, 0x11943),
    Interval(0x119D4, 0x119D7),
    Interval(0x119DA, 0x119DB),
    Interval(0x119E0, 0x119E0),
    Interval(0x11A01, 0x11A0A),
    Interval(0x11A33, 0x11A38),
    Interval(0x11A3B, 0x11A3E),
    Interval(0x11A47, 0x11A47),
    Interval(0x11A51, 0x11A56),
    Interval(0x11A59, 0x11A5B),
    Interval(0x11A8A, 0x11A96),
    Interval(0x11A98, 0x11A99),
    Interval(0x11C30, 0x11C36),
    Interval(0x11C38, 0x11C3D),
    Interval(0x11C3F, 0x11C3F),
    Interval(0x11C92, 0x11CA7),
    Interval(0x11CAA, 0x11CB0),
    Interval(0x11CB2, 0x11CB3),
    Interval(0x11CB5, 0x11CB6),
    Interval(0x11D31, 0x11D36),
    Interval(0x11D3A, 0x11D3A),
    Interval(0x11D3C, 0x11D3D),
    Interval(0x11D3F, 0x11D45),
    Interval(0x11D47, 0x11D47),
    Interval(0x11D90, 0x11D91),
    Interval(0x11D95, 0x11D95),
    Interval(0x11D97, 0x11D97),
    Interval(0x11EF3, 0x11EF4),
    Interval(0x11F00, 0x11F01),
    Interval(0x11F36, 0x11F3A),
    Interval(0x11F40, 0x11F40),
    Interval(0x11F42, 0x11F42),
    Interval(0x11F5A, 0x11F5A),
    Interval(0x13430, 0x13440),
    Interval(0x13447, 0x13455),
    Interval(0x1611E, 0x16129),
    Interval(0x1612D, 0x1612F),
    Interval(0x16AF0, 0x16AF4),
    Interval(0x16B30, 0x16B36),
    Interval(0x16F4F, 0x16F4F),
    Interval(0x16F8F, 0x16F92),
    Interval(0x16FE4, 0x16FE4),
    Interval(0x1BC9D, 0x1BC9E),
    Interval(0x1BCA0, 0x1BCA3),
    Interval(0x1CF00, 0x1CF2D),
    Interval(0x1CF30, 0x1CF46),
    Interval(0x1D167, 0x1D169),
    Interval(0x1D173, 0x1D182),
    Interval(0x1D185, 0x1D18B),
    Interval(0x1D1AA, 0x1D1AD),
    Interval(0x1D242, 0x1D244),
    Interval(0x1DA00, 0x1DA36),
    Interval(0x1DA3B, 0x1DA6C),
    Interval(0x1DA75, 0x1DA75),
    Interval(0x1DA84, 0x1DA84),
    Interval(0x1DA9B, 0x1DA9F),
    Interval(0x1DAA1, 0x1DAAF),
    Interval(0x1E000, 0x1E006),
    Interval(0x1E008, 0x1E018),
    Interval(0x1E01B, 0x1E021),
    Interval(0x1E023, 0x1E024),
    Interval(0x1E026, 0x1E02A),
    Interval(0x1E08F, 0x1E08F),
    Interval(0x1E130, 0x1E136),
    Interval(0x1E2AE, 0x1E2AE),
    Interval(0x1E2EC, 0x1E2EF),
    Interval(0x1E4EC, 0x1E4EF),
    Interval(0x1E5EE, 0x1E5EF),
    Interval(0x1E8D0, 0x1E8D6),
    Interval(0x1E944, 0x1E94A),
    Interval(0x1F3FB, 0x1F3FF),
    Interval(0xE0001, 0xE0001),
    Interval(0xE0020, 0xE007F),
    Interval(0xE0100, 0xE01EF),
)

/* Sorted list of non-overlapping intervals of East Asian Wide (W) and
    * Fullwidth (F) characters. Generated from Unicode 16.0 EastAsianWidth.txt.
    * Used for binary search to determine width-2 characters.
*/
private val wide = arrayOf(
    Interval(0x1100, 0x115F),  /* Hangul Jamo */
    Interval(0x231A, 0x231B),  /* Watch, Hourglass */
    Interval(0x2329, 0x232A),  /* Angle brackets */
    Interval(0x23E9, 0x23EC),  /* Playback symbols */
    Interval(0x23F0, 0x23F0),  /* Alarm clock */
    Interval(0x23F3, 0x23F3),  /* Hourglass flowing */
    Interval(0x25FD, 0x25FE),  /* Medium small squares */
    Interval(0x2614, 0x2615),  /* Umbrella, Hot beverage */
    Interval(0x2630, 0x2637),  /* Trigrams */
    Interval(0x2648, 0x2653),  /* Zodiac signs */
    Interval(0x267F, 0x267F),  /* Wheelchair */
    Interval(0x268A, 0x268F),  /* Yijing mono/digrams */
    Interval(0x2693, 0x2693),  /* Anchor */
    Interval(0x26A1, 0x26A1),  /* High voltage */
    Interval(0x26AA, 0x26AB),  /* Circles */
    Interval(0x26BD, 0x26BE),  /* Soccer, Baseball */
    Interval(0x26C4, 0x26C5),  /* Snowman, Sun behind cloud */
    Interval(0x26CE, 0x26CE),  /* Ophiuchus */
    Interval(0x26D4, 0x26D4),  /* No entry */
    Interval(0x26EA, 0x26EA),  /* Church */
    Interval(0x26F2, 0x26F3),  /* Fountain, Golf */
    Interval(0x26F5, 0x26F5),  /* Sailboat */
    Interval(0x26FA, 0x26FA),  /* Tent */
    Interval(0x26FD, 0x26FD),  /* Fuel pump */
    Interval(0x2705, 0x2705),  /* Check mark */
    Interval(0x270A, 0x270B),  /* Raised fist, Raised hand */
    Interval(0x2728, 0x2728),  /* Sparkles */
    Interval(0x274C, 0x274C),  /* Cross mark */
    Interval(0x274E, 0x274E),  /* Cross mark (square) */
    Interval(0x2753, 0x2755),  /* Question/Exclamation marks */
    Interval(0x2757, 0x2757),  /* Exclamation mark */
    Interval(0x2795, 0x2797),  /* Plus, Minus, Division */
    Interval(0x27B0, 0x27B0),  /* Curly loop */
    Interval(0x27BF, 0x27BF),  /* Double curly loop */
    Interval(0x2B1B, 0x2B1C),  /* Black/White large squares */
    Interval(0x2B50, 0x2B50),  /* Star */
    Interval(0x2B55, 0x2B55),  /* Heavy circle */
    Interval(0x2E80, 0x303E),  /* CJK Radicals .. CJK Symbols (excl. U+303F) */
    Interval(0x3041, 0x33BF),  /* Hiragana .. CJK Compatibility */
    Interval(0x33C0, 0x33FF),  /* CJK Compatibility (cont) */
    Interval(0x3400, 0x4DFF),  /* CJK Unified Ideographs Extension A + Yijing Hexagrams */
    Interval(0x4E00, 0xA4CF),  /* CJK Unified Ideographs .. Yi */
    Interval(0xA960, 0xA97C),  /* Hangul Jamo Extended-A */
    Interval(0xAC00, 0xD7A3),  /* Hangul Syllables */
    Interval(0xF900, 0xFAFF),  /* CJK Compatibility Ideographs */
    Interval(0xFE10, 0xFE19),  /* Vertical forms */
    Interval(0xFE30, 0xFE6F),  /* CJK Compatibility Forms */
    Interval(0xFF00, 0xFF60),  /* Fullwidth Forms */
    Interval(0xFFE0, 0xFFE6),  /* Fullwidth Signs */
    Interval(0x16FE0, 0x16FF1),  /* Ideographic Symbols and Punctuation */
    Interval(0x17000, 0x187F7),  /* Tangut */
    Interval(0x18800, 0x18CD5),  /* Tangut Components */
    Interval(0x18CFF, 0x18D08),  /* Tangut Supplement */
    Interval(0x1AFF0, 0x1AFF3),  /* Kana Extended-B */
    Interval(0x1AFF5, 0x1AFFB),  /* Kana Extended-B (cont) */
    Interval(0x1AFFD, 0x1AFFE),  /* Kana Extended-B (cont) */
    Interval(0x1B000, 0x1B122),  /* Kana Supplement */
    Interval(0x1B132, 0x1B132),  /* Small Kana */
    Interval(0x1B150, 0x1B152),  /* Small Kana Extension */
    Interval(0x1B155, 0x1B155),  /* Small Kana (cont) */
    Interval(0x1B164, 0x1B167),  /* Small Kana Extension (cont) */
    Interval(0x1B170, 0x1B2FB),  /* Nushu */
    Interval(0x1D300, 0x1D356),  /* Tai Xuan Jing Symbols */
    Interval(0x1D360, 0x1D376),  /* Counting Rod Numerals */
    Interval(0x1F004, 0x1F004),  /* Mahjong Red Dragon */
    Interval(0x1F0CF, 0x1F0CF),  /* Playing Card Black Joker */
    Interval(0x1F100, 0x1F10A),  /* Enclosed Alphanumeric Supplement */
    Interval(0x1F110, 0x1F12D),  /* Enclosed Alphanumeric Supplement (cont) */
    Interval(0x1F130, 0x1F169),  /* Enclosed Alphanumeric Supplement (cont) */
    Interval(0x1F170, 0x1F1AC),  /* Enclosed Alphanumeric Supplement (cont) */
    Interval(0x1F1E6, 0x1F202),  /* Regional Indicators .. Enclosed Ideographic */
    Interval(0x1F210, 0x1F23B),  /* Enclosed Ideographic Supplement */
    Interval(0x1F240, 0x1F248),  /* Enclosed Ideographic Supplement (cont) */
    Interval(0x1F250, 0x1F251),  /* Enclosed Ideographic Supplement (cont) */
    Interval(0x1F260, 0x1F265),  /* Enclosed Ideographic Supplement (cont) */
    Interval(0x1F300, 0x1F320),  /* Miscellaneous Symbols and Pictographs */
    Interval(0x1F32D, 0x1F335),  /* Food and Drink */
    Interval(0x1F337, 0x1F37C),  /* Plants and Nature */
    Interval(0x1F37E, 0x1F393),  /* Drinks and Celebrations */
    Interval(0x1F3A0, 0x1F3CA),  /* Activities */
    Interval(0x1F3CF, 0x1F3D3),  /* Sports */
    Interval(0x1F3E0, 0x1F3F0),  /* Buildings */
    Interval(0x1F3F4, 0x1F3F4),  /* Black Flag */
    Interval(0x1F3F8, 0x1F43E),  /* Sports and Animals */
    Interval(0x1F440, 0x1F440),  /* Eyes */
    Interval(0x1F442, 0x1F4FC),  /* People and Objects */
    Interval(0x1F4FF, 0x1F53D),  /* Objects */
    Interval(0x1F54B, 0x1F54E),  /* Religious */
    Interval(0x1F550, 0x1F567),  /* Clock faces */
    Interval(0x1F57A, 0x1F57A),  /* Dancing */
    Interval(0x1F595, 0x1F596),  /* Gestures */
    Interval(0x1F5A4, 0x1F5A4),  /* Black Heart */
    Interval(0x1F5FB, 0x1F64F),  /* Places and People */
    Interval(0x1F680, 0x1F6C5),  /* Transport */
    Interval(0x1F6CC, 0x1F6CC),  /* Sleeping */
    Interval(0x1F6D0, 0x1F6D2),  /* Shopping */
    Interval(0x1F6D5, 0x1F6D7),  /* Places */
    Interval(0x1F6DC, 0x1F6DF),  /* Transport (cont) */
    Interval(0x1F6EB, 0x1F6EC),  /* Airplane */
    Interval(0x1F6F4, 0x1F6FC),  /* Transport */
    Interval(0x1F7E0, 0x1F7EB),  /* Colored circles/squares */
    Interval(0x1F7F0, 0x1F7F0),  /* Heavy equals sign */
    Interval(0x1F90C, 0x1F93A),  /* Gestures and Activities */
    Interval(0x1F93C, 0x1F945),  /* Sports */
    Interval(0x1F947, 0x1F9FF),  /* Awards, Objects, People */
    Interval(0x1FA00, 0x1FA53),  /* Chess symbols */
    Interval(0x1FA60, 0x1FA6D),  /* Xiangqi */
    Interval(0x1FA70, 0x1FA7C),  /* Symbols and Pictographs Extended-A */
    Interval(0x1FA80, 0x1FA89),  /* Symbols and Pictographs Extended-A (cont) */
    Interval(0x1FA8F, 0x1FAC6),  /* Symbols and Pictographs Extended-A (cont) */
    Interval(0x1FACE, 0x1FADC),  /* Symbols and Pictographs Extended-A (cont) */
    Interval(0x1FADF, 0x1FAE9),  /* Symbols and Pictographs Extended-A (cont) */
    Interval(0x1FAF0, 0x1FAF8),  /* Hand gestures */
    Interval(0x20000, 0x2FFFD),  /* CJK Unified Ideographs Extension B..F */
    Interval(0x30000, 0x3FFFD),  /* CJK Unified Ideographs Extension G..J */
)

/* auxiliary function for binary search in interval table */
private fun bisearch(ucs: Int, table: Array<Interval>): Boolean {
    @Suppress("NAME_SHADOWING") var max = table.size - 1
    var min = 0
    var mid: Int
    if (ucs < table[0].first || ucs > table[max].last) return false
    while (max >= min) {
        mid = (min + max) / 2
        if (ucs > table[mid].last) min = mid + 1 else if (ucs < table[mid].first) max = mid - 1 else return true
    }
    return false
}

/**
 * Scans the cluster for Variation Selectors that override width.
 */
private fun calculateClusterWidth(cs: CharSequence, index: Int, size: Int): Int {
    val cp = codePointAt(cs, index)
    val w = wcwidth(cp)

    var pos = index + charCount(cp)
    val end = index + size
    while (pos < end) {
        val ncp = codePointAt(cs, pos)
        if (ncp == 0xFE0F) return 2 // VS16: Emoji presentation
        if (ncp == 0xFE0E) return 1 // VS15: Text presentation
        pos += charCount(ncp)
    }
    return if (w < 0) 0 else w
}

// --- KMP Unicode Helpers ---

// See java.lang.Character
private const val MIN_SUPPLEMENTARY_CODE_POINT = 0x10000

// See java.lang.Character.codePointAt
private fun codePointAt(cs: CharSequence, index: Int): Int {
    val high = cs[index]
    if (high.isHighSurrogate() && index + 1 < cs.length) {
        val low = cs[index + 1]
        if (low.isLowSurrogate()) {
            return (high.code - Char.MIN_HIGH_SURROGATE.code shl 10) + (low.code - Char.MIN_LOW_SURROGATE.code) + MIN_SUPPLEMENTARY_CODE_POINT
        }
    }
    return high.code
}

// See java.lang.Character.charCount
private fun charCount(cp: Int): Int = if (cp >= MIN_SUPPLEMENTARY_CODE_POINT) 2 else 1

private fun isRegionalIndicator(cp: Int): Boolean = cp in 0x1F1E6..0x1F1FF

// endregion
