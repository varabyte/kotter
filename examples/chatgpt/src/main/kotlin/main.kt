import com.varabyte.kotter.foundation.anim.Anim
import com.varabyte.kotter.foundation.anim.text
import com.varabyte.kotter.foundation.anim.textAnimOf
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.render.aside
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.shutdown.addShutdownHook
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.render.RenderScope
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

enum class State {
    USER_TYPING,
    ROBOT_THINKING,
    ROBOT_TYPED,
}

enum class UserOption {
    REGENERATE,
    QUIT
}

// region ChatGPT API: See https://platform.openai.com/docs/api-reference/making-requests

const val CHATGPT_BASE_URL = "https://api.openai.com/v1/"
val JSON_MEDIA_TYPE = "application/json".toMediaType()

class ApiKeyInterceptor(val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        return chain.proceed(request)
    }
}

@Serializable
data class Message(
    val role: String,
    val content: String,
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
) {
    constructor(model: String, message: Message) : this(model, listOf(message))
}

@Serializable
data class ChatResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val usage: Usage,
    val choices: List<Choice>
) {
    @Serializable
    data class Usage(
        val promptTokens: Long,
        val completionTokens: Long,
        val totalTokens: Long
    )

    @Serializable
    data class Choice(
        val message: Message,
        val finishReason: String,
        val index: Long,
    )
}

// endregion

suspend fun sendMessageToChatGpt(json: Json, httpClient: OkHttpClient, message: String): ChatResponse? {
    val request = Request.Builder()
        .url("${CHATGPT_BASE_URL}chat/completions")
        .post(
            json.encodeToString(
                ChatRequest(
                    model = "gpt-3.5-turbo",
                    message = Message(
                        role = "user",
                        content = message
                    )
                )
            ).toRequestBody(JSON_MEDIA_TYPE)
        ).build()

    return withContext(Dispatchers.IO) {
        val call = httpClient.newCall(request)
        val response = call.execute()

        response.body?.takeIf { response.code == 200 }?.let { body ->
            json.decodeFromString(body.string())
        }
    }
}

fun main() = session {
    val apiKey = System.getenv("OPENAI_KEY")?.takeIf { it.isNotEmpty() }
    if (apiKey == null) {
        section {
            textLine()
            red {
                textLine("OPENAI_KEY environment variable must be set!")
            }
            textLine("Please see the README for instructions.")
            textLine("Program exiting.")
        }.run()
        return@session
    }

    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        // OpenAI uses snake_case, while our Kotlin objects use pascalCase
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    val httpClient = OkHttpClient.Builder()
        .addInterceptor(ApiKeyInterceptor(apiKey))
        .writeTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .build()

    var state by liveVarOf(State.USER_TYPING)
    var userText by liveVarOf("")
    var robotText by liveVarOf("")
    var userOption by liveVarOf(UserOption.REGENERATE)
    val thinkingAnim = textAnimOf(listOf("⠋", "⠙", "⠸", "⠴", "⠦", "⠇"), 150.milliseconds)

    val userPrompt = "\uD83E\uDD14 > "
    val robotPrompt = "\uD83E\uDD16 > "

    fun RenderScope.humanColor(block: RenderScope.() -> Unit) {
        white(scopedBlock = block)
    }
    fun RenderScope.robotColor(block: RenderScope.() -> Unit) {
        // I yoinked the cyan-ish color of ChatGPT's icon
        rgb(0x00a78e, scopedBlock = block)
    }
    fun RenderScope.infoColor(block: RenderScope.() -> Unit) {
        black(isBright = true, scopedBlock = block)
    }
    fun RenderScope.invertIf(condition: Boolean, block: RenderScope.() -> Unit) {
        if (condition) invert()
        block()
        if (condition) clearInvert()
    }

    section {
        textLine()
        when (state) {
            State.USER_TYPING -> {
                humanColor { text(userPrompt); input() }
            }

            State.ROBOT_THINKING -> {
                humanColor { text(userPrompt); textLine(userText) }
                textLine()
                robotColor { text(robotPrompt); text(thinkingAnim) }
            }

            State.ROBOT_TYPED -> {
                humanColor { text(userPrompt); textLine(userText) }
                textLine()
                robotColor { text(robotPrompt); textLine(robotText) }
                textLine()
                infoColor { textLine("Use the arrow keys then ENTER to select an option, or begin typing to continue chatting.") }
                textLine()
                invertIf(userOption == UserOption.REGENERATE) {
                    text(" Regenerate ")
                }
                text(' ')
                invertIf(userOption == UserOption.QUIT) {
                    text(" Quit ")
                }
                textLine()
                textLine()
                humanColor { text(userPrompt) }
            }
        }
    }.run {
        var shouldQuit = false
        addShutdownHook { shouldQuit = true }

        onInputEntered {
            check(state == State.USER_TYPING)
            userText = input
            state = State.ROBOT_THINKING
        }

        onKeyPressed {
            if (state == State.ROBOT_TYPED) {
                fun continueChatting(initialText: String? = null) {
                    aside {
                        // Add previous chat results to the console history
                        textLine()
                        humanColor { text(userPrompt); textLine(userText) }
                        textLine()
                        robotColor { text(robotPrompt); textLine(robotText) }
                    }

                    // Reset state as we begin another round of chatting
                    userOption = UserOption.REGENERATE
                    userText = ""
                    robotText = ""
                    setInput(initialText.orEmpty())
                    state = State.USER_TYPING
                }

                when (key) {
                    Keys.ENTER -> {
                        when (userOption) {
                            UserOption.REGENERATE -> state = State.ROBOT_THINKING
                            UserOption.QUIT -> shouldQuit = true
                        }
                    }
                    Keys.LEFT -> {
                        userOption = UserOption.values()[(userOption.ordinal + UserOption.values().size - 1) % UserOption.values().size]
                    }
                    Keys.RIGHT -> {
                        userOption = UserOption.values()[(userOption.ordinal + 1) % UserOption.values().size]
                    }
                    Keys.DOWN -> continueChatting()
                    is CharKey -> continueChatting((key as CharKey).code.toString())
                }
            }
        }

        var lastState = state
        while (!shouldQuit) {
            if (lastState != state) {
                lastState = state

                if (state == State.ROBOT_THINKING) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val response = sendMessageToChatGpt(json, httpClient, userText)
                        robotText = if (response != null && response.choices.isNotEmpty()) {
                            response.choices.first().message.content
                        } else {
                            "(Error retrieving text from ChatGPT)"
                        }
                        state = State.ROBOT_TYPED
                    }
                }
            }
            delay(Anim.ONE_FRAME_60FPS)
        }
    }
}