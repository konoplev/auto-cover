package me.konoplev.autocover.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@ConfigurationProperties(prefix = "agent")
@ConfigurationPropertiesScan
data class AgentProperties(
    val coverage: CoverageProperties = CoverageProperties(),
    val memory: MemoryProperties = MemoryProperties(),
    val logging: LoggingProperties = LoggingProperties(),
)

data class CoverageProperties(
    val testCommand: String? = null,
    val testResultLocation: String? = null,
)

data class MemoryProperties(
    val max: MaxMessagesProperties = MaxMessagesProperties(),
)

data class MaxMessagesProperties(
    val messages: Int = 20,
)

data class LoggingProperties(
    val enabled: Boolean = true,
)
