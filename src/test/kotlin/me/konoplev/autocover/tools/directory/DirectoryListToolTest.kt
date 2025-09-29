package me.konoplev.autocover.tools.directory

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectoryListToolTest {

    private lateinit var directoryListTool: DirectoryListTool

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        directoryListTool = DirectoryListTool()
    }

    @Test
    fun `listDirectory should show both files and directories`() {
        // Given: Create mixed content in directory
        val file1 = File(tempDir.toFile(), "file1.txt")
        val file2 = File(tempDir.toFile(), "file2.kt")
        val subdir1 = File(tempDir.toFile(), "subdir1")
        val subdir2 = File(tempDir.toFile(), "subdir2")

        file1.writeText("content1")
        file2.writeText("content2")
        subdir1.mkdir()
        subdir2.mkdir()

        // When: List directory contents
        val result = directoryListTool.listDirectory(tempDir.toString())

        // Then: Should show both files and directories with correct types
        assertTrue(result.contains("Directory contents for"))
        assertTrue(result.contains("[FILE] file1.txt"))
        assertTrue(result.contains("[FILE] file2.kt"))
        assertTrue(result.contains("[DIR] subdir1"))
        assertTrue(result.contains("[DIR] subdir2"))
    }

    @Test
    fun `listDirectory should show file sizes`() {
        // Given: Create files with different sizes
        val smallFile = File(tempDir.toFile(), "small.txt")
        val largerFile = File(tempDir.toFile(), "larger.txt")

        smallFile.writeText("Hi") // 2 bytes
        largerFile.writeText("This is a longer content") // 24 bytes

        // When: List directory contents
        val result = directoryListTool.listDirectory(tempDir.toString())

        // Then: Should show file sizes
        assertTrue(result.contains("[FILE] small.txt (2 bytes)"))
        assertTrue(result.contains("[FILE] larger.txt (24 bytes)"))
    }

    @Test
    fun `listDirectory should sort contents alphabetically by formatted string`() {
        // Given: Create files and directories in non-alphabetical order
        val fileZ = File(tempDir.toFile(), "z-file.txt")
        val fileA = File(tempDir.toFile(), "a-file.txt")
        val dirY = File(tempDir.toFile(), "y-dir")
        val dirB = File(tempDir.toFile(), "b-dir")

        fileZ.writeText("content")
        fileA.writeText("content")
        dirY.mkdir()
        dirB.mkdir()

        // When: List directory contents
        val result = directoryListTool.listDirectory(tempDir.toString())

        // Then: Should be sorted alphabetically by the full formatted string
        // This means [DIR] entries come before [FILE] entries when sorted as strings
        val lines = result.lines().filter { it.startsWith("[") }
        assertTrue(lines.size == 4)

        // The sorting is done on "[DIR] name" vs "[FILE] name (size bytes)"
        // So directories (starting with "[DIR]") come before files (starting with "[FILE]")
        assertTrue(lines.any { it.contains("[DIR] b-dir") })
        assertTrue(lines.any { it.contains("[DIR] y-dir") })
        assertTrue(lines.any { it.contains("[FILE] a-file.txt") })
        assertTrue(lines.any { it.contains("[FILE] z-file.txt") })

        // Extract just the formatted entries and verify they are sorted
        val sortedLines = lines.sorted()
        assertEquals(sortedLines, lines) // Should already be sorted
    }

    @Test
    fun `listDirectory should handle empty directory`() {
        // Given: An empty directory
        val emptyDir = File(tempDir.toFile(), "empty")
        emptyDir.mkdir()

        // When: List empty directory
        val result = directoryListTool.listDirectory(emptyDir.absolutePath)

        // Then: Should indicate directory is empty
        assertTrue(result.startsWith("Directory is empty"))
        assertTrue(result.contains(emptyDir.absolutePath))
    }

    @Test
    fun `listDirectory should handle non-existent directory`() {
        // Given: A non-existent directory path
        val nonExistentDir = File(tempDir.toFile(), "nonexistent").absolutePath

        // When: Try to list non-existent directory
        val result = directoryListTool.listDirectory(nonExistentDir)

        // Then: Should return error message
        assertTrue(result.startsWith("Error: Directory does not exist"))
        assertTrue(result.contains(nonExistentDir))
    }

    @Test
    fun `listDirectory should handle file path instead of directory`() {
        // Given: A file path instead of directory
        val testFile = File(tempDir.toFile(), "file.txt")
        testFile.writeText("content")

        // When: Try to list file as directory
        val result = directoryListTool.listDirectory(testFile.absolutePath)

        // Then: Should return error message
        assertTrue(result.startsWith("Error: Path is not a directory"))
        assertTrue(result.contains(testFile.absolutePath))
    }

    @Test
    fun `listFiles should show only files`() {
        // Given: Create mixed content in directory
        val file1 = File(tempDir.toFile(), "file1.txt")
        val file2 = File(tempDir.toFile(), "file2.kt")
        val subdir = File(tempDir.toFile(), "subdir")

        file1.writeText("content1")
        file2.writeText("content2")
        subdir.mkdir()

        // When: List only files
        val result = directoryListTool.listFiles(tempDir.toString())

        // Then: Should show only files, not directories
        assertTrue(result.contains("Files in"))
        assertTrue(result.contains("file1.txt"))
        assertTrue(result.contains("file2.kt"))
        assertFalse(result.contains("subdir"))
    }

    @Test
    fun `listFiles should show file sizes`() {
        // Given: Create files with different sizes
        val file1 = File(tempDir.toFile(), "file1.txt")
        val file2 = File(tempDir.toFile(), "file2.txt")

        file1.writeText("A") // 1 byte
        file2.writeText("Hello World") // 11 bytes

        // When: List files
        val result = directoryListTool.listFiles(tempDir.toString())

        // Then: Should show file sizes
        assertTrue(result.contains("file1.txt (1 bytes)"))
        assertTrue(result.contains("file2.txt (11 bytes)"))
    }

    @Test
    fun `listFiles should sort files alphabetically`() {
        // Given: Create files in non-alphabetical order
        val fileZ = File(tempDir.toFile(), "z-file.txt")
        val fileA = File(tempDir.toFile(), "a-file.txt")
        val fileM = File(tempDir.toFile(), "m-file.txt")

        fileZ.writeText("content")
        fileA.writeText("content")
        fileM.writeText("content")

        // When: List files
        val result = directoryListTool.listFiles(tempDir.toString())

        // Then: Should be sorted alphabetically
        val lines = result.lines().filter { it.contains("(") && it.contains("bytes)") }
        val fileNames = lines.map { it.substringBefore(" (") }
        assertEquals(listOf("a-file.txt", "m-file.txt", "z-file.txt"), fileNames)
    }

    @Test
    fun `listFiles should handle directory with no files`() {
        // Given: A directory with only subdirectories
        val subdir1 = File(tempDir.toFile(), "subdir1")
        val subdir2 = File(tempDir.toFile(), "subdir2")
        subdir1.mkdir()
        subdir2.mkdir()

        // When: List files
        val result = directoryListTool.listFiles(tempDir.toString())

        // Then: Should indicate no files found
        assertTrue(result.startsWith("No files found in directory"))
        assertTrue(result.contains(tempDir.toString()))
    }

    @Test
    fun `listFiles should handle non-existent directory`() {
        // Given: A non-existent directory path
        val nonExistentDir = File(tempDir.toFile(), "nonexistent").absolutePath

        // When: Try to list files in non-existent directory
        val result = directoryListTool.listFiles(nonExistentDir)

        // Then: Should return error message
        assertTrue(result.startsWith("Error: Directory does not exist"))
        assertTrue(result.contains(nonExistentDir))
    }

    @Test
    fun `listFiles should handle file path instead of directory`() {
        // Given: A file path instead of directory
        val testFile = File(tempDir.toFile(), "file.txt")
        testFile.writeText("content")

        // When: Try to list files in file path
        val result = directoryListTool.listFiles(testFile.absolutePath)

        // Then: Should return error message
        assertTrue(result.startsWith("Error: Path is not a directory"))
        assertTrue(result.contains(testFile.absolutePath))
    }

    @Test
    fun `listDirectories should show only directories`() {
        // Given: Create mixed content in directory
        val file1 = File(tempDir.toFile(), "file1.txt")
        val file2 = File(tempDir.toFile(), "file2.kt")
        val subdir1 = File(tempDir.toFile(), "subdir1")
        val subdir2 = File(tempDir.toFile(), "subdir2")

        file1.writeText("content1")
        file2.writeText("content2")
        subdir1.mkdir()
        subdir2.mkdir()

        // When: List only directories
        val result = directoryListTool.listDirectories(tempDir.toString())

        // Then: Should show only directories, not files
        assertTrue(result.contains("Subdirectories in"))
        assertTrue(result.contains("subdir1"))
        assertTrue(result.contains("subdir2"))
        assertFalse(result.contains("file1.txt"))
        assertFalse(result.contains("file2.kt"))
    }

    @Test
    fun `listDirectories should sort directories alphabetically`() {
        // Given: Create directories in non-alphabetical order
        val dirZ = File(tempDir.toFile(), "z-dir")
        val dirA = File(tempDir.toFile(), "a-dir")
        val dirM = File(tempDir.toFile(), "m-dir")

        dirZ.mkdir()
        dirA.mkdir()
        dirM.mkdir()

        // When: List directories
        val result = directoryListTool.listDirectories(tempDir.toString())

        // Then: Should be sorted alphabetically
        val lines = result.lines().filter { it.isNotEmpty() && !it.contains("Subdirectories in") }
        assertEquals(listOf("a-dir", "m-dir", "z-dir"), lines)
    }

    @Test
    fun `listDirectories should handle directory with no subdirectories`() {
        // Given: A directory with only files
        val file1 = File(tempDir.toFile(), "file1.txt")
        val file2 = File(tempDir.toFile(), "file2.txt")
        file1.writeText("content")
        file2.writeText("content")

        // When: List directories
        val result = directoryListTool.listDirectories(tempDir.toString())

        // Then: Should indicate no subdirectories found
        assertTrue(result.startsWith("No subdirectories found in"))
        assertTrue(result.contains(tempDir.toString()))
    }

    @Test
    fun `listDirectories should handle non-existent directory`() {
        // Given: A non-existent directory path
        val nonExistentDir = File(tempDir.toFile(), "nonexistent").absolutePath

        // When: Try to list directories in non-existent path
        val result = directoryListTool.listDirectories(nonExistentDir)

        // Then: Should return error message
        assertTrue(result.startsWith("Error: Directory does not exist"))
        assertTrue(result.contains(nonExistentDir))
    }

    @Test
    fun `listDirectories should handle file path instead of directory`() {
        // Given: A file path instead of directory
        val testFile = File(tempDir.toFile(), "file.txt")
        testFile.writeText("content")

        // When: Try to list directories in file path
        val result = directoryListTool.listDirectories(testFile.absolutePath)

        // Then: Should return error message
        assertTrue(result.startsWith("Error: Path is not a directory"))
        assertTrue(result.contains(testFile.absolutePath))
    }

    @Test
    fun `listDirectory should handle files with special characters in names`() {
        // Given: Create files with special characters
        val specialFile1 = File(tempDir.toFile(), "file with spaces.txt")
        val specialFile2 = File(tempDir.toFile(), "file-with-dashes.txt")
        val specialFile3 = File(tempDir.toFile(), "file_with_underscores.txt")

        specialFile1.writeText("content")
        specialFile2.writeText("content")
        specialFile3.writeText("content")

        // When: List directory
        val result = directoryListTool.listDirectory(tempDir.toString())

        // Then: Should handle special characters in names
        assertTrue(result.contains("[FILE] file with spaces.txt"))
        assertTrue(result.contains("[FILE] file-with-dashes.txt"))
        assertTrue(result.contains("[FILE] file_with_underscores.txt"))
    }

    @Test
    fun `listDirectory should handle mixed case file names`() {
        // Given: Create files with mixed case names
        val fileA = File(tempDir.toFile(), "FileA.txt")
        val fileB = File(tempDir.toFile(), "fileb.txt")
        val fileC = File(tempDir.toFile(), "FILEC.txt")

        fileA.writeText("content")
        fileB.writeText("content")
        fileC.writeText("content")

        // When: List directory
        val result = directoryListTool.listDirectory(tempDir.toString())

        // Then: Should preserve case in names and sort correctly
        assertTrue(result.contains("[FILE] FileA.txt"))
        assertTrue(result.contains("[FILE] fileb.txt"))
        assertTrue(result.contains("[FILE] FILEC.txt"))
    }

    @Test
    fun `listDirectory should handle empty files`() {
        // Given: Create empty files
        val emptyFile1 = File(tempDir.toFile(), "empty1.txt")
        val emptyFile2 = File(tempDir.toFile(), "empty2.txt")

        emptyFile1.createNewFile()
        emptyFile2.createNewFile()

        // When: List directory
        val result = directoryListTool.listDirectory(tempDir.toString())

        // Then: Should show 0 bytes for empty files
        assertTrue(result.contains("[FILE] empty1.txt (0 bytes)"))
        assertTrue(result.contains("[FILE] empty2.txt (0 bytes)"))
    }

    @Test
    fun `listFiles should handle different file extensions`() {
        // Given: Create files with various extensions
        val txtFile = File(tempDir.toFile(), "document.txt")
        val ktFile = File(tempDir.toFile(), "code.kt")
        val jsonFile = File(tempDir.toFile(), "data.json")
        val noExtFile = File(tempDir.toFile(), "README")

        txtFile.writeText("text content")
        ktFile.writeText("kotlin code")
        jsonFile.writeText("{\"key\": \"value\"}")
        noExtFile.writeText("readme content")

        // When: List files
        val result = directoryListTool.listFiles(tempDir.toString())

        // Then: Should show all file types
        assertTrue(result.contains("document.txt"))
        assertTrue(result.contains("code.kt"))
        assertTrue(result.contains("data.json"))
        assertTrue(result.contains("README"))
    }

    @Test
    fun `all methods should handle nested directory structure`() {
        // Given: Create nested directory structure
        val nestedDir = File(tempDir.toFile(), "level1")
        nestedDir.mkdir()

        val file1 = File(tempDir.toFile(), "root-file.txt")
        val file2 = File(nestedDir, "nested-file.txt")
        val subdir = File(tempDir.toFile(), "subdir")

        file1.writeText("root content")
        file2.writeText("nested content")
        subdir.mkdir()

        // When: List root directory contents
        val dirResult = directoryListTool.listDirectory(tempDir.toString())
        val filesResult = directoryListTool.listFiles(tempDir.toString())
        val dirsResult = directoryListTool.listDirectories(tempDir.toString())

        // Then: Should show root level content only (not recursive)
        // Directory listing should show root file and both directories
        assertTrue(dirResult.contains("[FILE] root-file.txt"))
        assertTrue(dirResult.contains("[DIR] level1"))
        assertTrue(dirResult.contains("[DIR] subdir"))
        assertFalse(dirResult.contains("nested-file.txt"))

        // Files listing should show only root file
        assertTrue(filesResult.contains("root-file.txt"))
        assertFalse(filesResult.contains("nested-file.txt"))

        // Directories listing should show both subdirectories
        assertTrue(dirsResult.contains("level1"))
        assertTrue(dirsResult.contains("subdir"))
    }
}
