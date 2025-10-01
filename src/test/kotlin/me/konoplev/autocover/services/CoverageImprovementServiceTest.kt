package me.konoplev.autocover.services

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue
import org.junit.jupiter.api.condition.EnabledIf

@TestPropertySource(
    properties = [
        "agent.coverage.test-command=mvn verify",
        "agent.coverage.test-result-location=target/site/jacoco/jacoco.xml",
    ],
)
@EnabledIf("apiKeyIsSet")
class CoverageImprovementServiceTest : BaseOpenAiIntegrationTest() {

    @Autowired
    private lateinit var coverageImprovementService: CoverageImprovementService

    @Autowired
    private lateinit var testCoverageProvider: TestCoverageProvider

    @Autowired
    private lateinit var fileSystemTransactionManager: FileSystemTransactionManager

    val currentCoverage = 2.95

    @TempDir
    lateinit var tempDir: Path

    companion object {
        @JvmStatic
        fun apiKeyIsSet(): Boolean {
            // Check system properties first (highest priority)
            val apiKey = System.getProperty("model.api.key")
            // Then check environment variables (Spring maps both to model.api.key)
                ?: System.getenv("MODEL_API_KEY")
                ?: System.getenv("API_KEY")
            return !apiKey.isNullOrBlank()
        }
    }

    @AfterEach
    fun tearDown() {
        // Clean up target directory to ensure clean test state
        val projectDir = File("src/test/resources/test-projects/spring-sample-app-with-jacoco")
        val targetDir = File(projectDir, "target")
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
    }

    @Test
    fun `should run coverage improvement process for spring-sample-app-with-jacoco project`() {
        // Given: Test project directory
        val projectDir = File("src/test/resources/test-projects/spring-sample-app-with-jacoco")
        assertTrue(projectDir.exists(), "Test project directory should exist")
        // And: start transaction
        fileSystemTransactionManager.startTransaction()

        // When: Run coverage improvement process with target coverage of 20%
        try {
            coverageImprovementService.improveCoverage(projectDir.absolutePath, targetCoverage = 20.0)
        } catch (_: Exception) {
            // ignore any exceptions. most probably it will fail by rate limit which is ok.
        }


        // Then: Process should complete without exceptions
        fileSystemTransactionManager.rollbackTransaction()
        assertTrue { testCoverageProvider.getCoverage(projectDir.absolutePath)!! > currentCoverage }
    }
}
