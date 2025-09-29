package me.konoplev.autocover.tools.directory

import dev.langchain4j.agent.tool.Tool
import me.konoplev.autocover.tools.FileSystemTool
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

@Component
class DirectoryListTool : FileSystemTool {

    private val logger = LoggerFactory.getLogger(DirectoryListTool::class.java)

    @Tool("List contents of a directory")
    fun listDirectory(directoryPath: String): String {
        return try {
            logger.debug("Listing contents of directory: {}", directoryPath)

            val path = Paths.get(directoryPath)
            if (!Files.exists(path)) {
                return "Error: Directory does not exist at path: $directoryPath"
            }

            if (!Files.isDirectory(path)) {
                return "Error: Path is not a directory: $directoryPath"
            }

            val contents = Files.list(path)
                .map { file ->
                    val name = file.fileName.toString()
                    val type = if (Files.isDirectory(file)) "DIR" else "FILE"
                    val size = if (Files.isRegularFile(file)) {
                        try {
                            " (${Files.size(file)} bytes)"
                        } catch (e: Exception) {
                            " (size unknown)"
                        }
                    } else {
                        ""
                    }
                    "[$type] $name$size"
                }
                .sorted()
                .toList()

            if (contents.isEmpty()) {
                "Directory is empty: $directoryPath"
            } else {
                logger.debug("Found {} items in directory: {}", contents.size, directoryPath)
                "Directory contents for $directoryPath:\n" + contents.joinToString("\n")
            }
        } catch (e: Exception) {
            val errorMessage = "Error listing directory $directoryPath: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }

    @Tool("List only files in a directory")
    fun listFiles(directoryPath: String): String {
        return try {
            logger.debug("Listing files in directory: {}", directoryPath)

            val path = Paths.get(directoryPath)
            if (!Files.exists(path)) {
                return "Error: Directory does not exist at path: $directoryPath"
            }

            if (!Files.isDirectory(path)) {
                return "Error: Path is not a directory: $directoryPath"
            }

            val files = Files.list(path)
                .filter { Files.isRegularFile(it) }
                .map { file ->
                    val name = file.fileName.toString()
                    val size = try {
                        " (${Files.size(file)} bytes)"
                    } catch (e: Exception) {
                        " (size unknown)"
                    }
                    "$name$size"
                }
                .sorted()
                .toList()

            if (files.isEmpty()) {
                "No files found in directory: $directoryPath"
            } else {
                logger.debug("Found {} files in directory: {}", files.size, directoryPath)
                "Files in $directoryPath:\n" + files.joinToString("\n")
            }
        } catch (e: Exception) {
            val errorMessage = "Error listing files in $directoryPath: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }

    @Tool("List only subdirectories in a directory")
    fun listDirectories(directoryPath: String): String {
        return try {
            logger.debug("Listing subdirectories in: {}", directoryPath)

            val path = Paths.get(directoryPath)
            if (!Files.exists(path)) {
                return "Error: Directory does not exist at path: $directoryPath"
            }

            if (!Files.isDirectory(path)) {
                return "Error: Path is not a directory: $directoryPath"
            }

            val directories = Files.list(path)
                .filter { Files.isDirectory(it) }
                .map { it.fileName.toString() }
                .sorted()
                .toList()

            if (directories.isEmpty()) {
                "No subdirectories found in: $directoryPath"
            } else {
                logger.debug("Found {} subdirectories in: {}", directories.size, directoryPath)
                "Subdirectories in $directoryPath:\n" + directories.joinToString("\n")
            }
        } catch (e: Exception) {
            val errorMessage = "Error listing subdirectories in $directoryPath: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }
}
