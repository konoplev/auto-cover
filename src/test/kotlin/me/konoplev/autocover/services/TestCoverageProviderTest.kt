package me.konoplev.autocover.services

import me.konoplev.autocover.config.AgentProperties
import me.konoplev.autocover.config.CoverageProperties
import me.konoplev.autocover.services.report.parser.CoberturaParser
import me.konoplev.autocover.services.report.parser.JacocoParser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private val projectDir = File("src/test/resources/test-projects/spring-sample-app-with-jacoco")

class TestCoverageProviderTest {

    private lateinit var testCoverageProvider: TestCoverageProvider

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        val agentProperties = AgentProperties(
            coverage = CoverageProperties(
                testCommand = "mvn verify",
                testResultLocation = "target/site/jacoco/jacoco.xml",
            ),
        )
        val reportParsers = listOf(CoberturaParser(), JacocoParser())
        val commandExecutionService = CommandExecutionService()
        testCoverageProvider = TestCoverageProvider(agentProperties, reportParsers, commandExecutionService)
    }

    @AfterEach
    fun tearDown() {
        // Clean up target directory to ensure clean test state
        val targetDir = File(projectDir, "target")
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
    }

    @Test
    fun `should get coverage from spring-sample-app-with-jacoco project`() {
        // Given: Test project directory
        assertTrue(projectDir.exists(), "Test project directory should exist")

        // When: Get coverage
        val coverage = testCoverageProvider.getCoverage(projectDir.absolutePath)

        // Then: Should return non-null, non-zero coverage
        assertNotNull(coverage, "Coverage should not be null")
        assertTrue(coverage > 0.0, "Coverage should be greater than 0, got: $coverage")
    }
}
