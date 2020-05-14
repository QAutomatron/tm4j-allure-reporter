package api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.isClientError
import com.github.kittinunf.fuel.core.isServerError
import data.jira.JiraResult
import mu.KotlinLogging

object JiraApiClient {
    private const val api = "https://api.adaptavist.io/tm4j/v2"
    var apiKey = ""
    private const val contentType = "application/json"
    private val mapper = jacksonObjectMapper()
    private val log = KotlinLogging.logger {}

    fun postTestExecution(projectKey: String, testCycleKey: String, jiraResult: JiraResult): TestCasePostResult {

        val jsonAsMap = hashMapOf(
            "projectKey" to projectKey,
            "testCaseKey" to jiraResult.testCaseKey,
            "testCycleKey" to testCycleKey,
            "statusName" to jiraResult.statusName,
            "executionTime" to jiraResult.executionTime,
            "environmentName" to jiraResult.environmentName,
            "actualEndDate" to jiraResult.actualEndDate,
            "comment" to jiraResult.comment
        )

        val (_, response, _) = Fuel.post("$api/testexecutions")
            .authentication().bearer(apiKey)
            .jsonBody(mapper.writeValueAsString(jsonAsMap))
            .response()

        return if (response.isClientError || response.isServerError) {
            val error = response.body().asString(contentType)
            log.error { "Report not posted: ${jiraResult.testCaseKey} <${jiraResult.statusName}>." +
                    "\n Error: $error" +
                    "\n Allure result: ${jiraResult.allureResult}" }
            TestCasePostResult(false, error)
        } else {
            log.info { "Report posted: ${jiraResult.testCaseKey} <${jiraResult.statusName}>" }
            TestCasePostResult(true, null)
        }
    }

    fun createTestCycle(projectKey: String, name: String, description: String): TestCycleResponse? {
        val jsonAsMap = hashMapOf(
            "projectKey" to projectKey,
            "name" to name,
            "description" to description,
            "statusName" to "Done"
        )

        val (_, response, _) = Fuel.post("$api/testcycles")
            .authentication().bearer(apiKey)
            .jsonBody(mapper.writeValueAsString(jsonAsMap))
            .response()
        .also { log.debug(it.toString()) }
        return if (response.isClientError || response.isServerError) {
            null
        } else {
            mapper.readValue(response.body().asString(contentType), TestCycleResponse::class.java)
        }
    }

    data class TestCasePostResult(
        val result: Boolean,
        val error: String?
    )
}