package me.konoplev.autocover.tools.file

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import me.konoplev.autocover.tools.FileSystemTool
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

@Component
class FileFindTool : FileSystemTool {

    private val logger = LoggerFactory.getLogger(FileFindTool::class.java)

    @Tool("Find files by name pattern in a directory")
    fun findFilesByName(
        @P("The directory path to search in") directoryPath: String,
        @P("The name pattern to search for") namePattern: String,
    ): String {
        return try {
            logger.debug("Finding files with pattern '{}' in directory: {}", namePattern, directoryPath)

            val path = Paths.get(directoryPath)
            if (!Files.exists(path)) {
                return "Error: Directory does not exist at path: $directoryPath"
            }

            if (!Files.isDirectory(path)) {
                return "Error: Path is not a directory: $directoryPath"
            }

            val matchingFiles = Files.walk(path)
                .filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString().contains(namePattern, ignoreCase = true) }
                .map { it.toString() }
                .toList()

            if (matchingFiles.isEmpty()) {
                "No files found matching pattern '$namePattern' in directory: $directoryPath"
            } else {
                logger.debug("Found {} files matching pattern '{}'", matchingFiles.size, namePattern)
                "Found ${matchingFiles.size} files:\n" + matchingFiles.joinToString("\n")
            }
        } catch (e: Exception) {
            val errorMessage = "Error finding files in $directoryPath: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }

    @Tool("Find files by extension in a directory")
    fun findFilesByExtension(
        @P("The directory path to search in") directoryPath: String,
        @P("The file extension to search for (e.g., 'java', 'kt', 'xml')") extension: String,
    ): String {
        return try {
            logger.debug("Finding files with extension '{}' in directory: {}", extension, directoryPath)

            val path = Paths.get(directoryPath)
            if (!Files.exists(path)) {
                return "Error: Directory does not exist at path: $directoryPath"
            }

            if (!Files.isDirectory(path)) {
                return "Error: Path is not a directory: $directoryPath"
            }

            val normalizedExtension = if (extension.startsWith(".")) extension else ".$extension"

            val matchingFiles = Files.walk(path)
                .filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString().endsWith(normalizedExtension, ignoreCase = true) }
                .map { it.toString() }
                .toList()

            if (matchingFiles.isEmpty()) {
                "No files found with extension '$normalizedExtension' in directory: $directoryPath"
            } else {
                logger.debug("Found {} files with extension '{}'", matchingFiles.size, normalizedExtension)
                "Found ${matchingFiles.size} files:\n" + matchingFiles.joinToString("\n")
            }
        } catch (e: Exception) {
            val errorMessage = "Error finding files in $directoryPath: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }

    @Tool("Search for text content within files in a directory")
    fun searchInFiles(
        @P("The directory path to search in") directoryPath: String,
        @P("The text to search for") searchText: String,
        @P("Optional file extension filter (e.g., 'java', 'kt')") fileExtension: String = "",
    ): String {
        return try {
            logger.debug("Searching for text '{}' in files in directory: {}", searchText, directoryPath)

            val path = Paths.get(directoryPath)
            if (!Files.exists(path)) {
                return "Error: Directory does not exist at path: $directoryPath"
            }

            if (!Files.isDirectory(path)) {
                return "Error: Path is not a directory: $directoryPath"
            }

            val matchingFiles = mutableListOf<String>()

            Files.walk(path)
                .filter { Files.isRegularFile(it) }
                .filter { file ->
                    if (fileExtension.isNotEmpty()) {
                        val normalizedExtension = if (fileExtension.startsWith(".")) fileExtension else ".$fileExtension"
                        file.fileName.toString().endsWith(normalizedExtension, ignoreCase = true)
                    } else {
                        true
                    }
                }
                .forEach { file ->
                    try {
                        val content = Files.readString(file)
                        if (content.contains(searchText, ignoreCase = true)) {
                            matchingFiles.add(file.toString())
                        }
                    } catch (e: Exception) {
                        logger.warn("Could not read file: $file, error: ${e.message}")
                    }
                }

            if (matchingFiles.isEmpty()) {
                "No files found containing text '$searchText' in directory: $directoryPath"
            } else {
                logger.debug("Found {} files containing text '{}'", matchingFiles.size, searchText)
                "Found ${matchingFiles.size} files containing '$searchText':\n" + matchingFiles.joinToString("\n")
            }
        } catch (e: Exception) {
            val errorMessage = "Error searching for text in $directoryPath: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }
}
