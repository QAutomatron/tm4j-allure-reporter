package api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.isClientError
import com.github.kittinunf.fuel.core.isServerError
import com.github.kittinunf.fuel.core.isSuccessful
import data.jira.JiraResult
import mu.KotlinLogging

object JiraApiClient {
    private const val api = "https://api.adaptavist.io/tm4j/v2"
    var apiKey = ""
    private const val contentType = "application/json"
    private val mapper = jacksonObjectMapper()
    private val log = KotlinLogging.logger {}

    fun postTestExecution(projectKey: String, testCycleKey: String, jiraResult: JiraResult): TestCasePostResult {

        val jiraResultRequest = JiraResultRequest(
            projectKey = projectKey,
            testCaseKey = jiraResult.testCaseKey,
            testCycleKey = testCycleKey,
            statusName = jiraResult.statusName,
            executionTime = jiraResult.executionTime,
            environmentName = jiraResult.environmentName,
            actualEndDate = jiraResult.actualEndDate,
            comment = jiraResult.comment
        )

        return postTestExecution(jiraResultRequest)
    }

    fun postTestExecution(jiraResultRequest: JiraResultRequest): TestCasePostResult {
        val (_, response, _) = Fuel.post("$api/testexecutions")
            .authentication().bearer(apiKey)
            .jsonBody(mapper.writeValueAsString(jiraResultRequest))
            .response()
        return handleJiraResultResponse(response, jiraResultRequest)
    }

    private fun handleJiraResultResponse(
        response: Response,
        jiraResultRequest: JiraResultRequest
    ): TestCasePostResult {
        val result = TestCasePostResult(response, jiraResultRequest)
        log.info {
            "Report posted status <${result.posted}>. " +
                    "\n Code ${result.response.statusCode}: ${result.jiraResultRequest.testCaseKey} <${result.jiraResultRequest.statusName}>." +
                    "\n Error: ${result.error}"
        }
        return result
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
        val response: Response,
        val jiraResultRequest: JiraResultRequest
    ) {
        val posted get() = response.isSuccessful
        val error: ErrorResponse?
            get() {
                return if (response.isClientError || response.isServerError) {
                    mapper.readValue(response.body().asString(contentType), ErrorResponse::class.java)
                } else null
            }
    }

    data class JiraResultRequest(
        val projectKey: String,
        val testCaseKey: String,
        val testCycleKey: String,
        val statusName: String,
        val executionTime: Long,
        val environmentName: String,
        val actualEndDate: String,
        val comment: String
    )

    data class ErrorResponse(val errorCode: Int?, val message: String?)
}