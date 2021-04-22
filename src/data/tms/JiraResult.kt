package data.tms

import data.allure.AllureResult

data class JiraResult(
    val testCaseKey: String,
    val statusName: String,
    val executionTime: Long,
    val environmentName: String,
    val actualEndDate: String,
    val comment: String,
    val allureResult: AllureResult
)