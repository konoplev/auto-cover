package me.konoplev.autocover.services.report.parser

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

@Component
class JacocoParser : ReportParser {

    private val logger = LoggerFactory.getLogger(JacocoParser::class.java)

    override fun isApplicable(reportContent: String): Boolean {
        val doc = parseDocument(reportContent) ?: return false
        val root = doc.documentElement
        if (root.nodeName != "report") return false
        // Must contain a LINE counter somewhere
        val counters = doc.getElementsByTagName("counter")
        for (i in 0 until counters.length) {
            val element = counters.item(i) as org.w3c.dom.Element
            if (element.getAttribute("type") == "LINE") return true
        }
        return false
    }

    override fun parseCoveragePercent(reportContent: String): Double? {
        return try {
            val doc = parseDocument(reportContent) ?: return null

            val counters = doc.getElementsByTagName("counter")

            // Prefer the global summary counter directly under <report>
            var rootCovered: Double? = null
            var rootMissed: Double? = null
            for (i in 0 until counters.length) {
                val node = counters.item(i)
                val element = node as org.w3c.dom.Element
                val type = element.getAttribute("type")
                val parent = element.parentNode
                if (type == "LINE" && parent != null && parent.nodeName == "report") {
                    rootCovered = element.getAttribute("covered").toDoubleOrNull()
                    rootMissed = element.getAttribute("missed").toDoubleOrNull()
                    break
                }
            }
            if (rootCovered != null && rootMissed != null) {
                val total = rootCovered + rootMissed
                if (total > 0) return (rootCovered / total) * 100.0
            }

            // Fallback: aggregate across all LINE counters
            var sumCovered = 0.0
            var sumMissed = 0.0
            for (i in 0 until counters.length) {
                val node = counters.item(i)
                val element = node as org.w3c.dom.Element
                if (element.getAttribute("type") == "LINE") {
                    sumCovered += element.getAttribute("covered").toDoubleOrNull() ?: 0.0
                    sumMissed += element.getAttribute("missed").toDoubleOrNull() ?: 0.0
                }
            }
            val total = sumCovered + sumMissed
            if (total > 0) return (sumCovered / total) * 100.0
            logger.warn("Unable to determine coverage from JaCoCo report content")
            null
        } catch (e: Exception) {
            logger.error("Failed to parse JaCoCo report: {}", e.message, e)
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
