import TestState.*
import com.varabyte.konsole.core.KonsoleScope
import com.varabyte.konsole.core.konsoleApp
import com.varabyte.konsole.core.text.*
import com.varabyte.konsole.core.text.ColorLayer.BG
import com.varabyte.konsole.core.timer.addTimer
import kotlinx.coroutines.*
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.random.Random

data class Test(
    val path: String,
    val state: TestState,
)

enum class TestState {
    Running,
    Pass,
    Fail,
}

private fun KonsoleScope.testRow(test: Test) {
    scopedState {
        black()
        when (test.state) {
            Running -> yellow(BG)
            Pass -> green(BG)
            Fail -> red(BG)
        }

        val stateStr = when (test.state) {
            Running -> "RUNS"
            Pass -> "PASS"
            Fail -> "FAIL"
        }

        text(" $stateStr ")
    }

    val dir = test.path.substringBeforeLast('/')
    val filename = test.path.substringAfterLast('/')
    text(" $dir/ ")
    bold { textLine(filename) }
}

private fun KonsoleScope.summary(tests: List<Test>, elapsedSecs: Int) {
    text("Tests: ")

    val numFailed = tests.count { it.state == Fail }
    if (numFailed > 0) {
        red { text("$numFailed failed") }
        text(", ")
    }

    val numPassed = tests.count { it.state == Pass }
    if (numPassed > 0) {
        green { text("$numPassed passed") }
        text(", ")
    }

    textLine("${tests.size} total")
    textLine("Time: ${elapsedSecs}s")
    textLine()
}

// https://github.com/JakeWharton/mosaic/tree/trunk/samples/jest
fun main() = konsoleApp {
    // Use a random with a fixed seed for deterministic output.
    val random = Random(1234)

    var elapsedSecs by KonsoleVar(0)
    val tests = KonsoleList<Test>()
    val paths = ConcurrentLinkedDeque(
        listOf(
            "tests/login.kt",
            "tests/signup.kt",
            "tests/forgot-password.kt",
            "tests/reset-password.kt",
            "tests/view-profile.kt",
            "tests/edit-profile.kt",
            "tests/delete-profile.kt",
            "tests/posts.kt",
            "tests/post.kt",
            "tests/comments.kt",
        )
    )

    konsole {
        val (done, running) = tests.partition { it.state != Running }
        if (done.isNotEmpty()) {
            for (test in done) {
                testRow(test)
            }
            textLine()
        }
        if (running.isNotEmpty()) {
            for (test in running) {
                testRow(test)
            }
            textLine()
        }

        summary(tests, elapsedSecs)
    }.run {
        val jobs = mutableListOf<Job>()
        val scope = CoroutineScope(SupervisorJob())
        repeat(4) {
            jobs.add(scope.launch {
                while (true) {
                    val path = paths.pollFirst() ?: break
                    val index = tests.withLock {
                        this += Test(path, Running)
                        size - 1
                    }
                    delay(random.nextLong(2_500L, 4_000L))

                    // Flip a coin biased towards success to produce the final state of the test
                    val newState = if (random.nextFloat() < 0.6f) Pass else Fail
                    tests[index] = tests[index].copy(state = newState)
                }
            })
        }
        addTimer(Duration.ofSeconds(1), repeat = true) {
            ++elapsedSecs
        }
        jobs.forEach { job -> job.join() }
    }
}