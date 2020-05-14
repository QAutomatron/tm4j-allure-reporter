package data.allure

import data.jira.JiraCaseStatus
import data.jira.JiraResult
import java.text.SimpleDateFormat
import java.util.*

data class AllureResult(
    val status: String,
    val start: Long,
    val stop: Long,
    val links: ArrayList<Link>,
    val statusDetails: StatusDetails?,
    val fullName: String
) {
    fun toJiraResult(): JiraResult {
        var key = ""
        if (links.isNotEmpty()) {
            val filtered = links.filter { it.name == "Issue" && it.url.contains("-T")}
            if (filtered.isNotEmpty()) key = filtered[0].url
        }
        val result = when (status) {
            "failed" -> JiraCaseStatus.failed
            "skipped" -> JiraCaseStatus.notExecuted
            "passed" -> JiraCaseStatus.pass
            "broken" -> JiraCaseStatus.failed
            else -> ""
        }
        val stackTrace = statusDetails?.trace ?: ""
        return JiraResult(
            testCaseKey = key,
            statusName = result,
            executionTime = stop - start,
            environmentName = "Android - Portrait",
            actualEndDate = dateToIso(stop).toString(),
            comment = stackTrace,
            allureResult = this
        )
    }
}

private fun dateToIso(epoch: Long): String? {
    // Input
    val date = Date(epoch)

    // Conversion
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(date)
}