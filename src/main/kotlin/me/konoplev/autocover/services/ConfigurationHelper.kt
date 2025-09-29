package me.konoplev.autocover.services

import dev.langchain4j.service.SystemMessage
import me.konoplev.autocover.config.AgentProperties
import org.springframework.stereotype.Service

@Service
class ConfigurationHelper(
    private val agentProperties: AgentProperties,
    private val projectConfigurationAssistant: ProjectConfigurationAssistant,
) {
    fun isConfigurationInstructionNeeded(): Boolean =
        agentProperties.coverage.testCommand.isNullOrBlank() || agentProperties.coverage.testResultLocation.isNullOrBlank()

    fun getInstructions(currentDirectory: String = java.io.File("").absolutePath): String {
        return projectConfigurationAssistant.chat(
            """
                You are an expert software engineer tasked with detecting the testing framework and build system configuration for a project.

                Current working directory: $currentDirectory

                Your goal is to analyze the provided project files and determine:
                The way to run the tests using the current framework to get the Cobertura XML report (with detailed tests coverage).
                In case the Cobertura not configured, suggest the steps to configure it.
                In the output mention the command to run tests and the path to the Cobertura XML.
                The command should be prefixed with `agentProperties.coverage.testCommand: ` and the path with `agentProperties.coverage.testResultLocation`
            """.trimIndent(),
        )
    }
}

interface ProjectConfigurationAssistant {
    @SystemMessage("You have access to file system tools. When using tools, you MUST provide ALL required parameters. For example: readFile(\"path/to/file\"), writeFile(\"path/to/file\", \"content\"), findFilesByExtension(\"directory\", \"extension\"). ")
    fun chat(message: String): String
}
