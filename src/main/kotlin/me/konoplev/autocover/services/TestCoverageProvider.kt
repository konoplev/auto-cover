package me.konoplev.autocover.services

import me.konoplev.autocover.services.report.parser.ReportParser
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

data class CommandResult(
    val exitCode: Int,
    val output: String,
    val errorOutput: String,
)

@Service
class TestCoverageProvider(
    private val reportParsers: List<ReportParser>,
    private val commandExecutionService: CommandExecutionService,
) {

    private val logger = LoggerFactory.getLogger(TestCoverageProvider::class.java)
    
    private var testCommand: String? = null
    private var reportLocation: String? = null

    /**
     * Sets the test command to be executed for coverage analysis.
     * Values are injected from application.yaml if configured.
     */
    @Value("\${agent.coverage.test-command:#{null}}")
    fun setTestCommand(command: String?) {
        this.testCommand = command
    }

    /**
     * Sets the location where the coverage report will be generated.
     * Values are injected from application.yaml if configured.
     */
    @Value("\${agent.coverage.test-result-location:#{null}}")
    fun setReportLocation(location: String?) {
        this.reportLocation = location
    }

    /**
     * Gets the current report location.
     */
    fun getReportLocation(): String? = reportLocation

    /**
     * Runs the configured test command (if provided) in [currentDirectory],
     * then parses the code coverage report at the configured result location and returns
     * the overall line coverage percentage (0.0..100.0). Returns null if unavailable.
     */
    fun getCoverage(currentDirectory: String = File("").absolutePath): Double? {
        if (!testCommand.isNullOrBlank()) {
            logger.debug("Executing test command: {} in {}", testCommand, currentDirectory)
            val result = commandExecutionService.executeCommand(testCommand!!, File(currentDirectory))
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

        val reportFile = resolvePath(currentDirectory, reportLocation!!)
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
