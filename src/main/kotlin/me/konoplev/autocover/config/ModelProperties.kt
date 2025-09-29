package me.konoplev.autocover.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@ConfigurationProperties(prefix = "model")
@ConfigurationPropertiesScan
data class ModelProperties(
    val provider: LLMProvider = LLMProvider.OPENAI,
    val name: String = "gpt-4.1-nano",
    val temperature: Double = 0.7,
    val max: MaxProperties = MaxProperties(),
    val timeout: TimeoutProperties = TimeoutProperties(),
    val api: ApiProperties = ApiProperties(),
    val google: GoogleProperties = GoogleProperties(),
    val ollama: OllamaProperties = OllamaProperties(),
    val rateLimit: RateLimitProperties = RateLimitProperties(),
)

data class MaxProperties(
    val tokens: Int = 4096,
)

data class TimeoutProperties(
    val seconds: Long = 30,
)

enum class LLMProvider {
    OPENAI,
    ANTHROPIC,
    GOOGLE,
    OLLAMA,
}

data class ApiProperties(
    val key: String = "",
)

data class GoogleProperties(
    val projectId: String = "",
    val location: String = "us-central1",
)

data class OllamaProperties(
    val baseUrl: String = "http://localhost:11434",
)

data class RateLimitProperties(
    val maxRetries: Int = 10,
    val initialDelaySeconds: Long = 3,
    val backoffMultiplier: Double = 1.5,
    val jitter: Boolean = true,
)
