package me.konoplev.autocover.tools.compile

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import me.konoplev.autocover.config.AgentProperties
import me.konoplev.autocover.services.CommandExecutionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

@Component
class CompileTool(
    private val agentProperties: AgentProperties,
    private val commandExecutionService: CommandExecutionService,
) : me.konoplev.autocover.tools.Tool {

    private val logger = LoggerFactory.getLogger(CompileTool::class.java)

    @Tool("Compile and run tests for a project directory")
    fun compileAndTest(
        @P("The project directory path to compile and test") projectDirectory: String,
    ): String {
        return try {
            logger.debug("Compiling and testing project in directory: {}", projectDirectory)

            val projectDir = File(projectDirectory)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                return "Error: Project directory does not exist: $projectDirectory"
            }

            // Use the test command from agent properties
            val testCommand = agentProperties.coverage.testCommand
            if (testCommand.isNullOrBlank()) {
                return "Error: No test command configured. Please set agent.coverage.test-command property."
            }

            logger.debug("Executing test command: {} in directory: {}", testCommand, projectDirectory)

            // Execute the test command and capture output
            val result = commandExecutionService.executeCommand(testCommand, projectDir)

            if (result.exitCode == 0) {
                "✅ Compilation and tests successful!\n\nOutput:\n${result.output}"
            } else {
                "❌ Compilation or tests failed with exit code ${result.exitCode}\n\n" +
                    "Error output:\n${result.errorOutput}\n\n" +
                    "Standard output:\n${result.output}\n\n" +
                    "Please fix the errors above and try again."
            }
        } catch (e: Exception) {
            val errorMessage = "Error compiling and testing project in $projectDirectory: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }
}
