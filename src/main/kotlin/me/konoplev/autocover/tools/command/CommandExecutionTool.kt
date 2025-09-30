package me.konoplev.autocover.tools.command

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import me.konoplev.autocover.services.CommandExecutionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

@Component
class CommandExecutionTool(
    private val commandExecutionService: CommandExecutionService,
) : me.konoplev.autocover.tools.Tool {

    private val logger = LoggerFactory.getLogger(CommandExecutionTool::class.java)

    @Tool("Execute a shell command in a specified directory")
    fun executeCommand(
        @P("The shell command to execute") command: String,
        @P("The working directory where the command should be executed") workingDirectory: String,
    ): String {
        return try {
            logger.debug("Executing command: {} in directory: {}", command, workingDirectory)

            val workingDir = File(workingDirectory)
            if (!workingDir.exists()) {
                return "Error: Working directory does not exist: $workingDirectory"
            }

            if (!workingDir.isDirectory) {
                return "Error: Path is not a directory: $workingDirectory"
            }

            val result = commandExecutionService.executeCommand(command, workingDir)

            if (result.exitCode == 0) {
                "✅ Command executed successfully!\n\nOutput:\n${result.output}"
            } else {
                "❌ Command failed with exit code ${result.exitCode}\n\n" +
                    "Error output:\n${result.errorOutput}\n\n" +
                    "Standard output:\n${result.output}"
            }
        } catch (e: Exception) {
            val errorMessage = "Error executing command '$command' in directory '$workingDirectory': ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }
}
