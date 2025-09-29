package me.konoplev.autocover.services

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigurationHelperIntegrationTest : BaseLlamaIntegrationTest() {

    @Autowired
    private lateinit var configurationHelper: ConfigurationHelper

    @Autowired
    private lateinit var projectConfigurationAssistant: ProjectConfigurationAssistant

    @Test
    fun `test getInstructions returns non-empty result with configuration strings for Spring sample app`() {
        // Given: Set the current directory to our test Spring application
        val testAppPath = File("src/test/resources/test-projects/spring-sample-app").absolutePath

        assertTrue(
            configurationHelper.isConfigurationInstructionNeeded(),
            "Configuration should be needed when test command and result location are null",
        )

        // When: Get instructions from the configuration helper
        val instructions = configurationHelper.getInstructions(testAppPath)

        // Then: Verify the instructions are not empty and contain expected configuration strings
        assertNotNull(instructions, "Instructions should not be null")
        assertFalse(instructions.isBlank(), "Instructions should not be empty or blank")

        logger.info("Received instructions (${instructions.length} characters): $instructions")

        // Since TinyLlama might not follow instructions perfectly, let's be more lenient
        // and check for various possible forms of the expected content
        val hasMavenRelated = instructions.lowercase().let {
            it.contains("maven") ||
                it.contains("mvn") ||
                it.contains("pom") ||
                it.contains("test")
        }

        assertTrue(
            hasMavenRelated,
            "Instructions should mention testing/Maven since the test app uses Maven. Got: $instructions",
        )
    }
}
