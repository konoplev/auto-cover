package me.konoplev.autocover.services

import me.konoplev.autocover.config.AgentProperties
import me.konoplev.autocover.services.report.parser.ReportParser
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

data class CommandResult(
    val exitCode: Int,
    val output: String,
    val errorOutput: String,
)

@Service
class TestCoverageProvider(
    private val agentProperties: AgentProperties,
    private val reportParsers: List<ReportParser>,
    private val commandExecutionService: CommandExecutionService,
) {

    private val logger = LoggerFactory.getLogger(TestCoverageProvider::class.java)

    /**
     * Runs the configured test command (if provided) in [currentDirectory],
     * then parses the Cobertura XML at the configured result location and returns
     * the overall line coverage percentage (0.0..100.0). Returns null if unavailable.
     */
    fun getCoverage(currentDirectory: String = File("").absolutePath): Double? {
        val testCommand = agentProperties.coverage.testCommand
        val reportLocation = agentProperties.coverage.testResultLocation

        if (!testCommand.isNullOrBlank()) {
            logger.debug("Executing test command: {} in {}", testCommand, currentDirectory)
            val result = commandExecutionService.executeCommand(testCommand, File(currentDirectory))
            if (result.exitCode != 0) {
                logger.warn("Test command failed with exit code {}: {}", result.exitCode, result.errorOutput)
                // Continue to attempt parsing existing report if present
            }
        } else {
            logger.debug("No test command configured; skipping test execution")
        }

        if (reportLocation.isNullOrBlank()) {
            logger.warn("No test result location configured; cannot compute coverage")
            return null
        }

        val reportFile = resolvePath(currentDirectory, reportLocation)
        if (!reportFile.exists()) {
            logger.warn("Report not found at: {}", reportFile.absolutePath)
            return null
        }

        val content = reportFile.readText()
        val parser = reportParsers.firstOrNull { it.isApplicable(content) }
        if (parser == null) {
            logger.warn("No applicable parser found for report at {}", reportFile.absolutePath)
            return null
        }
        return parser.parseCoveragePercent(content)
    }

    private fun resolvePath(baseDir: String, path: String): File {
        val file = File(path)
        return if (file.isAbsolute) file else File(baseDir, path)
    }
}
