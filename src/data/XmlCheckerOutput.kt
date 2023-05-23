package data

import data.tms.AutomationStatus

data class XmlCheckerOutput(
    val missingIds: ArrayList<String> = arrayListOf(),
) {
    var duplicates: String = ""
    var tsm: TsmOutput = TsmOutput()
    var coverageReport: CoverageReport? = null
}

data class CoverageReport(
    val total: Int,
    val notDeprecated: Int,
    val coveragePercentage: Coverage,
    val countByStatuses: MutableMap<AutomationStatus, Int>
)

data class Coverage(
    val totalWontPercentage: String,
    val totalCanBePercentage: String,
    val automatedFromCanBe: String,
    val automatedFromTotal: String,
    val notAutomatedPercentageFromCanBe: String,
    val notAutomatedFromTotal: String
    )
data class TsmOutput(
    val shouldBeInZephyr: ArrayList<String> = arrayListOf(),
    val shouldBeInCode: ArrayList<String> = arrayListOf(),
    val deprecatedCase: ArrayList<String> = arrayListOf(),
    val missingStatus: ArrayList<String> = arrayListOf()
)