package me.konoplev.autocover.tools.file

import me.konoplev.autocover.services.FileSystemTransactionManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FileWriteToolTest {

    private lateinit var fileWriteTool: FileWriteTool

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        fileWriteTool = FileWriteTool(FileSystemTransactionManager())
    }

    @Test
    fun `writeFile should create new file with content`() {
        // Given: A file path that doesn't exist
        val testFile = File(tempDir.toFile(), "new-file.txt")
        val content = "This is new content"

        // When: Write content to the file
        val result = fileWriteTool.writeFile(testFile.absolutePath, content)

        // Then: Should create file and write content successfully
        assertTrue(result.startsWith("Successfully wrote content to file"))
        assertTrue(testFile.exists())
        assertEquals(content, testFile.readText())
    }

    @Test
    fun `writeFile should overwrite existing file content`() {
        // Given: An existing file with content
        val testFile = File(tempDir.toFile(), "existing-file.txt")
        testFile.writeText("Original content")

        val newContent = "Overwritten content"

        // When: Write new content to the file
        val result = fileWriteTool.writeFile(testFile.absolutePath, newContent)

        // Then: Should overwrite the file content
        assertTrue(result.startsWith("Successfully wrote content to file"))
        assertEquals(newContent, testFile.readText())
        assertNotEquals("Original content", testFile.readText())
    }

    @Test
    fun `writeFile should handle UTF-8 content with special characters`() {
        // Given: Content with UTF-8 characters
        val testFile = File(tempDir.toFile(), "utf8-file.txt")
        val utf8Content = "Hello 世界! 🌍 Café naïve résumé"

        // When: Write UTF-8 content
        val result = fileWriteTool.writeFile(testFile.absolutePath, utf8Content)

        // Then: Should write UTF-8 content correctly
        assertTrue(result.startsWith("Successfully wrote content to file"))
        assertEquals(utf8Content, testFile.readText())
    }

    @Test
    fun `writeFile should handle multiline content`() {
        // Given: Multiline content
        val testFile = File(tempDir.toFile(), "multiline.txt")
        val multilineContent = """
            Line 1
            Line 2 with spaces

            Line 4 after empty line
            Final line
        """.trimIndent()

        // When: Write multiline content
        val result = fileWriteTool.writeFile(testFile.absolutePath, multilineContent)

        // Then: Should preserve all lines and formatting
        assertTrue(result.startsWith("Successfully wrote content to file"))
        assertEquals(multilineContent, testFile.readText())
    }

    @Test
    fun `writeFile should create parent directories when they don't exist`() {
        // Given: A nested file path where parent directories don't exist
        val nestedFile = File(tempDir.toFile(), "nested/deep/path/file.txt")
        val content = "Content in nested directory"

        // When: Write to nested file path
        val result = fileWriteTool.writeFile(nestedFile.absolutePath, content)

        // Then: Should create parent directories and file
        assertTrue(result.startsWith("Successfully wrote content to file"))
        assertTrue(nestedFile.exists())
        assertTrue(nestedFile.parentFile.exists())
        assertEquals(content, nestedFile.readText())
    }

    @Test
    fun `writeFile should handle different line endings`() {
        // Given: Content with different line endings
        val testFile = File(tempDir.toFile(), "line-endings.txt")
        val contentWithLineEndings = "Unix line\nWindows line\r\nOld Mac line\rMixed content\n"

        // When: Write content with mixed line endings
        val result = fileWriteTool.writeFile(testFile.absolutePath, contentWithLineEndings)

        // Then: Should preserve all line endings
        assertTrue(result.startsWith("Successfully wrote content to file"))
        assertEquals(contentWithLineEndings, testFile.readText())
    }

    @Test
    fun `appendToFile should add content to new file`() {
        // Given: A file path that doesn't exist
        val testFile = File(tempDir.toFile(), "new-append-file.txt")
        val content = "First line of content"

        // When: Append to non-existing file
        val result = fileWriteTool.appendToFile(testFile.absolutePath, content)

        // Then: Should create file with appended content
        assertTrue(result.startsWith("Successfully appended content to file"))
        assertTrue(testFile.exists())
        assertEquals(content, testFile.readText())
    }

    @Test
    fun `appendToFile should add content to existing file`() {
        // Given: An existing file with content
        val testFile = File(tempDir.toFile(), "existing-append.txt")
        val originalContent = "Original content"
        val appendedContent = "\nAppended content"
        testFile.writeText(originalContent)

        // When: Append new content
        val result = fileWriteTool.appendToFile(testFile.absolutePath, appendedContent)

        // Then: Should append to existing content
        assertTrue(result.startsWith("Successfully appended content to file"))
        assertEquals(originalContent + appendedContent, testFile.readText())
    }

    @Test
    fun `appendToFile should handle multiple appends`() {
        // Given: A file path
        val testFile = File(tempDir.toFile(), "multiple-appends.txt")

        // When: Append multiple times
        fileWriteTool.appendToFile(testFile.absolutePath, "Line 1")
        fileWriteTool.appendToFile(testFile.absolutePath, "\nLine 2")
        val result = fileWriteTool.appendToFile(testFile.absolutePath, "\nLine 3")

        // Then: Should contain all appended content
        assertTrue(result.startsWith("Successfully appended content to file"))
        assertEquals("Line 1\nLine 2\nLine 3", testFile.readText())
    }

    @Test
    fun `appendToFile should create parent directories when they don't exist`() {
        // Given: A nested file path where parent directories don't exist
        val nestedFile = File(tempDir.toFile(), "append/nested/path/file.txt")
        val content = "Appended content in nested directory"

        // When: Append to nested file path
        val result = fileWriteTool.appendToFile(nestedFile.absolutePath, content)

        // Then: Should create parent directories and append content
        assertTrue(result.startsWith("Successfully appended content to file"))
        assertTrue(nestedFile.exists())
        assertTrue(nestedFile.parentFile.exists())
        assertEquals(content, nestedFile.readText())
    }

    @Test
    fun `appendToFile should handle empty content`() {
        // Given: An existing file and empty content
        val testFile = File(tempDir.toFile(), "append-empty.txt")
        val originalContent = "Original content"
        testFile.writeText(originalContent)

        // When: Append empty content
        val result = fileWriteTool.appendToFile(testFile.absolutePath, "")

        // Then: Should not change the file content
        assertTrue(result.startsWith("Successfully appended content to file"))
        assertEquals(originalContent, testFile.readText())
    }

    @Test
    fun `appendToFile should handle UTF-8 content`() {
        // Given: An existing file and UTF-8 content to append
        val testFile = File(tempDir.toFile(), "append-utf8.txt")
        val originalContent = "Original: Hello"
        val appendContent = " 世界! 🌍"
        testFile.writeText(originalContent)

        // When: Append UTF-8 content
        val result = fileWriteTool.appendToFile(testFile.absolutePath, appendContent)

        // Then: Should append UTF-8 content correctly
        assertTrue(result.startsWith("Successfully appended content to file"))
        assertEquals(originalContent + appendContent, testFile.readText())
    }

    @Test
    fun `writeFile and appendToFile should work together correctly`() {
        // Given: A file path
        val testFile = File(tempDir.toFile(), "write-then-append.txt")
        val initialContent = "Initial content"
        val appendedContent1 = "\nFirst append"
        val appendedContent2 = "\nSecond append"

        // When: Write then append multiple times
        fileWriteTool.writeFile(testFile.absolutePath, initialContent)
        fileWriteTool.appendToFile(testFile.absolutePath, appendedContent1)
        val result = fileWriteTool.appendToFile(testFile.absolutePath, appendedContent2)

        // Then: Should have all content in order
        assertTrue(result.startsWith("Successfully appended content to file"))
        assertEquals(initialContent + appendedContent1 + appendedContent2, testFile.readText())
    }

    @Test
    fun `writeFile should truncate existing content completely`() {
        // Given: An existing file with substantial content
        val testFile = File(tempDir.toFile(), "truncate-test.txt")
        val longOriginalContent = "Very long original content ".repeat(100)
        val shortNewContent = "Short"
        testFile.writeText(longOriginalContent)

        // When: Write shorter content
        val result = fileWriteTool.writeFile(testFile.absolutePath, shortNewContent)

        // Then: Should completely replace with new content
        assertTrue(result.startsWith("Successfully wrote content to file"))
        assertEquals(shortNewContent, testFile.readText())
        assertEquals(5, testFile.readText().length) // "Short" is 5 characters
    }

    @Test
    fun `writeFile should handle content with tabs and various whitespace`() {
        // Given: Content with different types of whitespace
        val testFile = File(tempDir.toFile(), "whitespace.txt")
        val whitespaceContent = "\t\tIndented with tabs\n    Indented with spaces\n\nEmpty line above"

        // When: Write whitespace content
        val result = fileWriteTool.writeFile(testFile.absolutePath, whitespaceContent)

        // Then: Should preserve all whitespace
        assertTrue(result.startsWith("Successfully wrote content to file"))
        assertEquals(whitespaceContent, testFile.readText())
    }

    @Test
    fun `writeFile should handle invalid file path gracefully`() {
        // Given: An invalid file path (contains illegal characters for most filesystems)
        val invalidPath = if (System.getProperty("os.name").lowercase().contains("windows")) {
            tempDir.toString() + "\\invalid|file<name>.txt"
        } else {
            tempDir.toString() + "/\u0000invalid"
        }

        val content = "Test content"

        // When: Try to write to invalid path
        val result = fileWriteTool.writeFile(invalidPath, content)

        // Then: Should return error message
        assertTrue(result.startsWith("Error writing to file"))
    }

    @Test
    fun `appendToFile should handle invalid file path gracefully`() {
        // Given: An invalid file path
        val invalidPath = if (System.getProperty("os.name").lowercase().contains("windows")) {
            tempDir.toString() + "\\invalid|append<file>.txt"
        } else {
            tempDir.toString() + "/\u0000invalid_append"
        }

        val content = "Test content"

        // When: Try to append to invalid path
        val result = fileWriteTool.appendToFile(invalidPath, content)

        // Then: Should return error message
        assertTrue(result.startsWith("Error appending to file"))
    }
}
