package me.konoplev.autocover.tools.directory

import me.konoplev.autocover.services.FileSystemTransactionManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectoryCreateToolTest {

    private lateinit var directoryCreateTool: DirectoryCreateTool

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        directoryCreateTool = DirectoryCreateTool(FileSystemTransactionManager())
    }

    @Test
    fun `createDirectory should create new directory`() {
        // Given: A path for new directory
        val newDir = File(tempDir.toFile(), "new-directory")

        // When: Create the directory
        val result = directoryCreateTool.createDirectory(newDir.absolutePath)

        // Then: Should create directory successfully
        assertTrue(result.startsWith("Successfully created directory"))
        assertTrue(newDir.exists())
        assertTrue(newDir.isDirectory())
    }

    @Test
    fun `createDirectory should handle existing directory`() {
        // Given: An existing directory
        val existingDir = File(tempDir.toFile(), "existing-directory")
        existingDir.mkdir()

        // When: Try to create the same directory
        val result = directoryCreateTool.createDirectory(existingDir.absolutePath)

        // Then: Should indicate directory already exists
        assertTrue(result.startsWith("Directory already exists"))
        assertTrue(existingDir.exists())
        assertTrue(existingDir.isDirectory())
    }

    @Test
    fun `createDirectory should handle file existing at path`() {
        // Given: A file at the target path
        val testFile = File(tempDir.toFile(), "existing-file.txt")
        testFile.writeText("content")

        // When: Try to create directory at file path
        val result = directoryCreateTool.createDirectory(testFile.absolutePath)

        // Then: Should return error about existing file
        assertTrue(result.startsWith("Error: A file already exists at path"))
        assertTrue(testFile.exists())
        assertTrue(testFile.isFile())
    }

    @Test
    fun `createDirectory should fail when parent doesn't exist`() {
        // Given: A nested path where parent doesn't exist
        val nestedDir = File(tempDir.toFile(), "nonexistent/nested/directory")

        // When: Try to create nested directory without creating parents
        val result = directoryCreateTool.createDirectory(nestedDir.absolutePath)

        // Then: Should return error (Files.createDirectory requires parent to exist)
        assertTrue(result.startsWith("Error creating directory"))
        assertFalse(nestedDir.exists())
    }

    @Test
    fun `createDirectory should handle special characters in directory name`() {
        // Given: Directory name with special characters (avoiding illegal ones)
        val expectedDirName = "directory with spaces and-dashes_underscores"
        val specialDir = File(tempDir.toFile(), expectedDirName)

        // When: Create directory with special characters
        val result = directoryCreateTool.createDirectory(specialDir.absolutePath)

        // Then: Should create directory successfully
        assertTrue(result.startsWith("Successfully created directory"))
        assertTrue(result.contains(specialDir.absolutePath))
        assertTrue(specialDir.exists())
        assertTrue(specialDir.isDirectory())

        // Verify the directory name is preserved correctly
        assertEquals(expectedDirName, specialDir.name)

        // Verify we can list the directory and see it with correct name
        val parentListing = specialDir.parentFile.listFiles()
        assertEquals(parentListing?.any { it.name == expectedDirName && it.isDirectory }, true)
    }

    @Test
    fun `createDirectories should create nested directories`() {
        // Given: A deeply nested path
        val nestedDir = File(tempDir.toFile(), "level1/level2/level3/level4")

        // When: Create nested directories
        val result = directoryCreateTool.createDirectories(nestedDir.absolutePath)

        // Then: Should create all directories in the path
        assertTrue(result.startsWith("Successfully created directories"))
        assertTrue(nestedDir.exists())
        assertTrue(nestedDir.isDirectory())

        // Verify all parent directories exist
        assertTrue(File(tempDir.toFile(), "level1").exists())
        assertTrue(File(tempDir.toFile(), "level1/level2").exists())
        assertTrue(File(tempDir.toFile(), "level1/level2/level3").exists())
    }

    @Test
    fun `createDirectories should handle existing directory`() {
        // Given: An existing directory structure
        val existingDir = File(tempDir.toFile(), "existing/nested/directory")
        existingDir.mkdirs()

        // When: Try to create the same nested directories
        val result = directoryCreateTool.createDirectories(existingDir.absolutePath)

        // Then: Should indicate directory already exists
        assertTrue(result.startsWith("Directory already exists"))
        assertTrue(existingDir.exists())
        assertTrue(existingDir.isDirectory())
    }

    @Test
    fun `createDirectories should handle file existing at path`() {
        // Given: A file at the target path
        val testFile = File(tempDir.toFile(), "existing-file.txt")
        testFile.writeText("content")

        // When: Try to create directories at file path
        val result = directoryCreateTool.createDirectories(testFile.absolutePath)

        // Then: Should return error about existing file
        assertTrue(result.startsWith("Error: A file already exists at path"))
        assertTrue(testFile.exists())
        assertTrue(testFile.isFile())
    }

    @Test
    fun `createDirectories should handle partially existing path`() {
        // Given: Partially existing directory structure
        val partialDir = File(tempDir.toFile(), "existing")
        partialDir.mkdir()
        val targetDir = File(partialDir, "new/nested/directories")

        // When: Create remaining directories
        val result = directoryCreateTool.createDirectories(targetDir.absolutePath)

        // Then: Should create missing directories
        assertTrue(result.startsWith("Successfully created directories"))
        assertTrue(targetDir.exists())
        assertTrue(targetDir.isDirectory())
    }

    @Test
    fun `createDirectories should create single directory when no nesting needed`() {
        // Given: A simple directory path (no nesting)
        val simpleDir = File(tempDir.toFile(), "simple-directory")

        // When: Create single directory using createDirectories
        val result = directoryCreateTool.createDirectories(simpleDir.absolutePath)

        // Then: Should create directory successfully
        assertTrue(result.startsWith("Successfully created directories"))
        assertTrue(simpleDir.exists())
        assertTrue(simpleDir.isDirectory())
    }

    @Test
    fun `deleteDirectory should remove empty directory`() {
        // Given: An empty directory
        val emptyDir = File(tempDir.toFile(), "empty-directory")
        emptyDir.mkdir()

        // When: Delete the empty directory
        val result = directoryCreateTool.deleteDirectory(emptyDir.absolutePath)

        // Then: Should delete directory successfully
        assertTrue(result.startsWith("Successfully deleted directory"))
        assertFalse(emptyDir.exists())
    }

    @Test
    fun `deleteDirectory should handle non-existent directory`() {
        // Given: A non-existent directory path
        val nonExistentDir = File(tempDir.toFile(), "nonexistent-directory")

        // When: Try to delete non-existent directory
        val result = directoryCreateTool.deleteDirectory(nonExistentDir.absolutePath)

        // Then: Should return error about non-existent directory
        assertTrue(result.startsWith("Error: Directory does not exist"))
    }

    @Test
    fun `deleteDirectory should handle file path instead of directory`() {
        // Given: A file instead of directory
        val testFile = File(tempDir.toFile(), "test-file.txt")
        testFile.writeText("content")

        // When: Try to delete file as directory
        val result = directoryCreateTool.deleteDirectory(testFile.absolutePath)

        // Then: Should return error about path not being directory
        assertTrue(result.startsWith("Error: Path is not a directory"))
        assertTrue(testFile.exists()) // File should remain
    }

    @Test
    fun `deleteDirectory should refuse to delete non-empty directory`() {
        // Given: A directory with content
        val nonEmptyDir = File(tempDir.toFile(), "non-empty-directory")
        nonEmptyDir.mkdir()
        val childFile = File(nonEmptyDir, "child-file.txt")
        childFile.writeText("content")

        // When: Try to delete non-empty directory
        val result = directoryCreateTool.deleteDirectory(nonEmptyDir.absolutePath)

        // Then: Should return error about directory not being empty
        assertTrue(result.startsWith("Error: Directory is not empty"))
        assertTrue(nonEmptyDir.exists()) // Directory should remain
        assertTrue(childFile.exists()) // Child file should remain
    }

    @Test
    fun `deleteDirectory should refuse to delete directory with subdirectories`() {
        // Given: A directory with subdirectories
        val parentDir = File(tempDir.toFile(), "parent-directory")
        val childDir = File(parentDir, "child-directory")
        childDir.mkdirs()

        // When: Try to delete directory with subdirectories
        val result = directoryCreateTool.deleteDirectory(parentDir.absolutePath)

        // Then: Should return error about directory not being empty
        assertTrue(result.startsWith("Error: Directory is not empty"))
        assertTrue(parentDir.exists()) // Parent directory should remain
        assertTrue(childDir.exists()) // Child directory should remain
    }

    @Test
    fun `directoryExists should return true for existing directory`() {
        // Given: An existing directory
        val existingDir = File(tempDir.toFile(), "existing-directory")
        existingDir.mkdir()

        // When: Check if directory exists
        val result = directoryCreateTool.directoryExists(existingDir.absolutePath)

        // Then: Should confirm directory exists
        assertTrue(result.startsWith("Directory exists"))
    }

    @Test
    fun `directoryExists should return false for non-existent directory`() {
        // Given: A non-existent directory path
        val nonExistentDir = File(tempDir.toFile(), "nonexistent-directory")

        // When: Check if directory exists
        val result = directoryCreateTool.directoryExists(nonExistentDir.absolutePath)

        // Then: Should indicate directory does not exist
        assertTrue(result.startsWith("Directory does not exist"))
    }

    @Test
    fun `directoryExists should return false for file path`() {
        // Given: A file instead of directory
        val testFile = File(tempDir.toFile(), "test-file.txt")
        testFile.writeText("content")

        // When: Check if file path is directory
        val result = directoryCreateTool.directoryExists(testFile.absolutePath)

        // Then: Should indicate directory does not exist (since it's a file)
        assertTrue(result.startsWith("Directory does not exist"))
    }

    @Test
    fun `directoryExists should handle nested directory paths`() {
        // Given: A nested directory structure
        val nestedDir = File(tempDir.toFile(), "level1/level2/level3")
        nestedDir.mkdirs()

        // When: Check existence of nested directory
        val result = directoryCreateTool.directoryExists(nestedDir.absolutePath)

        // Then: Should confirm nested directory exists
        assertTrue(result.startsWith("Directory exists"))
    }

    @Test
    fun `integration test - create, check, and delete directory lifecycle`() {
        // Given: A directory path
        val testDir = File(tempDir.toFile(), "lifecycle-test-directory")

        // When & Then: Complete lifecycle
        // 1. Verify directory doesn't exist initially
        var result = directoryCreateTool.directoryExists(testDir.absolutePath)
        assertTrue(result.startsWith("Directory does not exist"))

        // 2. Create directory
        result = directoryCreateTool.createDirectory(testDir.absolutePath)
        assertTrue(result.startsWith("Successfully created directory"))
        assertTrue(testDir.exists())

        // 3. Verify directory now exists
        result = directoryCreateTool.directoryExists(testDir.absolutePath)
        assertTrue(result.startsWith("Directory exists"))

        // 4. Try to create again (should indicate already exists)
        result = directoryCreateTool.createDirectory(testDir.absolutePath)
        assertTrue(result.startsWith("Directory already exists"))

        // 5. Delete directory
        result = directoryCreateTool.deleteDirectory(testDir.absolutePath)
        assertTrue(result.startsWith("Successfully deleted directory"))
        assertFalse(testDir.exists())

        // 6. Verify directory no longer exists
        result = directoryCreateTool.directoryExists(testDir.absolutePath)
        assertTrue(result.startsWith("Directory does not exist"))
    }

    @Test
    fun `integration test - create nested directories and clean up`() {
        // Given: A nested directory path
        val nestedDir = File(tempDir.toFile(), "integration/test/nested/directories")

        // When & Then: Nested directories lifecycle
        // 1. Create nested directories
        var result = directoryCreateTool.createDirectories(nestedDir.absolutePath)
        assertTrue(result.startsWith("Successfully created directories"))
        assertTrue(nestedDir.exists())

        // 2. Verify all levels exist
        val level1 = File(tempDir.toFile(), "integration")
        val level2 = File(level1, "test")
        val level3 = File(level2, "nested")

        assertTrue(directoryCreateTool.directoryExists(level1.absolutePath).startsWith("Directory exists"))
        assertTrue(directoryCreateTool.directoryExists(level2.absolutePath).startsWith("Directory exists"))
        assertTrue(directoryCreateTool.directoryExists(level3.absolutePath).startsWith("Directory exists"))
        assertTrue(directoryCreateTool.directoryExists(nestedDir.absolutePath).startsWith("Directory exists"))

        // 3. Clean up from deepest to shallowest (since deleteDirectory only removes empty directories)
        result = directoryCreateTool.deleteDirectory(nestedDir.absolutePath)
        assertTrue(result.startsWith("Successfully deleted directory"))

        result = directoryCreateTool.deleteDirectory(level3.absolutePath)
        assertTrue(result.startsWith("Successfully deleted directory"))

        result = directoryCreateTool.deleteDirectory(level2.absolutePath)
        assertTrue(result.startsWith("Successfully deleted directory"))

        result = directoryCreateTool.deleteDirectory(level1.absolutePath)
        assertTrue(result.startsWith("Successfully deleted directory"))

        // 4. Verify all directories are gone
        assertFalse(nestedDir.exists())
        assertFalse(level3.exists())
        assertFalse(level2.exists())
        assertFalse(level1.exists())
    }

    @Test
    fun `createDirectory should handle invalid characters gracefully`() {
        // Given: Directory name with potentially problematic characters
        val invalidPath = if (System.getProperty("os.name").lowercase().contains("windows")) {
            tempDir.toString() + "\\invalid|directory<name>"
        } else {
            tempDir.toString() + "/\u0000invalid"
        }

        // When: Try to create directory with invalid characters
        val result = directoryCreateTool.createDirectory(invalidPath)

        // Then: Should return error
        assertTrue(result.startsWith("Error creating directory"))
    }

    @Test
    fun `createDirectories should handle invalid characters gracefully`() {
        // Given: Nested path with potentially problematic characters
        val invalidPath = if (System.getProperty("os.name").lowercase().contains("windows")) {
            tempDir.toString() + "\\invalid|nested<directory>\\name"
        } else {
            tempDir.toString() + "/\u0000invalid/nested"
        }

        // When: Try to create directories with invalid characters
        val result = directoryCreateTool.createDirectories(invalidPath)

        // Then: Should return error
        assertTrue(result.startsWith("Error creating directories"))
    }
}
