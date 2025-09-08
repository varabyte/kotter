import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.text.*
import kotlin.math.roundToInt

const val W = 40
const val H = 15

private fun Int.toHexStr() = this.toString(16).padStart(2, '0')
private fun Float.toPercentStr() = "${(this * 100).roundToInt()}%"

enum class State {
    PICKING_COLOR,
    ENTERING_HUE,
    ENTERING_SAT,
    ENTERING_VAL,
}

fun main() = session {
    // Instructions never need to change; output them first
    section {
        p {
            textLine("h: enter hue")
            textLine("s: enter saturation")
            textLine("v: enter value")
            textLine()
            textLine("arrows: move picker (changes saturation and value)")
            textLine("home/end: saturation to 0/100%")
            textLine("pg up/down: value to 0/100%")
            textLine()
            textLine("Press Q to quit")
            textLine()
            textLine("NOTE: If colors look clumped, your terminal may not support true colors")
            textLine()
        }
    }.run()

    // Instructions never need to change; output them first
    var xCurr by liveVarOf(0)
    var yCurr by liveVarOf(0)
    var h by liveVarOf(0)
    var state by liveVarOf(State.PICKING_COLOR)
    section {
        val sCurr = (xCurr / W.toFloat())
        val vCurr = 1f - (yCurr / H.toFloat())

        for (y in 0..H) {
            for (x in 0..W) {
                val s = x / W.toFloat()
                val v = 1f - (y / H.toFloat())

                if (xCurr == x && yCurr == y) {
                    text("░")
                } else {
                    hsv(h, s, v) {
                        text("█")
                    }
                }
            }
            textLine()
        }

        val rgb = HSV(h, sCurr, vCurr).toRgb()
        textLine()
        when (state) {
            State.ENTERING_HUE -> {
                text("Enter hue (0 - 360): "); input(); textLine()
            }

            State.ENTERING_SAT -> {
                text("Enter saturation (0 - 100): "); input(); textLine()
            }

            State.ENTERING_VAL -> {
                text("Enter value (0 - 100): "); input(); textLine()
            }

            else -> {
                textLine("HSV: $h°, ${sCurr.toPercentStr()}, ${vCurr.toPercentStr()}")
                textLine("RGB: ${rgb.r}, ${rgb.g}, ${rgb.b} (0x${rgb.r.toHexStr()}${rgb.g.toHexStr()}${rgb.b.toHexStr()})")
            }
        }
        textLine()
    }.runUntilSignal {
        onKeyPressed {
            if (state == State.PICKING_COLOR) {
                when (key) {
                    Keys.Left -> xCurr = (xCurr - 1).coerceAtLeast(0)
                    Keys.Right -> xCurr = (xCurr + 1).coerceAtMost(W)
                    Keys.Up -> yCurr = (yCurr - 1).coerceAtLeast(0)
                    Keys.Down -> yCurr = (yCurr + 1).coerceAtMost(H)
                    Keys.Home -> xCurr = 0
                    Keys.End -> xCurr = W
                    Keys.PageUp -> yCurr = 0
                    Keys.PageDown -> yCurr = H
                    Keys.H -> state = State.ENTERING_HUE
                    Keys.S -> state = State.ENTERING_SAT
                    Keys.V -> state = State.ENTERING_VAL
                    Keys.Q -> signal()
                }
            } else {
                if (key == Keys.Escape) {
                    state = State.PICKING_COLOR
                }
            }
        }

        onInputEntered {
            when (state) {
                State.ENTERING_HUE -> {
                    input.toIntOrNull()?.let { hIn ->
                        if (hIn in (0..360)) {
                            h = hIn
                        }
                    }
                }

                State.ENTERING_SAT -> {
                    input.toIntOrNull()?.let { sIn ->
                        if (sIn in (0..100)) {
                            xCurr = W * sIn / 100
                        }
                    }
                }

                State.ENTERING_VAL -> {
                    input.toIntOrNull()?.let { vIn ->
                        if (vIn in (0..100)) {
                            yCurr = H - H * vIn / 100
                        }
                    }
                }

                else -> {
                    error("Unexpected state: $state")
                }
            }

            clearInput()
            state = State.PICKING_COLOR
        }
    }
}
