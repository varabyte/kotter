import com.varabyte.kotter.foundation.text.clearInvert
import com.varabyte.kotter.foundation.text.invert
import com.varabyte.kotter.foundation.text.rgb
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.timer.addTimer
import com.varabyte.kotter.runtime.RunScope
import com.varabyte.kotter.runtime.Section
import com.varabyte.kotter.runtime.SectionScope
import com.varabyte.kotter.runtime.concurrent.createKey
import com.varabyte.kotter.runtime.render.RenderScope
import java.time.Duration

// This file introduces the `textorize` method which applies one of several TextorizeEffect effects. You can call this
// method within a render block. When you use it, it automatically adds a timer that causes the block to repaint
// enabling animated effects.
//
// You can change the active effect within a run block using `setTextorizeEffect` (or `setNextTextorizeEffect` if you
// just want to increment through them).
//
// In other words:
//
// ```
// section {
//   textorize("Example")
// }.runUntilKeyPressed(Keys.Q) {
//   onKeyPressed {
//     when(key) {
//       Keys.1 -> setTextorizeEffect(TextorizeEffect.RAINBOW)
//       Keys.2 -> setTextorizeEffect(TextorizeEffect.BLINKING)
//       Keys.3 -> setTextorizeEffect(TextorizeEffect.SCROLLING)
//     }
//   }
// }
// ```
//
// This example is kind of forced, but it serves to showcase how you can save state using the backing `data` field.
// Since we are using a key with a `Section.Lifecycle`, our state will automatically be cleaned up when the section
// exits.

private val TextorizeStateKey = Section.Lifecycle.createKey<TextorizeState>()

enum class TextorizeEffect {
    RAINBOW,
    BLINKING,
    SCROLLING,
}

class TextorizeState(
    renderScope: RenderScope,
    currentEffect: TextorizeEffect
) {
    var counter = 0
        private set

    var currentEffect = currentEffect
        set(value) {
            counter = 0
            field = value
        }

    init {
        renderScope.data.addTimer(Duration.ofMillis(125), repeat = true) {
            counter++
            renderScope.section.requestRerender()
        }
    }
}

private val RAINBOW_COLORS = listOf(
    0xFF0000,
    0xFF7F00,
    0xFFFF00,
    0x00FF00,
    0x0000FF,
    0x3783FF,
    0x9400D3,
)

fun RenderScope.textorize(text: String, initialEffect: TextorizeEffect = TextorizeEffect.values()[0]) {
    data.putIfAbsent(TextorizeStateKey, provideInitialValue = { TextorizeState(this, initialEffect) }) {
        scopedState {
            when (currentEffect) {
                TextorizeEffect.RAINBOW -> {
                    // Math works out so that colors move right to left
                    val startingIndex = RAINBOW_COLORS.size - (counter % RAINBOW_COLORS.size)
                    var nextColorOffset = 0
                    text.forEach { c ->
                        if (!c.isWhitespace()) {
                            val colorIndex = (startingIndex + nextColorOffset) % RAINBOW_COLORS.size
                            rgb(RAINBOW_COLORS[colorIndex])
                            ++nextColorOffset
                        }
                        text(c)
                    }
                }
                TextorizeEffect.BLINKING -> {
                    val shouldBlink = (counter / 5) % 2 == 1
                    if (shouldBlink) invert()
                    text(text)
                    clearInvert()
                }
                TextorizeEffect.SCROLLING -> {
                    // Add a space so that when text wraps there's a blank between the first and last words
                    val finalText = "$text "
                    val firstChar = counter % finalText.length
                    text(finalText.substring(firstChar, finalText.length))
                    text(finalText.substring(0, firstChar))
                }
            }
        }
    }
}

// We use `SectionScope` here to indicate that this function can be called from both `section` AND `run` blocks.
private fun SectionScope.getTexterizeState(): TextorizeState {
    return data.getValue(TextorizeStateKey)
}

fun SectionScope.getTextorizeEffect(): TextorizeEffect {
    return getTexterizeState().currentEffect
}

// We use `RunScope` here because it doesn't really make sense to set the textorize effect inside a render pass
// (even if we technically could)
fun RunScope.setTextorizeEffect(effect: TextorizeEffect) {
    getTexterizeState().currentEffect = effect
}

fun RunScope.setNextTextorizeEffect() {
    setTextorizeEffect(TextorizeEffect.values()[(getTextorizeEffect().ordinal + 1) % TextorizeEffect.values().size])
}