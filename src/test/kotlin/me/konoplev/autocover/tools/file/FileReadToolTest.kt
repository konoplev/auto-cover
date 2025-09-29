package me.konoplev.autocover.tools.file

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileReadToolTest {

    private lateinit var fileReadTool: FileReadTool

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        fileReadTool = FileReadTool()
    }

    @Test
    fun `readFile should return file content when file exists and is readable`() {
        // Given: Create a test file with content
        val testFile = File(tempDir.toFile(), "test.txt")
        val expectedContent = "This is test content\nwith multiple lines\nand some special characters: !@#$%"
        testFile.writeText(expectedContent)

        // When: Read the file
        val result = fileReadTool.readFile(testFile.absolutePath)

        // Then: Should return the exact content
        assertEquals(expectedContent, result)
    }

    @Test
    fun `readFile should handle empty files`() {
        // Given: Create an empty file
        val testFile = File(tempDir.toFile(), "empty.txt")
        testFile.createNewFile()

        // When: Read the empty file
        val result = fileReadTool.readFile(testFile.absolutePath)

        // Then: Should return empty string
        assertEquals("", result)
    }

    @Test
    fun `readFile should handle files with UTF-8 content`() {
        // Given: Create a file with UTF-8 characters
        val testFile = File(tempDir.toFile(), "utf8.txt")
        val expectedContent = "Hello 世界! 🌍 Café naïve résumé"
        testFile.writeText(expectedContent, Charsets.UTF_8)

        // When: Read the file
        val result = fileReadTool.readFile(testFile.absolutePath)

        // Then: Should return the UTF-8 content correctly
        assertEquals(expectedContent, result)
    }

    @Test
    fun `readFile should return error message for non-existent file`() {
        // Given: A non-existent file path
        val nonExistentPath = File(tempDir.toFile(), "nonexistent.txt").absolutePath

        // When: Try to read non-existent file
        val result = fileReadTool.readFile(nonExistentPath)

        // Then: Should return error message
        assertTrue(result.startsWith("Error: File does not exist"))
        assertTrue(result.contains(nonExistentPath))
    }

    @Test
    fun `readFile should return error message when path is a directory`() {
        // Given: A directory path
        val subDir = File(tempDir.toFile(), "subdir")
        subDir.mkdir()

        // When: Try to read directory as file
        val result = fileReadTool.readFile(subDir.absolutePath)

        // Then: Should return error message (Files.readString will throw exception for directory)
        assertTrue(result.startsWith("Error reading file"))
    }

    // This test might not work on all systems (Windows doesn't support POSIX permissions)
    @Test
    fun `readFile should handle permission errors gracefully`() {
        // Given: Create a file and attempt to make it unreadable
        val testFile = File(tempDir.toFile(), "restricted.txt")
        testFile.writeText("secret content")

        try {
            // Try to remove read permissions (POSIX systems only)
            val path = testFile.toPath()
            if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
                val permissions = Files.getPosixFilePermissions(path).toMutableSet()
                permissions.remove(PosixFilePermission.OWNER_READ)
                permissions.remove(PosixFilePermission.GROUP_READ)
                permissions.remove(PosixFilePermission.OTHERS_READ)
                Files.setPosixFilePermissions(path, permissions)

                // When: Try to read the unreadable file
                val result = fileReadTool.readFile(testFile.absolutePath)

                // Then: Should return error message for unreadable file
                assertTrue(result.startsWith("Error: File is not readable") || result.startsWith("Error reading file"))

                // Restore permissions for cleanup
                permissions.add(PosixFilePermission.OWNER_READ)
                Files.setPosixFilePermissions(path, permissions)
            }
        } catch (e: Exception) {
            // If permission manipulation fails, just verify the file can be read normally
            val result = fileReadTool.readFile(testFile.absolutePath)
            assertEquals("secret content", result)
        }
    }

    @Test
    fun `readFile should handle absolute paths correctly`() {
        // Given: Create a test file
        val testFile = File(tempDir.toFile(), "absolute.txt")
        val expectedContent = "absolute path content"
        testFile.writeText(expectedContent)

        // When: Use absolute path
        val result = fileReadTool.readFile(testFile.absolutePath)

        // Then: Should read the file successfully
        assertEquals(expectedContent, result)
    }

    @Test
    fun `readFile should handle files with BOM (Byte Order Mark)`() {
        // Given: Create a file with UTF-8 BOM
        val testFile = File(tempDir.toFile(), "bom.txt")
        val content = "Content with BOM"
        // UTF-8 BOM is EF BB BF
        val bomBytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val contentBytes = content.toByteArray(Charsets.UTF_8)
        testFile.writeBytes(bomBytes + contentBytes)

        // When: Read the file
        val result = fileReadTool.readFile(testFile.absolutePath)

        // Then: Should handle BOM correctly (Java typically handles this automatically)
        // The result might or might not include the BOM depending on Java's handling
        assertTrue(result.contains("Content with BOM"))
    }

    @Test
    fun `readFile should handle Windows-style line endings`() {
        // Given: Create a file with Windows line endings (CRLF)
        val testFile = File(tempDir.toFile(), "windows.txt")
        val windowsContent = "Line 1\r\nLine 2\r\nLine 3\r\n"
        testFile.writeBytes(windowsContent.toByteArray())

        // When: Read the file
        val result = fileReadTool.readFile(testFile.absolutePath)

        // Then: Should return content with line endings preserved
        assertEquals(windowsContent, result)
    }

    @Test
    fun `readFile should handle mixed line endings`() {
        // Given: Create a file with mixed line endings
        val testFile = File(tempDir.toFile(), "mixed-endings.txt")
        val mixedContent = "Unix line\nWindows line\r\nOld Mac line\rAnother Unix line\n"
        testFile.writeBytes(mixedContent.toByteArray())

        // When: Read the file
        val result = fileReadTool.readFile(testFile.absolutePath)

        // Then: Should return content with all line endings preserved
        assertEquals(mixedContent, result)
    }
}
