package me.konoplev.autocover.config.factory

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel
import me.konoplev.autocover.config.LLMProvider
import me.konoplev.autocover.config.ModelProperties
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class LLMFactory(
    private val modelProperties: ModelProperties,
) {

    private val logger = LoggerFactory.getLogger(LLMFactory::class.java)

    fun createChatModel(): ChatLanguageModel {
        logger.info("Creating chat model for provider: {}", modelProperties.provider)

        return when (modelProperties.provider) {
            LLMProvider.OPENAI -> createOpenAiModel()
            LLMProvider.ANTHROPIC -> createAnthropicModel()
            LLMProvider.GOOGLE -> createGoogleModel()
            LLMProvider.OLLAMA -> createOllamaModel()
        }
    }

    private fun createOpenAiModel(): ChatLanguageModel {
        return OpenAiChatModel.builder()
            .apiKey(modelProperties.api.key)
            .modelName(modelProperties.name)
            .temperature(modelProperties.temperature)
            .maxTokens(modelProperties.max.tokens)
            .timeout(Duration.ofSeconds(modelProperties.timeout.seconds))
            .build()
    }

    private fun createAnthropicModel(): ChatLanguageModel {
        return AnthropicChatModel(
            apiKey = modelProperties.api.key,
            modelName = modelProperties.name,
            temperature = modelProperties.temperature,
            maxTokens = modelProperties.max.tokens,
            timeout = Duration.ofSeconds(modelProperties.timeout.seconds),
        )
    }

    private fun createGoogleModel(): ChatLanguageModel {
        return VertexAiGeminiChatModel.builder()
            .project(modelProperties.google.projectId)
            .location(modelProperties.google.location)
            .modelName(modelProperties.name)
            .temperature(modelProperties.temperature.toFloat())
            .maxOutputTokens(modelProperties.max.tokens)
            .build()
    }

    private fun createOllamaModel(): ChatLanguageModel {
        return OllamaChatModel.builder()
            .baseUrl(modelProperties.ollama.baseUrl)
            .modelName(modelProperties.name)
            .temperature(modelProperties.temperature)
            .timeout(Duration.ofSeconds(modelProperties.timeout.seconds))
            .numPredict(4096) // Allow longer responses for complete file generation
            .build()
    }
}

class AnthropicChatModel(
    private val apiKey: String,
    private val modelName: String,
    private val temperature: Double,
    private val maxTokens: Int,
    private val timeout: Duration,
) : ChatLanguageModel {

    private val client = OkHttpClient.Builder()
        .connectTimeout(timeout)
        .readTimeout(timeout)
        .writeTimeout(timeout)
        .build()

    private val objectMapper = jacksonObjectMapper()

    override fun generate(userMessage: String): String {
        val requestBody = mapOf(
            "model" to modelName,
            "max_tokens" to maxTokens,
            "temperature" to temperature,
            "messages" to listOf(
                mapOf("role" to "user", "content" to userMessage),
            ),
        )

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(objectMapper.writeValueAsString(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Anthropic API request failed: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string() ?: throw RuntimeException("Empty response body")
            val jsonResponse = objectMapper.readTree(responseBody)

            return jsonResponse.get("content")?.get(0)?.get("text")?.asText()
                ?: throw RuntimeException("No text content in response")
        }
    }

    override fun generate(messages: MutableList<ChatMessage>): Response<AiMessage> {
        val userMessage = messages.lastOrNull()?.text() ?: ""
        val responseText = generate(userMessage)
        val aiMessage = AiMessage.from(responseText)
        return Response.from(aiMessage)
    }
}
