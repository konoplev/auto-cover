package me.konoplev.autocover.services.report.parser

interface ReportParser {
    fun isApplicable(reportContent: String): Boolean
    fun parseCoveragePercent(reportContent: String): Double?
}
