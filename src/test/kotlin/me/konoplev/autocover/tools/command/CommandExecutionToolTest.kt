package me.konoplev.autocover.tools.command

import me.konoplev.autocover.services.CommandExecutionService
import me.konoplev.autocover.services.CommandResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandExecutionToolTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `executeCommand should return error when working directory does not exist`() {
        // Given
        val commandExecutionService = CommandExecutionService()
        val commandExecutionTool = CommandExecutionTool(commandExecutionService)

        // When
        val result = commandExecutionTool.executeCommand("echo test", "/nonexistent/directory")

        // Then
        assertEquals("Error: Working directory does not exist: /nonexistent/directory", result)
    }

    @Test
    fun `executeCommand should return error when path is not a directory`() {
        // Given
        val commandExecutionService = CommandExecutionService()
        val commandExecutionTool = CommandExecutionTool(commandExecutionService)
        val tempFile = File(tempDir.toFile(), "not-a-directory.txt")
        tempFile.writeText("test")

        // When
        val result = commandExecutionTool.executeCommand("echo test", tempFile.absolutePath)

        // Then
        assertEquals("Error: Path is not a directory: ${tempFile.absolutePath}", result)
    }

    @Test
    fun `executeCommand should work with valid command and directory`() {
        // Given
        val commandExecutionService = CommandExecutionService()
        val commandExecutionTool = CommandExecutionTool(commandExecutionService)

        // When
        val result = commandExecutionTool.executeCommand("echo 'Hello World'", tempDir.toString())

        // Then
        assertTrue(result.contains("✅ Command executed successfully!"))
        assertTrue(result.contains("Hello World"))
    }
}
