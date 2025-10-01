package me.konoplev.autocover.services

import com.fasterxml.jackson.databind.ObjectMapper
import dev.langchain4j.service.SystemMessage
import me.konoplev.autocover.config.AgentProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectConfiguration(
    val testCommand: String,
    val testResultLocation: String
)

@Service
class ConfigurationHelper(
    private val agentProperties: AgentProperties,
    private val projectConfigurationAssistant: ProjectConfigurationAssistant,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(ConfigurationHelper::class.java)

    fun isConfigurationInstructionNeeded(): Boolean =
        agentProperties.coverage.testCommand.isNullOrBlank() || agentProperties.coverage.testResultLocation.isNullOrBlank()

    fun getTestCoverageConfiguration(currentDirectory: String = java.io.File("").absolutePath): ProjectConfiguration? {
        val response = projectConfigurationAssistant.chat(
            """
                You are an expert software engineer tasked with detecting the testing framework and build system configuration for a project.

                Current working directory: $currentDirectory

                Your goal is to analyze the provided project files and determine:
                1. The appropriate test command to run tests and generate code coverage reports
                2. The location where the coverage report will be generated
                3. If coverage tools are not configured, set them up and configure them properly

                IMPORTANT: You have access to command execution tools. Use them to:
                - Analyze the project structure to identify the build system (Maven, Gradle, etc.)
                - Execute test commands to verify they work
                - Check if coverage reports are generated
                - Configure coverage tools if needed by running setup commands
                - Determine the best coverage tool for the project (JaCoCo, Cobertura, Istanbul, etc.)

                CRITICAL: Your response must be ONLY a valid JSON object with the exact structure shown below. Do not include any explanations, error messages, or additional text.

                Example of the required JSON format:
                {
                    "testCommand": "mvn test",
                    "testResultLocation": "target/site/jacoco/jacoco.xml"
                }

                Use the command execution tools to verify that:
                1. The test command actually works in the project directory
                2. The test result location exists after running the command
                3. If coverage tools are not configured, execute the necessary commands to set them up

                IMPORTANT: After using tools, you must return the final result in the JSON format above. Do not return tool calls or function calls - return the actual configuration values.

                Your response must start with { and end with }. No other text before or after the JSON.
            """.trimIndent(),
        )
        return parseConfigurationResponse(response)
    }

    fun parseConfigurationResponse(response: String): ProjectConfiguration? {
        return try {
            logger.debug("Parsing configuration response: {}", response)
            objectMapper.readValue(response, ProjectConfiguration::class.java)
        } catch (e: Exception) {
            logger.error("Failed to parse configuration response: {}", response, e)
            null
        }
    }
}

interface ProjectConfigurationAssistant {
    @SystemMessage("You have access to file system tools and command execution tools. When using tools, you MUST provide ALL required parameters. For example: readFile(\"path/to/file\"), writeFile(\"path/to/file\", \"content\"), findFilesByExtension(\"directory\", \"extension\"), executeCommand(\"command\", \"workingDirectory\"). ")
    fun chat(message: String): String
}
