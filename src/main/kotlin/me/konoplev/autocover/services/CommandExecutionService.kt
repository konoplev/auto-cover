package me.konoplev.autocover.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class CommandExecutionService {

    private val logger = LoggerFactory.getLogger(CommandExecutionService::class.java)

    /**
     * Executes a command and returns the result with output and error information.
     */
    fun executeCommand(command: String, workingDir: File): CommandResult {
        logger.debug("Executing command: {} in directory: {}", command, workingDir.absolutePath)

        val processBuilder = ProcessBuilder(listOf("bash", "-lc", command))
            .directory(workingDir)
            .redirectErrorStream(false) // Keep error and output separate

        val process = processBuilder.start()

        val output = process.inputStream.bufferedReader().readText()
        val errorOutput = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        logger.debug("Command completed with exit code: {}", exitCode)

        return CommandResult(exitCode, output, errorOutput)
    }
}
