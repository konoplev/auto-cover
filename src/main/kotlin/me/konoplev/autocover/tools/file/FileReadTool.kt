package me.konoplev.autocover.tools.file

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import me.konoplev.autocover.tools.FileSystemTool
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths

@Component
class FileReadTool : FileSystemTool {

    private val logger = LoggerFactory.getLogger(FileReadTool::class.java)

    @Tool("Read the contents of a file")
    fun readFile(@P("The path to the file to read") filePath: String): String {
        return try {
            logger.debug("Reading file: {}", filePath)

            val path = Paths.get(filePath)
            if (!Files.exists(path)) {
                "Error: File does not exist at path: $filePath"
            } else if (!Files.isReadable(path)) {
                "Error: File is not readable at path: $filePath"
            } else {
                val content = Files.readString(path)
                logger.debug("Successfully read {} characters from file: {}", content.length, filePath)
                content
            }
        } catch (e: Exception) {
            val errorMessage = "Error reading file $filePath: ${e.message}"
            logger.error(errorMessage, e)
            errorMessage
        }
    }
}
