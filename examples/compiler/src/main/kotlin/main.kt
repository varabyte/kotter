import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.collections.*
import com.varabyte.kotter.foundation.render.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.foundation.timer.*
import com.varabyte.kotter.runtime.render.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class Report(private val line: Int, private val text: String, private val isError: Boolean) {
    fun render(scope: RenderScope) {
        scope.apply {
            when (isError) {
                true -> {
                    red(); text("e")
                }

                false -> {
                    yellow(); text("w")
                }
            }
            textLine(" (L$line): $text")
        }
    }
}

sealed class CompileResult(val warnings: List<Report>) {
    class Success(warnings: List<Report>) : CompileResult(warnings)
    class Failure(warnings: List<Report>, val errors: List<Report>) : CompileResult(warnings)
}

sealed class ThreadState {
    class Working(val file: String) : ThreadState()
    object Idle : ThreadState()
}

private val SAMPLE_WARNINGS = listOf(
    "Shadowed variable",
    "Unchecked cast",
    "Unused method",
    "Unused variable",
    "Deprecated method",
    "Field can be final",
    "Method can be private",
    "`when` statement not exhaustive",
)

private val SAMPLE_ERRORS = listOf(
    "Class already defined",
    "Keyword expected",
    "Incorrect parameter count in function call",
    "Can't call a private method",
    "Illegal character",
    "Cannot cast between types",
    "Override doesn't match any method in super class",
    "Type mismatch",
    "Unresolved name",
)

fun main() = session {
    var elapsedMs by liveVarOf(0L)
    var finished by liveVarOf(false)
    val filesToCompile = liveListOf<String>().apply {
        val rootDir = "/home/user/projects/demo/src/main/kotlin"
        ('a'..'z')
            .map { "$rootDir/$it.kt" }
            .forEach { filename -> add(filename) }
    }
    val results = liveListOf<CompileResult>()
    val threads = liveListOf<ThreadState>().apply {
        repeat(4) { add(ThreadState.Idle) }
    }

    section {
        threads.forEachIndexed { i, threadState ->
            text("Thread #${i + 1}: ")
            when (threadState) {
                ThreadState.Idle -> green { text("Idle") }
                is ThreadState.Working -> cyan {
                    text("Compiling ${threadState.file.substringAfterLast('/')}")
                }
            }
            textLine()
        }

        if (finished) {
            textLine()
            text("Finished in ${(elapsedMs / 1000.0).roundToInt()}s with ")
            yellow { text("${results.sumOf { it.warnings.size }} warning(s)") }
            text(" and ")
            red { text("${results.filterIsInstance<CompileResult.Failure>().sumOf { it.errors.size }} error(s)") }
        }
    }.run {
        // Use a random with a fixed seed for deterministic output.
        val random = Random(1234)

        val jobs = mutableListOf<Job>()
        val scope = CoroutineScope(SupervisorJob())
        repeat(threads.size) { threadIndex ->
            jobs.add(scope.launch {
                while (true) {
                    val file = filesToCompile.removeFirstOrNull() ?: break
                    threads[threadIndex] = ThreadState.Working(file)
                    delay(random.nextLong(250, 2000))
                    val succeeded = random.nextFloat() < 0.9f
                    val hasWarnings = random.nextFloat() < 0.4f
                    val warnings = mutableListOf<Report>()
                    if (hasWarnings) {
                        var line = 1
                        for (i in 0..random.nextInt(2)) {
                            line += random.nextInt(0, 100)
                            warnings.add(Report(line, SAMPLE_WARNINGS.random(), isError = false))
                        }
                    }
                    val result = if (succeeded) {
                        CompileResult.Success(warnings)
                    } else {
                        val errors = mutableListOf<Report>()
                        var line = 1
                        for (i in 0..random.nextInt(3)) {
                            line += random.nextInt(0, 100)
                            errors.add(Report(line, SAMPLE_ERRORS.random(), isError = true))
                        }
                        CompileResult.Failure(warnings, errors)
                    }

                    if (result.warnings.isNotEmpty() || result is CompileResult.Failure) {
                        aside {
                            underline { cyan { textLine(file) } }
                            result.warnings.forEach { warning -> warning.render(this) }
                            if (result is CompileResult.Failure) {
                                result.errors.forEach { error -> error.render(this) }
                            }
                            textLine()
                        }
                    }

                    results.add(result)
                }
                threads[threadIndex] = ThreadState.Idle
            })
        }

        addTimer(10.milliseconds, repeat = true) {
            elapsedMs += this.elapsed.inWholeMilliseconds
        }
        jobs.forEach { job -> job.join() }
        finished = true
    }
}
