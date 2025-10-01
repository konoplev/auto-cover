package me.konoplev.autocover.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigurationHelperIntegrationTest : BaseLlamaIntegrationTest() {

    @Autowired
    private lateinit var configurationHelper: ConfigurationHelper

    @Test
    fun `test getInstructions returns valid JSON configuration for Spring sample app`() {
        // Given: Set the current directory to our test Spring application
        val testAppPath = File("src/test/resources/test-projects/spring-sample-app-no-coverage-setup").absolutePath

        assertTrue(
            configurationHelper.isConfigurationInstructionNeeded(),
            "Configuration should be needed when test command and result location are null",
        )

        // When: Get configuration
        val configuration = configurationHelper.getInstructions(testAppPath)

        // Then: Verify the instructions are not empty
        assertNotNull(configuration, "Configuration should not be null")

        // Verify the configuration was parsed successfully
        assertNotNull(configuration, "Configuration should be parsed successfully from JSON response")
        
        // Verify the configuration contains valid values
        assertFalse(
            configuration.testCommand.isBlank(),
            "Test command should not be blank. Got: ${configuration.testCommand}"
        )
        
        assertFalse(
            configuration.testResultLocation.isBlank(),
            "Test result location should not be blank. Got: ${configuration.testResultLocation}"
        )

        // Verify the configuration makes sense for a Maven project
        val hasMavenRelated = configuration.testCommand.lowercase().let {
                it.contains("mvn") &&
                it.contains("test")
        }

        assertTrue(
            hasMavenRelated,
            "Test command should mention Maven since the test app uses Maven. Got: ${configuration.testCommand}",
        )
    }
}
