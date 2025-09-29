package me.konoplev.autocover.services.report.parser

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JacocoParserTest {

    private val parser = JacocoParser()

    @Test
    fun `should detect applicability and parse coverage from Jacoco report`() {
        val url = this::class.java.getResource("/reports/jacoco/jacoco.xml")
        assertNotNull(url, "JaCoCo report resource not found")
        val content = url.readText()

        assertTrue(parser.isApplicable(content))

        val coverage = parser.parseCoveragePercent(content)
        assertNotNull(coverage)
        assertEquals(2.941, coverage / 1.0, 0.01)
    }
}
