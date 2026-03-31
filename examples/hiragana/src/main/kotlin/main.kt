import com.varabyte.kotter.foundation.input.InputEdit
import com.varabyte.kotter.foundation.input.multilineInput
import com.varabyte.kotter.foundation.input.onInputChanged
import com.varabyte.kotter.foundation.input.onInputCursorChanged
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.textLine

// Small characters which can extend some base hiragana characters.
private val Yoon = setOf('ゃ', 'ゅ', 'ょ')
private val Sokuon = 'っ'

private val RomajiToHiraganaMap = mapOf(
    // Vowels
    "a" to "あ",
    "i" to "い",
    "u" to "う",
    "e" to "え",
    "o" to "お",

    // K-row + pause
    "kka" to "っか",
    "kki" to "っき",
    "kku" to "っく",
    "kke" to "っけ",
    "kko" to "っこ",

    // K-row
    "ka" to "か",
    "ki" to "き",
    "ku" to "く",
    "ke" to "け",
    "ko" to "こ",

    // S-row + pause
    "ssa" to "っさ",
    "sshi" to "っし",
    "ssu" to "っす",
    "sse" to "っせ",
    "sso" to "っそ",

    // S-row
    "sa" to "さ",
    "shi" to "し",
    "su" to "す",
    "se" to "せ",
    "so" to "そ",

    // T-row + pause
    "tta" to "った",
    "tchi" to "っち",
    "ttsu" to "っつ",
    "tte" to "って",
    "tto" to "っと",

    // T-row
    "ta" to "た",
    "chi" to "ち",
    "tsu" to "つ",
    "te" to "て",
    "to" to "と",

    // N-row
    "na" to "な",
    "ni" to "に",
    "nu" to "ぬ",
    "ne" to "ね",
    "no" to "の",

    // H-row
    "ha" to "は",
    "hi" to "ひ",
    "fu" to "ふ",
    "he" to "へ",
    "ho" to "ほ",

    // M-row
    "ma" to "ま",
    "mi" to "み",
    "mu" to "む",
    "me" to "め",
    "mo" to "も",

    // Y-row
    "ya" to "や",
    "yu" to "ゆ",
    "yo" to "よ",

    // R-row
    "ra" to "ら",
    "ri" to "り",
    "ru" to "る",
    "re" to "れ",
    "ro" to "ろ",

    // W-row
    "wa" to "わ",
    "wo" to "を",

    // N
    "nn" to "ん",

    // Dakuten (voiced) - G-row
    "ga" to "が",
    "gi" to "ぎ",
    "gu" to "ぐ",
    "ge" to "げ",
    "go" to "ご",

    // Dakuten - Z-row
    "za" to "ざ",
    "ji" to "じ",
    "zu" to "ず",
    "ze" to "ぜ",
    "zo" to "ぞ",

    // Dakuten - D-row
    "da" to "だ",
    "di" to "ぢ",
    "du" to "づ",
    "de" to "で",
    "do" to "ど",

    // Dakuten - B-row
    "ba" to "ば",
    "bi" to "び",
    "bu" to "ぶ",
    "be" to "べ",
    "bo" to "ぼ",

    // Handakuten - P-row + pause
    "ppa" to "っぱ",
    "ppi" to "っぴ",
    "ppu" to "っぷ",
    "ppe" to "っぺ",
    "ppo" to "っぽ",

    // Handakuten - P-row
    "pa" to "ぱ",
    "pi" to "ぴ",
    "pu" to "ぷ",
    "pe" to "ぺ",
    "po" to "ぽ",

    // Combo characters (Yōon)
    "kya" to "きゃ",
    "kyu" to "きゅ",
    "kyo" to "きょ",

    "sha" to "しゃ",
    "shu" to "しゅ",
    "sho" to "しょ",

    "cha" to "ちゃ",
    "chu" to "ちゅ",
    "cho" to "ちょ",

    "nya" to "にゃ",
    "nyu" to "にゅ",
    "nyo" to "にょ",

    "hya" to "ひゃ",
    "hyu" to "ひゅ",
    "hyo" to "ひょ",

    "mya" to "みゃ",
    "myu" to "みゅ",
    "myo" to "みょ",

    "rya" to "りゃ",
    "ryu" to "りゅ",
    "ryo" to "りょ",

    "gya" to "ぎゃ",
    "gyu" to "ぎゅ",
    "gyo" to "ぎょ",

    "ja" to "じゃ",
    "ju" to "じゅ",
    "jo" to "じょ",

    "bya" to "びゃ",
    "byu" to "びゅ",
    "byo" to "びょ",

    "pya" to "ぴゃ",
    "pyu" to "ぴゅ",
    "pyo" to "ぴょ",

    // Double combo characters (pause + Yōon)
    "kkya" to "っきゃ",
    "kkyu" to "っきゅ",
    "kkyo" to "っきょ",

    "ssha" to "っしゃ",
    "sshu" to "っしゅ",
    "ssho" to "っしょ",

    "ccha" to "っちゃ",
    "cchu" to "っちゅ",
    "ccho" to "っちょ",

    "ppya" to "っぴゃ",
    "ppyu" to "っぴゅ",
    "ppyo" to "っぴょ",

    // Misc romanizations
    "wi" to "ゐ",
    "we" to "ゑ"
)

