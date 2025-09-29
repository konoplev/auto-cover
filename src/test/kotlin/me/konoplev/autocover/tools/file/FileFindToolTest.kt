package me.konoplev.autocover.tools.file

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileFindToolTest {

    private lateinit var fileFindTool: FileFindTool

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        fileFindTool = FileFindTool()
    }

    @Test
    fun `findFilesByName should find files matching name pattern`() {
        // Given: Create test files
        val testFile1 = File(tempDir.toFile(), "TestFile.txt")
        val testFile2 = File(tempDir.toFile(), "AnotherTest.kt")
        val testFile3 = File(tempDir.toFile(), "NoMatch.java")

        testFile1.writeText("content1")
        testFile2.writeText("content2")
        testFile3.writeText("content3")

        // When: Search for files containing "Test"
        val result = fileFindTool.findFilesByName(tempDir.toString(), "Test")

        // Then: Should find both TestFile.txt and AnotherTest.kt
        assertTrue(result.contains("Found 2 files"))
        assertTrue(result.contains("TestFile.txt"))
        assertTrue(result.contains("AnotherTest.kt"))
        assertFalse(result.contains("NoMatch.java"))
    }

    @Test
    fun `findFilesByName should be case insensitive`() {
        // Given: Create test file
        val testFile = File(tempDir.toFile(), "TestFile.txt")
        testFile.writeText("content")

        // When: Search with different case
        val result = fileFindTool.findFilesByName(tempDir.toString(), "testfile")

        // Then: Should find the file
        assertTrue(result.contains("Found 1 files"))
        assertTrue(result.contains("TestFile.txt"))
    }

    @Test
    fun `findFilesByName should return no results when no matches found`() {
        // Given: Create test file
        val testFile = File(tempDir.toFile(), "TestFile.txt")
        testFile.writeText("content")

        // When: Search for non-existing pattern
        val result = fileFindTool.findFilesByName(tempDir.toString(), "NonExistent")

        // Then: Should return no matches message
        assertTrue(result.contains("No files found matching pattern 'NonExistent'"))
    }

    @Test
    fun `findFilesByName should handle non-existent directory`() {
        // When: Search in non-existent directory
        val result = fileFindTool.findFilesByName("/non/existent/path", "test")

        // Then: Should return error message
        assertTrue(result.startsWith("Error: Directory does not exist"))
    }

    @Test
    fun `findFilesByName should handle file path instead of directory`() {
        // Given: Create test file
        val testFile = File(tempDir.toFile(), "TestFile.txt")
        testFile.writeText("content")

        // When: Pass file path instead of directory
        val result = fileFindTool.findFilesByName(testFile.absolutePath, "test")

        // Then: Should return error message
        assertTrue(result.startsWith("Error: Path is not a directory"))
    }

    @Test
    fun `findFilesByName should search recursively in subdirectories`() {
        // Given: Create nested directory structure
        val subDir = File(tempDir.toFile(), "subdir")
        subDir.mkdir()

        val testFile1 = File(tempDir.toFile(), "TestFile.txt")
        val testFile2 = File(subDir, "NestedTest.kt")

        testFile1.writeText("content1")
        testFile2.writeText("content2")

        // When: Search for files containing "Test"
        val result = fileFindTool.findFilesByName(tempDir.toString(), "Test")

        // Then: Should find both files
        assertTrue(result.contains("Found 2 files"))
        assertTrue(result.contains("TestFile.txt"))
        assertTrue(result.contains("NestedTest.kt"))
    }

    @Test
    fun `findFilesByExtension should find files with specific extension`() {
        // Given: Create test files with different extensions
        val ktFile = File(tempDir.toFile(), "Test.kt")
        val javaFile = File(tempDir.toFile(), "Test.java")
        val txtFile = File(tempDir.toFile(), "Test.txt")

        ktFile.writeText("kotlin content")
        javaFile.writeText("java content")
        txtFile.writeText("text content")

        // When: Search for .kt files
        val result = fileFindTool.findFilesByExtension(tempDir.toString(), "kt")

        // Then: Should find only .kt file
        assertTrue(result.contains("Found 1 files"))
        assertTrue(result.contains("Test.kt"))
        assertFalse(result.contains("Test.java"))
        assertFalse(result.contains("Test.txt"))
    }

    @Test
    fun `findFilesByExtension should handle extension with dot prefix`() {
        // Given: Create test file
        val ktFile = File(tempDir.toFile(), "Test.kt")
        ktFile.writeText("kotlin content")

        // When: Search with dot prefix
        val result = fileFindTool.findFilesByExtension(tempDir.toString(), ".kt")

        // Then: Should find the file
        assertTrue(result.contains("Found 1 files"))
        assertTrue(result.contains("Test.kt"))
    }

    @Test
    fun `findFilesByExtension should be case insensitive`() {
        // Given: Create test file
        val ktFile = File(tempDir.toFile(), "Test.KT")
        ktFile.writeText("kotlin content")

        // When: Search with lowercase extension
        val result = fileFindTool.findFilesByExtension(tempDir.toString(), "kt")

        // Then: Should find the file
        assertTrue(result.contains("Found 1 files"))
        assertTrue(result.contains("Test.KT"))
    }

    @Test
    fun `findFilesByExtension should return no results when no matches found`() {
        // Given: Create test file with different extension
        val txtFile = File(tempDir.toFile(), "Test.txt")
        txtFile.writeText("content")

        // When: Search for non-existing extension
        val result = fileFindTool.findFilesByExtension(tempDir.toString(), "kt")

        // Then: Should return no matches message
        assertTrue(result.contains("No files found with extension '.kt'"))
    }

    @Test
    fun `findFilesByExtension should handle non-existent directory`() {
        // When: Search in non-existent directory
        val result = fileFindTool.findFilesByExtension("/non/existent/path", "kt")

        // Then: Should return error message
        assertTrue(result.startsWith("Error: Directory does not exist"))
    }

    @Test
    fun `searchInFiles should find files containing text`() {
        // Given: Create test files with different content
        val file1 = File(tempDir.toFile(), "file1.txt")
        val file2 = File(tempDir.toFile(), "file2.txt")
        val file3 = File(tempDir.toFile(), "file3.txt")

        file1.writeText("This contains searchable text")
        file2.writeText("Another file with searchable content")
        file3.writeText("No matching content here")

        // When: Search for "searchable"
        val result = fileFindTool.searchInFiles(tempDir.toString(), "searchable")

        // Then: Should find both file1 and file2
        assertTrue(result.contains("Found 2 files"))
        assertTrue(result.contains("file1.txt"))
        assertTrue(result.contains("file2.txt"))
        assertFalse(result.contains("file3.txt"))
    }

    @Test
    fun `searchInFiles should be case insensitive`() {
        // Given: Create test file
        val file = File(tempDir.toFile(), "test.txt")
        file.writeText("This contains SEARCHABLE text")

        // When: Search with different case
        val result = fileFindTool.searchInFiles(tempDir.toString(), "searchable")

        // Then: Should find the file
        assertTrue(result.contains("Found 1 files"))
        assertTrue(result.contains("test.txt"))
    }

    @Test
    fun `searchInFiles should filter by file extension when specified`() {
        // Given: Create test files with different extensions
        val ktFile = File(tempDir.toFile(), "test.kt")
        val txtFile = File(tempDir.toFile(), "test.txt")

        ktFile.writeText("This contains searchable text")
        txtFile.writeText("This also contains searchable text")

        // When: Search only in .kt files
        val result = fileFindTool.searchInFiles(tempDir.toString(), "searchable", "kt")

        // Then: Should find only .kt file
        assertTrue(result.contains("Found 1 files"))
        assertTrue(result.contains("test.kt"))
        assertFalse(result.contains("test.txt"))
    }

    @Test
    fun `searchInFiles should handle extension with dot prefix`() {
        // Given: Create test file
        val ktFile = File(tempDir.toFile(), "test.kt")
        ktFile.writeText("This contains searchable text")

        // When: Search with dot prefix
        val result = fileFindTool.searchInFiles(tempDir.toString(), "searchable", ".kt")

        // Then: Should find the file
        assertTrue(result.contains("Found 1 files"))
        assertTrue(result.contains("test.kt"))
    }

    @Test
    fun `searchInFiles should return no results when no matches found`() {
        // Given: Create test file
        val file = File(tempDir.toFile(), "test.txt")
        file.writeText("This contains some content")

        // When: Search for non-existing text
        val result = fileFindTool.searchInFiles(tempDir.toString(), "nonexistent")

        // Then: Should return no matches message
        assertTrue(result.contains("No files found containing text 'nonexistent'"))
    }

    @Test
    fun `searchInFiles should handle non-existent directory`() {
        // When: Search in non-existent directory
        val result = fileFindTool.searchInFiles("/non/existent/path", "test")

        // Then: Should return error message
        assertTrue(result.startsWith("Error: Directory does not exist"))
    }

    @Test
    fun `searchInFiles should handle unreadable files gracefully`() {
        // Given: Create binary file that might cause issues
        val binaryFile = File(tempDir.toFile(), "binary.bin")
        binaryFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03))

        val textFile = File(tempDir.toFile(), "text.txt")
        textFile.writeText("This contains searchable text")

        // When: Search for text
        val result = fileFindTool.searchInFiles(tempDir.toString(), "searchable")

        // Then: Should find the text file and handle binary file gracefully
        assertTrue(result.contains("Found 1 files"))
        assertTrue(result.contains("text.txt"))
    }

    @Test
    fun `searchInFiles should search recursively in subdirectories`() {
        // Given: Create nested directory structure
        val subDir = File(tempDir.toFile(), "subdir")
        subDir.mkdir()

        val file1 = File(tempDir.toFile(), "file1.txt")
        val file2 = File(subDir, "file2.txt")

        file1.writeText("This contains searchable text")
        file2.writeText("This also contains searchable text")

        // When: Search for text
        val result = fileFindTool.searchInFiles(tempDir.toString(), "searchable")

        // Then: Should find both files
        assertTrue(result.contains("Found 2 files"))
        assertTrue(result.contains("file1.txt"))
        assertTrue(result.contains("file2.txt"))
    }
}
