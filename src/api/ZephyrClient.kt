package api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import data.tms.ErrorResponse
import data.tms.JiraResult
import data.tms.TestCaseResponse
import data.tms.TestCasesResponse
import mu.KotlinLogging

class ZephyrClient(private val apiKey: String) {

    companion object {
        private const val api = "https://api.adaptavist.io/tm4j/v2"
        private const val contentType = "application/json"
        private val mapper = jacksonObjectMapper()
        private val log = KotlinLogging.logger {}
    }

    /**
     * GET /testcases
     * @param projectKey
     * @param maxResults
     */
    fun getTestCases(projectKey: String, maxResults: Int = 10): TestCasesResponse? {
        log.info { "Getting cases from TSM" }
        val (request, response, result) = Fuel.get("$api/testcases?maxResults=$maxResults&projectKey=$projectKey")
            .authentication().bearer(apiKey)
            .responseObject<TestCasesResponse>()
        responseHandler(request, response)
        return result.component1()
    }

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

    fun updateCase(updatedCase: TestCaseResponse) {
        log.info { "Will set [${updatedCase.key}] labels to [${updatedCase.labels}]" }
        val (request, response, _) = Fuel.put("$api/testcases/${updatedCase.key}")
            .authentication().bearer(apiKey)
            .jsonBody(mapper.writeValueAsString(updatedCase))
            .response()
        responseHandler(request, response)
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

    private fun responseHandler(request: Request, response: Response) {
        if (response.isSuccessful) {
            log.info { "Request ${request.method} to ${request.url} is OK" }
        } else if (response.isClientError || response.isServerError) {
            val error = mapper.readValue(response.body().asString(contentType), ErrorResponse::class.java)
            log.error { "Error updating test case: $error" }
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
}