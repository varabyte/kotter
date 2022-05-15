import MandelbrotModel.Companion.MAX_ITERATIONS
import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.input.runUntilKeyPressed
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.timer.addTimer
import com.varabyte.kotterx.decorations.bordered
import kotlinx.coroutines.*
import java.time.Duration

private const val VIEW_WIDTH = 70
private const val VIEW_HEIGHT = 17

@JvmInline
value class Imaginary(val value: Double) {
    operator fun plus(other: Imaginary) = Imaginary(this.value + other.value)
    operator fun times(other: Imaginary) = -(value * other.value)
    operator fun times(other: Double) = Imaginary(this.value * other)
}

val Double.i get() = Imaginary(this)
operator fun Double.times(img: Imaginary) = Imaginary(this * img.value)

class Complex(val real: Double, val img: Imaginary) {
    operator fun plus(other: Complex) = Complex(this.real + other.real, this.img + other.img)
    operator fun times(other: Complex) = Complex(
        this.real * other.real + this.img * other.img, this.real * other.img + this.img * other.real
    )

    // The square of the absolute value; avoids an unnecessary square root if you don't care about it.
    // (e.g. testing abs() > 2 you can just do abs2() > 4)
    fun abs2() = real * real + img.value * img.value
}

operator fun Double.plus(img: Imaginary) = Complex(this, img)

class MandelbrotModel {
    companion object {
        const val MAX_ITERATIONS = 26
    }
    var isCalculating = false
        private set
    /** Buffer of number of iterations it took before this value diverged */
    val iterations = Array(VIEW_WIDTH * VIEW_HEIGHT) { 0 }

    /** Recalculate the value of [iterations] centered at [cx] by [cy] */
    fun calculate(cx: Double, cy: Double, zoom: Double): Boolean {
        if (isCalculating) return false
        isCalculating = true
        runBlocking {
            val jobs = mutableListOf<Job>()
            val delta = 1.0 / zoom
            val centerCellY = VIEW_HEIGHT / 2
            val centerCellX = VIEW_WIDTH / 2
            for (cellY in 0 until VIEW_HEIGHT) {
                // Note: Screen coordinates are flipped from math coordinates
                val y = -(cy + (cellY - centerCellY) * delta)
                for (cellX in 0 until VIEW_WIDTH) {
                    val x = cx + (cellX - centerCellX) * delta
                    jobs.add(calculateCell(cellX, cellY, x, y))
                }
            }

            jobs.joinAll()
        }
        isCalculating = false
        return true
    }


    // See: https://en.wikipedia.org/wiki/Mandelbrot_set#Formal_definition
    // See also: https://matplotlib.org/matplotblog/posts/animated-fractals (code is in python)
    private fun calculateCell(cellX: Int, cellY: Int, x: Double, y: Double): Job {
        // Probably overkill for our limited terminal resolution. But, Mandelbrot calculations are
        // totally parallelizable, so if just for the sake of demonstration, let's do it!
        // Since this a toy example, I did not profile timing, but in production, you totally should.
        // Use default dispatcher for calculation heavy logic
        return CoroutineScope(Dispatchers.Default).launch {
            val c = x + y.i
            var z = 0.0 + 0.0.i

            for (i in 0 .. MAX_ITERATIONS) {
                iterations[cellX, cellY] = i
                if (z.abs2() > 4.0) break
                z = (z * z) + c
            }
        }
    }
}

operator fun Array<Int>.get(x: Int, y: Int) = this[y * VIEW_WIDTH + x]
operator fun Array<Int>.set(x: Int, y: Int, value: Int) {
    this[y * VIEW_WIDTH + x] = value
}


fun main() = session(clearTerminal = true) {
    section {
        p {
            cyan {
                textLine("Press SPACE to toggle animating on and off")
                textLine("Press ARROW KEYS to pan around")
                textLine("Press W/S to zoom in/out")
                textLine("Press Q to quit")
            }
        }
    }.run()

    val mandelbrot = MandelbrotModel()
    var zoom = 25.0
    var cx = -0.5
    var cy = 0.0
    mandelbrot.calculate(cx, cy, zoom)
    section {
        bordered {
            for (y in 0 until VIEW_HEIGHT) {
                for (x in 0 until VIEW_WIDTH) {
                    // Lots of richness in early iteration aborts, so choose interesting colors for those crevices.
                    // TODO: Choose colors more algorithmically and less brute force?
                    val numIterations = mandelbrot.iterations[x, y]
                    val color = when {
                        numIterations == MAX_ITERATIONS -> Color.BRIGHT_WHITE
                        numIterations > 20 -> Color.WHITE
                        numIterations > 15 -> Color.BRIGHT_YELLOW
                        numIterations > 12 -> Color.YELLOW
                        numIterations > 10 -> Color.BRIGHT_MAGENTA
                        numIterations > 8 -> Color.MAGENTA
                        numIterations > 6 -> Color.BRIGHT_RED
                        numIterations > 4 -> Color.RED
                        numIterations > 2 -> Color.BRIGHT_BLACK
                        numIterations > 0 -> Color.BLACK
                        else -> null
                    }
                    if (color != null) {
                        color(color)
                        text("*")
                    }
                }
                textLine()
            }
        }
    }.runUntilKeyPressed(Keys.Q) {
        var paused = true
        onKeyPressed {
            if (key == Keys.SPACE) {
                paused = !paused
                return@onKeyPressed
            }

            when (key) {
                Keys.W -> { zoom *= 1.1 }
                Keys.S -> { zoom = (zoom / 1.1).coerceAtLeast(1.0) }
                Keys.LEFT -> { cx -= (1.0 / zoom) }
                Keys.RIGHT -> { cx += (1.0 / zoom) }
                Keys.UP -> { cy -= (1.0 / zoom) }
                Keys.DOWN -> { cy += (1.0 / zoom) }
                else -> return@onKeyPressed
            }

            paused = true
            if (mandelbrot.calculate(cx, cy, zoom)) {
                rerender()
            }
        }
        addTimer(Duration.ofMillis(50), repeat = true) {
            if (!paused) {
                zoom *= 1.1
                if (mandelbrot.calculate(cx, cy, zoom)) {
                    rerender()
                }
            }
        }
    }
}