package data

import data.tms.AutomationStatus

data class XmlCheckerOutput(
    val missingIds: ArrayList<String> = arrayListOf(),
) {
    var duplicates: String = ""
    var tsm: TsmOutput = TsmOutput()
    var coverage: Coverage? = null
}
data class Coverage(val total: Int, val notDeprecated: Int,  val byStatus: ArrayList<CountByStatus>)
data class CountByStatus(val status: AutomationStatus, val count: Int)
data class TsmOutput(
    val shouldBeInZephyr: ArrayList<String> = arrayListOf(),
    val shouldBeInCode: ArrayList<String> = arrayListOf(),
    val deprecatedCase: ArrayList<String> = arrayListOf(),
    val missingStatus: ArrayList<String> = arrayListOf()
)