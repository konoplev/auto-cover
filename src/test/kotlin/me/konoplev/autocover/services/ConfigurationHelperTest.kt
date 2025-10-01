package me.konoplev.autocover.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import me.konoplev.autocover.config.AgentProperties
import me.konoplev.autocover.config.CoverageProperties
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigurationHelperTest {

    private val agentProperties = AgentProperties(
        coverage = CoverageProperties(testCommand = null, testResultLocation = null)
    )

    private val projectConfigurationAssistant = object : ProjectConfigurationAssistant {
        override fun chat(message: String): String = "mock response"
    }

    private val objectMapper = ObjectMapper().registerModule(kotlinModule())

    @Test
    fun `parseConfigurationResponse should parse valid JSON correctly`() {
        // Given
        val configurationHelper = ConfigurationHelper(agentProperties, projectConfigurationAssistant, objectMapper)
        val jsonResponse = """
            {
                "testCommand": "mvn test",
                "testResultLocation": "target/site/cobertura/coverage.xml"
            }
        """.trimIndent()

        // When
        val result = configurationHelper.parseConfigurationResponse(jsonResponse)

        // Then
        assertNotNull(result)
        assertEquals("mvn test", result.testCommand)
        assertEquals("target/site/cobertura/coverage.xml", result.testResultLocation)
    }

    @Test
    fun `parseConfigurationResponse should return null for invalid JSON`() {
        // Given
        val configurationHelper = ConfigurationHelper(agentProperties, projectConfigurationAssistant, objectMapper)
        val invalidJson = "invalid json"

        // When
        val result = configurationHelper.parseConfigurationResponse(invalidJson)

        // Then
        assertEquals(null, result)
    }

    @Test
    fun `isConfigurationInstructionNeeded should return true when properties are null`() {
        // Given
        val configurationHelper = ConfigurationHelper(agentProperties, projectConfigurationAssistant, objectMapper)

        // When
        val result = configurationHelper.isConfigurationInstructionNeeded()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isConfigurationInstructionNeeded should return true when properties are blank`() {
        // Given
        val agentPropertiesWithBlank = AgentProperties(
            coverage = CoverageProperties(testCommand = "", testResultLocation = "")
        )
        val configurationHelper = ConfigurationHelper(agentPropertiesWithBlank, projectConfigurationAssistant, objectMapper)

        // When
        val result = configurationHelper.isConfigurationInstructionNeeded()

        // Then
        assertTrue(result)
    }
}
