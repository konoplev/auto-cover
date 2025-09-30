package me.konoplev.autocover.services

import me.konoplev.autocover.tools.directory.DirectoryCreateTool
import me.konoplev.autocover.tools.directory.DirectoryListTool
import me.konoplev.autocover.tools.file.FileReadTool
import me.konoplev.autocover.tools.file.FileWriteTool
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileSystemTransactionManagerTest {

    private lateinit var transactionManager: FileSystemTransactionManager
    private lateinit var fileWriteTool: FileWriteTool
    private lateinit var fileReadTool: FileReadTool
    private lateinit var directoryCreateTool: DirectoryCreateTool
    private lateinit var directoryListTool: DirectoryListTool

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        transactionManager = FileSystemTransactionManager()

        fileWriteTool = FileWriteTool(transactionManager)
        fileReadTool = FileReadTool()
        directoryCreateTool = DirectoryCreateTool(transactionManager)
        directoryListTool = DirectoryListTool()
    }

    @Test
    fun `should track and commit file creation`() {
        // Given: Active transaction
        transactionManager.startTransaction()
        val testFile = File(tempDir.toFile(), "test-file.txt")

        // When: Create file
        val writeResult = fileWriteTool.writeFile(testFile.absolutePath, "test content")

        // Then: File should be created
        assertTrue(writeResult.startsWith("Successfully wrote content"))
        assertTrue(testFile.exists())
        assertEquals("test content", testFile.readText())

        // When: Commit transaction
        transactionManager.commitTransaction()

        // Then: Changes should be permanent
        // Committed successfully
        assertTrue(testFile.exists())
        assertEquals("test content", testFile.readText())
    }

    @Test
    fun `should track and rollback file creation`() {
        // Given: Active transaction
        transactionManager.startTransaction()
        val testFile = File(tempDir.toFile(), "test-file.txt")

        // When: Create file
        fileWriteTool.writeFile(testFile.absolutePath, "test content")
        assertTrue(testFile.exists())

        // When: Rollback transaction
        transactionManager.rollbackTransaction()

        // Then: File should be removed
        // Rolled back successfully
        assertFalse(testFile.exists())
    }

    @Test
    fun `should track and commit file modification`() {
        // Given: Existing file
        val testFile = File(tempDir.toFile(), "existing-file.txt")
        testFile.writeText("original content")

        // Given: Active transaction
        transactionManager.startTransaction()

        // When: Modify file
        val writeResult = fileWriteTool.writeFile(testFile.absolutePath, "modified content")

        // Then: File should be modified
        assertTrue(writeResult.startsWith("Successfully wrote content"))
        assertEquals("modified content", testFile.readText())

        // When: Commit transaction
        transactionManager.commitTransaction()

        // Then: Changes should be permanent and backup removed
        // Committed successfully
        assertEquals("modified content", testFile.readText())
        assertFalse(File(testFile.absolutePath + ".backup").exists())
    }

    @Test
    fun `should track and rollback file modification`() {
        // Given: Existing file
        val testFile = File(tempDir.toFile(), "existing-file.txt")
        testFile.writeText("original content")

        // Given: Active transaction
        transactionManager.startTransaction()

        // When: Modify file
        fileWriteTool.writeFile(testFile.absolutePath, "modified content")
        assertEquals("modified content", testFile.readText())

        // When: Rollback transaction
        transactionManager.rollbackTransaction()

        // Then: Original content should be restored
        assertEquals("original content", testFile.readText())
        assertFalse(File(testFile.absolutePath + ".backup").exists())
    }

    @Test
    fun `should track and commit file deletion`() {
        // Given: Existing file
        val testFile = File(tempDir.toFile(), "file-to-delete.txt")
        testFile.writeText("content to delete")

        // Given: Active transaction
        transactionManager.startTransaction()

        // When: Delete file
        val deleteResult = fileWriteTool.deleteFile(testFile.absolutePath)

        // Then: File should be deleted
        assertTrue(deleteResult.startsWith("Successfully deleted file"))
        assertFalse(testFile.exists())

        // When: Commit transaction
        transactionManager.commitTransaction()

        // Then: File should remain deleted and .removed backup cleaned up
        // Committed successfully
        assertFalse(testFile.exists())
        assertFalse(File(testFile.absolutePath + ".removed").exists())
    }

    @Test
    fun `should track and rollback file deletion`() {
        // Given: Existing file
        val testFile = File(tempDir.toFile(), "file-to-delete.txt")
        val originalContent = "content to delete"
        testFile.writeText(originalContent)

        // Given: Active transaction
        transactionManager.startTransaction()

        // When: Delete file
        fileWriteTool.deleteFile(testFile.absolutePath)
        assertFalse(testFile.exists())

        // When: Rollback transaction
        transactionManager.rollbackTransaction()

        // Then: File should be restored
        // Rolled back successfully
        assertTrue(testFile.exists())
        assertEquals(originalContent, testFile.readText())
        assertFalse(File(testFile.absolutePath + ".removed").exists())
    }

    @Test
    fun `should track and commit directory creation`() {
        // Given: Active transaction
        transactionManager.startTransaction()
        val testDir = File(tempDir.toFile(), "test-directory")

        // When: Create directory
        val createResult = directoryCreateTool.createDirectory(testDir.absolutePath)

        // Then: Directory should be created
        assertTrue(createResult.startsWith("Successfully created directory"))
        assertTrue(testDir.exists())
        assertTrue(testDir.isDirectory())

        // When: Commit transaction
        transactionManager.commitTransaction()

        // Then: Directory should remain
        assertTrue(testDir.exists())
        assertTrue(testDir.isDirectory())
    }

    @Test
    fun `should track and rollback directory creation`() {
        // Given: Active transaction
        transactionManager.startTransaction()
        val testDir = File(tempDir.toFile(), "test-directory")

        // When: Create directory
        directoryCreateTool.createDirectory(testDir.absolutePath)
        assertTrue(testDir.exists())

        // When: Rollback transaction
        transactionManager.rollbackTransaction()

        // Then: Directory should be removed
        assertFalse(testDir.exists())
    }

    @Test
    fun `should track and commit nested directory creation`() {
        // Given: Active transaction
        transactionManager.startTransaction()
        val nestedDir = File(tempDir.toFile(), "level1/level2/level3")

        // When: Create nested directories
        val createResult = directoryCreateTool.createDirectories(nestedDir.absolutePath)

        // Then: All directories should be created
        assertTrue(createResult.startsWith("Successfully created directories"))
        assertTrue(nestedDir.exists())
        assertTrue(nestedDir.isDirectory())

        // When: Commit transaction
        transactionManager.commitTransaction()

        // Then: All directories should remain
        assertTrue(nestedDir.exists())
        assertTrue(File(tempDir.toFile(), "level1").exists())
        assertTrue(File(tempDir.toFile(), "level1/level2").exists())
    }

    @Test
    fun `should track and rollback nested directory creation`() {
        // Given: Active transaction
        transactionManager.startTransaction()
        val nestedDir = File(tempDir.toFile(), "level1/level2/level3")

        // When: Create nested directories
        directoryCreateTool.createDirectories(nestedDir.absolutePath)
        assertTrue(nestedDir.exists())

        // When: Rollback transaction
        transactionManager.rollbackTransaction()

        // Then: All created directories should be removed
        assertFalse(nestedDir.exists())
        assertFalse(File(tempDir.toFile(), "level1/level2").exists())
        assertFalse(File(tempDir.toFile(), "level1").exists())
    }

    @Test
    fun `should track and commit directory deletion`() {
        // Given: Existing empty directory
        val testDir = File(tempDir.toFile(), "directory-to-delete")
        testDir.mkdir()

        // Given: Active transaction
        transactionManager.startTransaction()

        // When: Delete directory
        val deleteResult = directoryCreateTool.deleteDirectory(testDir.absolutePath)

        // Then: Directory should be deleted
        assertTrue(deleteResult.startsWith("Successfully deleted directory"))
        assertFalse(testDir.exists())

        // When: Commit transaction
        transactionManager.commitTransaction()

        // Then: Directory should remain deleted
        assertFalse(testDir.exists())
        assertFalse(File(testDir.absolutePath + ".removed").exists())
    }

    @Test
    fun `should track and rollback directory deletion`() {
        // Given: Existing empty directory
        val testDir = File(tempDir.toFile(), "directory-to-delete")
        testDir.mkdir()

        // Given: Active transaction
        transactionManager.startTransaction()

        // When: Delete directory
        directoryCreateTool.deleteDirectory(testDir.absolutePath)
        assertFalse(testDir.exists())

        // When: Rollback transaction
        transactionManager.rollbackTransaction()

        // Then: Directory should be restored
        assertTrue(testDir.exists())
        assertTrue(testDir.isDirectory())
        assertFalse(File(testDir.absolutePath + ".removed").exists())
    }

    @Test
    fun `should handle complex transaction with multiple operations`() {
        // Given: Existing files and directories
        val existingFile = File(tempDir.toFile(), "existing.txt")
        existingFile.writeText("original content")
        val existingDir = File(tempDir.toFile(), "existing-dir")
        existingDir.mkdir()

        // Given: Active transaction
        transactionManager.startTransaction()

        // When: Perform multiple operations
        val newFile = File(tempDir.toFile(), "new-file.txt")
        val newDir = File(tempDir.toFile(), "new-directory")

        fileWriteTool.writeFile(newFile.absolutePath, "new content")
        fileWriteTool.writeFile(existingFile.absolutePath, "modified content")
        directoryCreateTool.createDirectory(newDir.absolutePath)
        directoryCreateTool.deleteDirectory(existingDir.absolutePath)

        // Then: All operations should be applied
        assertTrue(newFile.exists())
        assertEquals("modified content", existingFile.readText())
        assertTrue(newDir.exists())
        assertFalse(existingDir.exists())

        // When: Rollback transaction
        transactionManager.rollbackTransaction()

        // Then: All changes should be reverted
        assertFalse(newFile.exists()) // Created file removed
        assertEquals("original content", existingFile.readText()) // Modified file restored
        assertFalse(newDir.exists()) // Created directory removed
        assertTrue(existingDir.exists()) // Deleted directory restored
    }

    @Test
    fun `should allow operations without active transaction`() {
        // Given: A test file path
        val testFile = File(tempDir.toFile(), "no-transaction.txt")
        val testDir = File(tempDir.toFile(), "no-transaction-dir")

        // When: Try to perform operations without transaction
        val writeResult = fileWriteTool.writeFile(testFile.absolutePath, "content")
        val createDirResult = directoryCreateTool.createDirectory(testDir.absolutePath)

        // Then: Should work normally (no transaction tracking)
        assertTrue(writeResult.startsWith("Successfully wrote content"))
        assertTrue(createDirResult.startsWith("Successfully created directory"))
        assertTrue(testFile.exists())
        assertTrue(testDir.exists())
        assertEquals("content", testFile.readText())
    }

    @Test
    fun `should allow read operations without transaction`() {
        // Given: Existing file
        val testFile = File(tempDir.toFile(), "readable-file.txt")
        testFile.writeText("readable content")

        // When: Try to read without transaction
        val readResult = fileReadTool.readFile(testFile.absolutePath)
        val listResult = directoryListTool.listDirectory(tempDir.toString())

        // Then: Read operations should work
        assertEquals("readable content", readResult)
        assertTrue(listResult.contains("readable-file.txt"))
    }

    @Test
    fun `should rollback mixed file operations - existing file modified and new file created`() {
        // Given: A file that exists before the transaction
        val existingFile = File(tempDir.toFile(), "existing-file.txt")
        val originalContent = "original content before transaction"
        existingFile.writeText(originalContent)

        // Given: Active transaction
        transactionManager.startTransaction()

        // When: Create a new file during transaction
        val newFile = File(tempDir.toFile(), "new-file.txt")
        val newFileContent = "content of new file"
        fileWriteTool.writeFile(newFile.absolutePath, newFileContent)

        // When: Edit the file that existed before the transaction
        val modifiedContent = "modified content during transaction"
        fileWriteTool.writeFile(existingFile.absolutePath, modifiedContent)

        // When: Edit the file created during the transaction
        val updatedNewFileContent = "updated content of new file"
        fileWriteTool.writeFile(newFile.absolutePath, updatedNewFileContent)

        // Then: Both files should have the modified content
        assertEquals(modifiedContent, existingFile.readText())
        assertEquals(updatedNewFileContent, newFile.readText())

        // When: Rollback transaction
        transactionManager.rollbackTransaction()

        // Then: The file that existed before should be restored to original content
        assertEquals(originalContent, existingFile.readText())
        assertFalse(File(existingFile.absolutePath + ".backup").exists())

        // Then: The file created during the transaction should be removed
        assertFalse(newFile.exists())
    }

    @Test
    fun `should rollback mixed file operations - existing file modified and new file created with a new directory`() {
        // Given: A file that exists before the transaction
        val existingFile = File(tempDir.toFile(), "existing-file.txt")
        val originalContent = "original content before transaction"
        existingFile.writeText(originalContent)

        // Given: Active transaction
        transactionManager.startTransaction()

        // When: Create a new file during transaction
        val newFile = File(tempDir.toFile(), "/new-directory/new-file.txt")
        val newFileContent = "content of new file"
        fileWriteTool.writeFile(newFile.absolutePath, newFileContent)

        // When: Edit the file that existed before the transaction
        val modifiedContent = "modified content during transaction"
        fileWriteTool.writeFile(existingFile.absolutePath, modifiedContent)

        // When: Edit the file created during the transaction
        val updatedNewFileContent = "updated content of new file"
        fileWriteTool.writeFile(newFile.absolutePath, updatedNewFileContent)

        // Then: Both files should have the modified content
        assertEquals(modifiedContent, existingFile.readText())
        assertEquals(updatedNewFileContent, newFile.readText())

        // When: Rollback transaction
        transactionManager.rollbackTransaction()

        // Then: The file that existed before should be restored to original content
        assertEquals(originalContent, existingFile.readText())
        assertFalse(File(existingFile.absolutePath + ".backup").exists())

        // Then: The file created during the transaction should be removed
        assertFalse(newFile.exists())
    }

    @Test
    fun `should rollback mixed file operations - existing file modified and new file created in new directory`() {
        // Given: A file that exists before the transaction
        val existingFile = File(tempDir.toFile(), "existing-file.txt")
        val originalContent = "original content before transaction"
        existingFile.writeText(originalContent)

        // Given: Active transaction
        transactionManager.startTransaction()

        // When: Create a new directory during transaction
        val newDirectory = File(tempDir.toFile(), "new-directory")
        directoryCreateTool.createDirectory(newDirectory.absolutePath)

        // When: Create a new file in the new directory during transaction
        val newFile = File(newDirectory, "new-file.txt")
        val newFileContent = "content of new file in new directory"
        fileWriteTool.writeFile(newFile.absolutePath, newFileContent)

        // When: Edit the file that existed before the transaction
        val modifiedContent = "modified content during transaction"
        fileWriteTool.writeFile(existingFile.absolutePath, modifiedContent)

        // When: Edit the file created during the transaction
        val updatedNewFileContent = "updated content of new file in new directory"
        fileWriteTool.writeFile(newFile.absolutePath, updatedNewFileContent)

        // Then: All operations should be applied
        assertTrue(newDirectory.exists())
        assertTrue(newDirectory.isDirectory())
        assertEquals(modifiedContent, existingFile.readText())
        assertEquals(updatedNewFileContent, newFile.readText())

        // When: Rollback transaction
        transactionManager.rollbackTransaction()

        // Then: The file that existed before should be restored to original content
        assertEquals(originalContent, existingFile.readText())
        assertFalse(File(existingFile.absolutePath + ".backup").exists())

        // Then: The new directory and file created during the transaction should be removed
        assertFalse(newDirectory.exists())
        assertFalse(newFile.exists())
    }

    @Test
    fun `should rollback mixed file operations - existing file modified and new file created and then removed`() {
        // Given: A file that exists before the transaction
        val existingFile = File(tempDir.toFile(), "existing-file.txt")
        val originalContent = "original content before transaction"
        existingFile.writeText(originalContent)

        // Given: Active transaction
        transactionManager.startTransaction()

        // When: Create a new file during transaction
        val newFile = File(tempDir.toFile(), "new-file.txt")
        val newFileContent = "content of new file"
        fileWriteTool.writeFile(newFile.absolutePath, newFileContent)

        // When: Edit the file that existed before the transaction
        val modifiedContent = "modified content during transaction"
        fileWriteTool.writeFile(existingFile.absolutePath, modifiedContent)

        // When: Remove the file created during the transaction
        fileWriteTool.deleteFile(newFile.absolutePath)

        // Then: The existing file should have the modified content
        assertEquals(modifiedContent, existingFile.readText())

        // When: Rollback transaction
        transactionManager.rollbackTransaction()

        // Then: The file that existed before should be restored to original content
        assertEquals(originalContent, existingFile.readText())
        assertFalse(File(existingFile.absolutePath + ".backup").exists())

        // Then: The file created during the transaction should be removed
        assertFalse(newFile.exists())
    }

    @Test
    fun `should rollback mixed file operations - existing file modified and new file created with a new directory and then removed`() {
        // Given: A file that exists before the transaction
        val existingFile = File(tempDir.toFile(), "existing-file.txt")
        val originalContent = "original content before transaction"
        existingFile.writeText(originalContent)

        // Given: Active transaction
        transactionManager.startTransaction()

        // When: Create a new file during transaction
        val newFile = File(tempDir.toFile(), "/new-directory/new-file.txt")
        val newFileContent = "content of new file"
        fileWriteTool.writeFile(newFile.absolutePath, newFileContent)

        // When: Edit the file that existed before the transaction
        val modifiedContent = "modified content during transaction"
        fileWriteTool.writeFile(existingFile.absolutePath, modifiedContent)

        // When: Remove the file created during the transaction
        fileWriteTool.deleteFile(newFile.absolutePath)

        // Then: The existing file should have the modified content
        assertEquals(modifiedContent, existingFile.readText())

        // When: Rollback transaction
        transactionManager.rollbackTransaction()

        // Then: The file that existed before should be restored to original content
        assertEquals(originalContent, existingFile.readText())
        assertFalse(File(existingFile.absolutePath + ".backup").exists())

        // Then: The file created during the transaction should be removed
        assertFalse(newFile.exists())
    }

}
