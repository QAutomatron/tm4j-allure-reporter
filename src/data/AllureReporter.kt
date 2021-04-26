package data

import JsonParser.getAllureResultsFrom
import api.ZephyrClient
import com.github.kittinunf.fuel.core.isServerError
import data.tms.JiraResult
import mu.KotlinLogging
import zephyrClient

object AllureReporter {
    private val log = KotlinLogging.logger { }

    fun reportAllure(
        reportDir: String,
        projectKey: String,
        cycleName: String,
        cycleDescription: String
    ) {
        // Get results
        val allureResults = getAllureResultsFrom(reportDir)
        val jiraResults = arrayListOf<JiraResult>()
        allureResults.forEach { jiraResults.add(it.toJiraResult()) }

        // Create TC
        val createdTestCycle =
            zephyrClient.createTestCycle(
                projectKey,
                cycleName,
                cycleDescription
            )

        // Post Executions
        if (createdTestCycle != null) {
            log.info("Test Cycle ${createdTestCycle.key} created. Will post results")
            log.info { "Results to post: ${allureResults.size}" }
            val postedResults = postResultsToJira(projectKey, createdTestCycle.key, jiraResults)
            log.info { "Results posted ${postedResults.filter { it.posted }.size}" }
            log.info { "Results NOT posted ${postedResults.filter { !it.posted }.size}" }

            // Retry server error posts
            log.info { "Will try to post again all results with server error" }
            val retryResults = arrayListOf<ZephyrClient.TestCasePostResult>()
            postedResults.filter { !it.posted && it.response.isServerError }.forEach {
                val result = zephyrClient.postTestExecution(it.jiraResultRequest)
                retryResults.add(result)
            }
            log.info { "Results NOT posted after retry ${retryResults.filter { !it.posted }.size}" }
        } else {
            log.info("Test Cycle not created. Will not post results")
        }
    }

    /**
     * Post results to Jira. Return array of not posted.
     */
    private fun postResultsToJira(
        projectKey: String,
        testCycleKey: String,
        resultsToPost: ArrayList<JiraResult>
    ): ArrayList<ZephyrClient.TestCasePostResult> {
        val postedResults = arrayListOf<ZephyrClient.TestCasePostResult>()
        resultsToPost.forEach {
            if (it.testCaseKey.isNotEmpty()) {
                val result = zephyrClient.postTestExecution(projectKey, testCycleKey, it)
                postedResults.add(result)
            } else {
                log.error { "Test Link is missed for ${it.allureResult.fullName}" }
            }
        }
        return postedResults
    }
}