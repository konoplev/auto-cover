package me.konoplev.autocover.services

import me.konoplev.autocover.tools.FileSystemTool
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

data class TransactionState(
    val createdFiles: MutableList<String> = mutableListOf(),
    val createdDirectories: MutableList<String> = mutableListOf(),
    val modifiedFiles: MutableList<Pair<String, String>> = mutableListOf(), // original file -> backup file
    val deletedItems: MutableList<Pair<String, String>> = mutableListOf(), // original path -> backup path (.removed)
    val preExistingFiles: MutableSet<String> = mutableSetOf(), // files that existed before transaction
    val preExistingDirectories: MutableSet<String> = mutableSetOf(), // directories that existed before transaction
    val startTimeMillis: Long = System.currentTimeMillis() // timestamp when transaction started
)

@Component
class FileSystemTransactionManager : FileSystemTool {

    private val logger = LoggerFactory.getLogger(FileSystemTransactionManager::class.java)
    private var currentTransaction: TransactionState? = null

    fun startTransaction() {
        try {
            if (currentTransaction != null) {
                logger.warn("Transaction already active. Please commit or rollback current transaction first.")
                return
            } else {
                currentTransaction = TransactionState()
                logger.debug("Started transaction")
            }
        } catch (e: Exception) {
            val errorMessage = "Error starting transaction: ${e.message}"
            logger.error(errorMessage, e)
        }
    }

