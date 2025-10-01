package me.konoplev.autocover.tools.file

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import kotlin.io.path.absolutePathString
import me.konoplev.autocover.services.FileSystemTransactionManager
import me.konoplev.autocover.tools.FileSystemTool
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@Component
class FileWriteTool(
    private val transactionManager: FileSystemTransactionManager,
) : FileSystemTool {

    private val logger = LoggerFactory.getLogger(FileWriteTool::class.java)

    @Tool("Write content to a file")
    fun writeFile(
        @P("The path to the file to write") filePath: String,
        @P("The content to write to the file") content: String,
    ): String {
        return try {
            logger.debug("Writing to file: {}", filePath)

            val file = File(filePath)
            val fileExistedBefore = file.exists()

            // Track in transaction (manager will handle whether a transaction is active)
            if (fileExistedBefore) {
                // Check if this file was created during the transaction
                if (transactionManager.wasFileCreatedDuringTransaction(filePath)) {
                    // File was created during transaction, don't create backup
                    logger.debug("Skipping backup for file created during transaction: {}", filePath)
                } else {
                    // File was pre-existing, create backup
                    transactionManager.trackPreExistingFile(filePath)
                    transactionManager.trackFileModification(filePath)
                }
            }

            val path = Paths.get(filePath)

            // Create parent directories if they don't exist
            path.parent?.let { parentPath ->
                if (!Files.exists(parentPath)) {
                    Files.createDirectories(parentPath)
                    transactionManager.trackDirectoryCreation(parentPath.absolutePathString())
                    logger.debug("Created parent directories for: {}", filePath)
                } else {
                    // Track pre-existing parent directories
                    if (!transactionManager.isDirectoryPreExisting(parentPath.absolutePathString())) {
                        transactionManager.trackPreExistingDirectory(parentPath.absolutePathString())
                    }
                }
            }

            // Process content to handle escape sequences properly
            val processedContent = processContent(content)
            Files.writeString(path, processedContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

            // Track file creation in transaction if the file didn't exist before
            if (!fileExistedBefore) {
                transactionManager.trackFileCreation(filePath)
            }

            logger.debug("Successfully wrote {} characters to file: {}", content.length, filePath)
            "Successfully wrote content to file: $filePath"
        } catch (e: Exception) {
            val errorMessage = "Error writing to file $filePath: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }

    @Tool("Append content to a file")
    fun appendToFile(
        @P("The path to the file to append to") filePath: String,
        @P("The content to append to the file") content: String,
    ): String {
        return try {
            logger.debug("Appending to file: {}", filePath)

            // Track in transaction if active
            val file = File(filePath)
            if (file.exists()) {
                // Check if this file was created during the transaction
                if (transactionManager.wasFileCreatedDuringTransaction(filePath)) {
                    // File was created during transaction, don't create backup
                    logger.debug("Skipping backup for file created during transaction: {}", filePath)
                } else {
                    // File was pre-existing, create backup
                    transactionManager.trackPreExistingFile(filePath)
                    transactionManager.trackFileModification(filePath)
                }
            }

            val path = Paths.get(filePath)

            // Create parent directories if they don't exist
            path.parent?.let { parentPath ->
                if (!Files.exists(parentPath)) {
                    Files.createDirectories(parentPath)
                    transactionManager.trackDirectoryCreation(parentPath.absolutePathString())
                    logger.debug("Created parent directories for: {}", filePath)
                } else {
                    // Track pre-existing parent directories
                    if (!transactionManager.isDirectoryPreExisting(parentPath.absolutePathString())) {
                        transactionManager.trackPreExistingDirectory(parentPath.absolutePathString())
                    }
                }
            }

            // Process content to handle escape sequences properly
            val processedContent = processContent(content)
            Files.writeString(path, processedContent, StandardOpenOption.CREATE, StandardOpenOption.APPEND)

            // Track file creation in transaction if active and file didn't exist before
            if (!File(filePath).exists()) {
                transactionManager.trackFileCreation(filePath)
            }

            logger.debug("Successfully appended {} characters to file: {}", content.length, filePath)
            "Successfully appended content to file: $filePath"
        } catch (e: Exception) {
            val errorMessage = "Error appending to file $filePath: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }

    @Tool("Delete a file")
    fun deleteFile(@P("The path to the file to delete") filePath: String): String {
        return try {
            logger.debug("Deleting file: {}", filePath)

            val file = File(filePath)

            if (!file.exists()) {
                return "Error: File does not exist at path: $filePath"
            }

            if (!file.isFile()) {
                return "Error: Path is not a file: $filePath"
            }

            // Track deletion in transaction if active
            transactionManager.trackItemDeletion(filePath)

            // Delete the actual file
            val deleted = file.delete()
            if (deleted) {
                logger.debug("Successfully deleted file: {}", filePath)
                "Successfully deleted file: $filePath"
            } else {
                return "Error: Failed to delete file: $filePath"
            }
        } catch (e: Exception) {
            val errorMessage = "Error deleting file $filePath: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }

    /**
     * Process content to handle escape sequences properly.
     * Converts literal escape sequences like \n, \t, etc. to actual characters.
     * Only processes literal backslash escape sequences, not actual line ending characters.
     */
    private fun processContent(content: String): String {
        logger.debug("Processing content with {} characters", content.length)

        val processed = content
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\r", "\r")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            // Handle any remaining backslashes that might be causing issues
            .replace(Regex("\\\\$"), "") // Remove trailing backslashes

        logger.debug("Processed content has {} characters", processed.length)
        return processed
    }
}