val HiraganaToRomajiMap = RomajiToHiraganaMap.map { entry -> entry.value to entry.key }.toMap()

private fun Char.isAlphaLetter() = this in 'a'..'z' || this in 'A'..'Z'

fun main() = session {
    section {
        textLine("English that you type will get converted to hiragana if possible.")
        textLine("For example, \"ha\" will be converted to \"は\".")
        textLine()
        textLine("Press ENTER for newlines, and CTRL-D to finish.")
        textLine()
    }.run()

    fun String.findUnconvertedText(index: Int): String {
        if (index !in this.indices) return ""

        val text = StringBuilder()

        var index = index
        while (index >= 0 && this[index].isAlphaLetter()) {
            text.insert(0, this[index])
            --index
        }

        return text.toString()
    }

    section {
        multilineInput()
    }.runUntilInputEntered {
        onInputCursorChanged {
            if (index !in input.indices) return@onInputCursorChanged
            val curr = input.getOrNull(index) ?: return@onInputCursorChanged

            if (curr in Yoon) {
                // Yoon is always the end of a combo character,
                // e.g. the ょ in きょ or っきょ
                --index
                cursorWidth++

                val prefix = input.getOrNull(index - 1)
                if (prefix == Sokuon) {
                    --index
                    cursorWidth++
                }
            } else if (curr == Sokuon) {
                // Sokuon is always the start of a combo character,
                // e.g. the っ in っき or っきょ
                cursorWidth++

                val suffix = input.getOrNull(index + 2)
                if (suffix in Yoon) {
                    cursorWidth++
                }
            } else { // normal hiragana character
                // Most often a solo character but might have a Sokuon prefix and/or a Yoon suffix
                val prefix = input.getOrNull(index - 1)
                val suffix = input.getOrNull(index + 1)
                if (prefix == Sokuon) {
                    --index
                    cursorWidth++
                }
                if (suffix in Yoon) {
                    cursorWidth++
                }
            }
        }

        onInputChanged {
            input = input.lowercase()

            when (edit) {
                is InputEdit.Added -> {
                    // Find all non-Japanese text before the current cursor and convert as much of it as possible. If
                    // the whole thing doesn't convert, try smaller and smaller checks, e.g. "xcvha" -> "xcvは"
                    val potentialMatchBuilder = StringBuilder(input.findUnconvertedText(index - 1))
                    while (potentialMatchBuilder.isNotEmpty()) {
                        val potentialMatch = potentialMatchBuilder.toString()
                        RomajiToHiraganaMap[potentialMatch]?.let { replacement ->
                            val replacedRomaji = StringBuilder(input)
                            input =
                                replacedRomaji.replace(index - (potentialMatch.length), index, replacement).toString()
                            index -= potentialMatch.length - replacement.length
                            break
                        }

                        potentialMatchBuilder.deleteCharAt(0)
                    }
                }
                is InputEdit.RemovedByBackspace -> {
                    // We might need to remove a compound word, in which case we need to delete more than a single
                    // character.
                    val valueToRemove = run {
                        val valueSoFar = StringBuilder(edit.value)
                        if (edit.value.singleOrNull() in Yoon) {
                            // Yoon is a combo suffix char, so index - 1 guaranteed to exist
                            valueSoFar.insert(0, input[index - 1])
                        }
                        val maybeSokuon = input.getOrNull(index - valueSoFar.length)?.takeIf { it == Sokuon }
                        if (maybeSokuon != null) valueSoFar.insert(0, Sokuon)

                        valueSoFar.toString()
                    }

                    HiraganaToRomajiMap[valueToRemove]?.let { romaji ->
                        // Although we "deleted" a Japanese character, we want to act like we only deleted one part of
                        // it, e.g. "は" + backspace == "h", not "" (if we treat "は" as "ha")
                        val romaji = romaji.dropLast(1)
                        index -= (valueToRemove.length - 1)
                        input = prevInput.replaceRange(index, index + valueToRemove.length, romaji)
                        index += romaji.length
                    }
                }
                is InputEdit.RemovedByDelete -> {
                    // Default behavior is fine
                }
            }
        }
    }
}