    fun commitTransaction() {
        try {
            val transaction = currentTransaction
            if (transaction == null) {
                logger.warn("No active transaction to commit")
                return
            }

            logger.debug("Committing transaction")

            // Remove all backup files and .removed markers since changes are being committed
            transaction.modifiedFiles.forEach { (_, backupPath) ->
                try {
                    val backupFile = File(backupPath)
                    if (backupFile.exists()) {
                        backupFile.delete()
                        logger.debug("Removed backup file: {}", backupPath)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to remove backup file: {}", backupPath, e)
                }
            }

            transaction.deletedItems.forEach { (_, removedPath) ->
                try {
                    val removedFile = File(removedPath)
                    if (removedFile.exists()) {
                        removeRecursively(removedFile)
                        logger.debug("Removed .removed marker: {}", removedPath)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to remove .removed marker: {}", removedPath, e)
                }
            }
            currentTransaction = null
        } catch (e: Exception) {
            val errorMessage = "Error committing transaction: ${e.message}"
            logger.error(errorMessage, e)
        }
    }

    fun rollbackTransaction() {
        try {
            val transaction = currentTransaction
            if (transaction == null) {
                logger.warn("No active transaction to rollback")
                return
            }

            logger.debug("Rolling back the transaction")
            logger.debug("Modified files to restore: {}", transaction.modifiedFiles)

            // Remove all created files and directories (in reverse order)
            transaction.createdFiles.reversed().forEach { filePath ->
                try {
                    val file = File(filePath)
                    if (file.exists()) {
                        file.delete()
                        logger.debug("Removed created file: {}", filePath)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to remove created file: {}", filePath, e)
                }
            }

            transaction.createdDirectories.reversed().forEach { dirPath ->
                try {
                    val dir = File(dirPath)
                    if (dir.exists() && dir.isDirectory()) {
                        dir.delete()
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to remove created directory: {}", dirPath, e)
                }
            }

            // Restore modified files from backups
            transaction.modifiedFiles.forEach { (originalPath, backupPath) ->
                try {
                    val originalFile = File(originalPath)
                    val backupFile = File(backupPath)
                    if (backupFile.exists()) {
                        Files.copy(backupFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        backupFile.delete()
                        logger.debug("Restored file from backup: {} -> {}", backupPath, originalPath)
                    } else {
                        logger.warn("Backup file does not exist: {}", backupPath)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to restore file from backup: {} -> {}", backupPath, originalPath, e)
                }
            }

            // Restore deleted items from .removed backups
            transaction.deletedItems.forEach { (originalPath, removedPath) ->
                try {
                    val originalFile = File(originalPath)
                    val removedFile = File(removedPath)
                    if (removedFile.exists()) {
                        if (removedFile.isDirectory()) {
                            copyDirectoryRecursively(removedFile, originalFile)
                        } else {
                            Files.copy(removedFile.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                        removeRecursively(removedFile)
                        logger.debug("Restored deleted item: {} -> {}", removedPath, originalPath)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to restore deleted item: {} -> {}", removedPath, originalPath, e)
                }
            }

            currentTransaction = null
        } catch (e: Exception) {
            val errorMessage = "Error rolling back transaction: ${e.message}"
            logger.error(errorMessage, e)
        }
    }

    // Internal methods for transaction tracking
    internal fun trackFileCreation(filePath: String) {
        currentTransaction?.let { transaction ->
            transaction.createdFiles.add(filePath)
            logger.debug("Tracked file creation: {}", filePath)
        }
    }

    internal fun trackDirectoryCreation(dirPath: String) {
        currentTransaction?.let { transaction ->
            transaction.createdDirectories.add(dirPath)
            logger.debug("Tracked directory creation: {}", dirPath)
        }
    }

    internal fun trackPreExistingFile(filePath: String) {
        currentTransaction?.let { transaction ->
            transaction.preExistingFiles.add(filePath)
            logger.debug("Tracked pre-existing file: {}", filePath)
        }
    }

    internal fun trackPreExistingDirectory(dirPath: String) {
        currentTransaction?.let { transaction ->
            transaction.preExistingDirectories.add(dirPath)
            logger.debug("Tracked pre-existing directory: {}", dirPath)
        }
    }

    internal fun isFilePreExisting(filePath: String): Boolean {
        return currentTransaction?.let { transaction ->
            transaction.preExistingFiles.contains(filePath)
        } ?: false
    }

    internal fun wasFileCreatedDuringTransaction(filePath: String): Boolean {
        return currentTransaction?.let { transaction ->
            transaction.createdFiles.contains(filePath)
        } ?: false
    }

    internal fun isDirectoryPreExisting(dirPath: String): Boolean {
        return currentTransaction?.let { transaction ->
            transaction.preExistingDirectories.contains(dirPath)
        } ?: false
    }

    internal fun getTransactionStartTime(): Long? {
        return currentTransaction?.startTimeMillis
    }

    internal fun trackFileModification(filePath: String) {
        currentTransaction?.let { transaction ->
            val file = File(filePath)
            if (file.exists()) {
                // Only create backup if this file was pre-existing (not created during transaction)
                if (isFilePreExisting(filePath)) {
                    val backupPath = "$filePath.backup"
                    try {
                        Files.copy(file.toPath(), Paths.get(backupPath), StandardCopyOption.REPLACE_EXISTING)
                        transaction.modifiedFiles.add(filePath to backupPath)
                        logger.debug("Created backup for pre-existing file modification: {} -> {}", filePath, backupPath)
                    } catch (e: Exception) {
                        logger.warn("Failed to create backup for: {}", filePath, e)
                    }
                } else {
                    logger.debug("Skipping backup for file created during transaction: {}", filePath)
                }
            }
        }
    }

    internal fun trackItemDeletion(itemPath: String) {
        currentTransaction?.let { transaction ->
            val item = File(itemPath)
            if (item.exists()) {
                // Check if this item was created during the transaction
                val wasCreatedDuringTransaction = transaction.createdFiles.contains(itemPath) || 
                    transaction.createdDirectories.contains(itemPath)
                
                if (wasCreatedDuringTransaction) {
                    // Item was created during transaction, just remove it from created lists
                    transaction.createdFiles.remove(itemPath)
                    transaction.createdDirectories.remove(itemPath)
                    logger.debug("Removed created item from tracking (no backup needed): {}", itemPath)
                } else {
                    // Item existed before transaction, create backup for restoration
                    val removedPath = "$itemPath.removed"
                    try {
                        if (item.isDirectory()) {
                            copyDirectoryRecursively(item, File(removedPath))
                        } else {
                            Files.copy(item.toPath(), Paths.get(removedPath), StandardCopyOption.REPLACE_EXISTING)
                        }
                        transaction.deletedItems.add(itemPath to removedPath)
                        logger.debug("Created .removed backup for deletion: {} -> {}", itemPath, removedPath)
                    } catch (e: Exception) {
                        logger.warn("Failed to create .removed backup for: {}", itemPath, e)
                    }
                }
            }
        }
    }

    private fun copyDirectoryRecursively(source: File, target: File) {
        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdirs()
            }
            source.listFiles()?.forEach { child ->
                copyDirectoryRecursively(child, File(target, child.name))
            }
        } else {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun removeRecursively(file: File) {
        if (file.isDirectory()) {
            file.listFiles()?.forEach { child ->
                removeRecursively(child)
            }
        }
        file.delete()
    }
}
