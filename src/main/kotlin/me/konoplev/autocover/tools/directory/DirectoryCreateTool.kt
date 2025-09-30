package me.konoplev.autocover.tools.directory

import dev.langchain4j.agent.tool.Tool
import me.konoplev.autocover.services.FileSystemTransactionManager
import me.konoplev.autocover.tools.FileSystemTool
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@Component
class DirectoryCreateTool(
    private val transactionManager: FileSystemTransactionManager,
) : FileSystemTool {

    private val logger = LoggerFactory.getLogger(DirectoryCreateTool::class.java)

    @Tool("Create a new directory")
    fun createDirectory(directoryPath: String): String {
        return try {
            logger.debug("Creating directory: {}", directoryPath)

            val path = Paths.get(directoryPath)

            if (Files.exists(path)) {
                if (Files.isDirectory(path)) {
                    // Track pre-existing directory
                    val dirLastModified = File(directoryPath).lastModified()
                    val transactionStartTime = transactionManager.getTransactionStartTime()
                    val isPreExisting = transactionStartTime != null && dirLastModified < transactionStartTime
                    
                    if (isPreExisting) {
                        transactionManager.trackPreExistingDirectory(directoryPath)
                    }
                    return "Directory already exists: $directoryPath"
                } else {
                    return "Error: A file already exists at path: $directoryPath"
                }
            }

            Files.createDirectory(path)

            // Track directory creation in transaction if active
            transactionManager.trackDirectoryCreation(directoryPath)

            logger.debug("Successfully created directory: {}", directoryPath)
            "Successfully created directory: $directoryPath"
        } catch (e: Exception) {
            val errorMessage = "Error creating directory $directoryPath: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }

    @Tool("Create a new directory and all parent directories if they don't exist")
    fun createDirectories(directoryPath: String): String {
        return try {
            logger.debug("Creating directories (including parents): {}", directoryPath)

            val path = Paths.get(directoryPath)

            if (Files.exists(path)) {
                if (Files.isDirectory(path)) {
                    // Track pre-existing directory
                    val dirLastModified = File(directoryPath).lastModified()
                    val transactionStartTime = transactionManager.getTransactionStartTime()
                    val isPreExisting = transactionStartTime != null && dirLastModified < transactionStartTime
                    
                    if (isPreExisting) {
                        transactionManager.trackPreExistingDirectory(directoryPath)
                    }
                    return "Directory already exists: $directoryPath"
                } else {
                    return "Error: A file already exists at path: $directoryPath"
                }
            }

            // Track which directories need to be created for transaction
            val pathsToCreate = mutableListOf<String>()
            var currentPath = File(directoryPath)
            // Build list of directories that don't exist (from deepest to shallowest)
            while (!currentPath.exists() && currentPath.parent != null) {
                pathsToCreate.add(0, currentPath.absolutePath) // Add at beginning to maintain order
                currentPath = currentPath.parentFile
            }

            Files.createDirectories(path)

            // Track all created directories in transaction if active
            pathsToCreate.forEach { createdPath ->
                transactionManager.trackDirectoryCreation(createdPath)
            }

            logger.debug("Successfully created directories: {}", directoryPath)
            "Successfully created directories: $directoryPath"
        } catch (e: Exception) {
            val errorMessage = "Error creating directories $directoryPath: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }

    @Tool("Delete an empty directory")
    fun deleteDirectory(directoryPath: String): String {
        return try {
            logger.debug("Deleting directory: {}", directoryPath)

            val path = Paths.get(directoryPath)

            if (!Files.exists(path)) {
                return "Error: Directory does not exist at path: $directoryPath"
            }

            if (!Files.isDirectory(path)) {
                return "Error: Path is not a directory: $directoryPath"
            }

            // Check if directory is empty
            Files.list(path).use { entries ->
                if (entries.findFirst().isPresent) {
                    return "Error: Directory is not empty: $directoryPath"
                }
            }

            transactionManager.trackItemDeletion(directoryPath)

            Files.delete(path)
            logger.debug("Successfully deleted directory: {}", directoryPath)
            "Successfully deleted directory: $directoryPath"
        } catch (e: Exception) {
            val errorMessage = "Error deleting directory $directoryPath: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }

    @Tool("Check if a directory exists")
    fun directoryExists(directoryPath: String): String {
        return try {
            val path = Paths.get(directoryPath)
            val exists = Files.exists(path) && Files.isDirectory(path)

            if (exists) {
                "Directory exists: $directoryPath"
            } else {
                "Directory does not exist: $directoryPath"
            }
        } catch (e: Exception) {
            val errorMessage = "Error checking directory existence $directoryPath: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }
}
