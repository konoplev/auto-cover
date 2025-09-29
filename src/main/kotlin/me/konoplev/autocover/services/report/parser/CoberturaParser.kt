package me.konoplev.autocover.services.report.parser

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

@Component
class CoberturaParser : ReportParser {

    private val logger = LoggerFactory.getLogger(CoberturaParser::class.java)

    override fun isApplicable(reportContent: String): Boolean {
        val doc = parseDocument(reportContent) ?: return false
        val root = doc.documentElement
        return root != null && root.nodeName == "coverage"
    }

    override fun parseCoveragePercent(reportContent: String): Double? {
        return try {
            val doc = parseDocument(reportContent) ?: return null
            val coverageElement = doc.documentElement
            val lineRateAttr = coverageElement.getAttribute("line-rate")
            if (!lineRateAttr.isNullOrBlank()) {
                val rate = lineRateAttr.toDoubleOrNull()
                if (rate != null) return rate * 100.0
            }

            val linesValid = coverageElement.getAttribute("lines-valid").toDoubleOrNull()
            val linesCovered = coverageElement.getAttribute("lines-covered").toDoubleOrNull()
            if (linesValid != null && linesCovered != null && linesValid > 0) {
                return (linesCovered / linesValid) * 100.0
            }

            logger.warn("Unable to determine coverage from Cobertura report content")
            null
        } catch (e: Exception) {
            logger.error("Failed to parse Cobertura report: {}", e.message, e)
            null
        }
    }

    private fun parseDocument(reportContent: String): Document? {
        return try {
            val dbf = DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                isXIncludeAware = false
                isExpandEntityReferences = false
            }
            val db = dbf.newDocumentBuilder().apply {
                setEntityResolver { _, _ -> InputSource(StringReader("")) }
            }
            val doc = db.parse(reportContent.byteInputStream())
            doc.documentElement.normalize()
            doc
        } catch (e: Exception) {
            logger.debug("Failed to parse XML content for applicability: {}", e.message)
            null
        }
    }
}
