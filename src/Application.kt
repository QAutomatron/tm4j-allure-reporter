import api.ZephyrClient
import com.apurebase.arkenv.Arkenv
import com.apurebase.arkenv.util.parse
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.isServerError
import data.AllureArguments
import data.MainArguments
import data.XmlArguments
import data.allure.AllureResult
import data.tms.JiraResult
import mu.KotlinLogging
import java.io.File

val log = KotlinLogging.logger {}
lateinit var zephyrClient: ZephyrClient

fun main(args: Array<String>) {
    Arkenv.parse(MainArguments, args)
    // Set API client
    zephyrClient = ZephyrClient(MainArguments.token)

    // Set params
    val projectKey = MainArguments.projectKey
    val reportDir = MainArguments.reportFrom
    val mode = MainArguments.mode

    when (mode.toLowerCase()) {
        "debug" -> {
            log.info { "Debug mode ON. Will not post" }
            return
        }
        "allure" -> {
            log.info { "Allure mode selected" }
            Arkenv.parse(AllureArguments, args)
            reportAllure(reportDir, projectKey, AllureArguments.cycleName, AllureArguments.cycleDescription)
        }
        "xml" -> {
            log.info { "XML mode selected" }
            Arkenv.parse(XmlArguments, args)
            XmlChecker.checkXml(reportDir, projectKey, XmlArguments.automationLabel, XmlArguments.updateCases)
        }
        else -> {
            log.info { "No mode selected, please select 'allure', 'debug' or 'xml'" }
        }
    }
}

private fun reportAllure(
    reportDir: String,
    projectKey: String,
    cycleName: String,
    cycleDescription: String
) {
    // Get results
    val allureResults = getResultsFromDirectory(reportDir)
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
fun postResultsToJira(
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

fun getResultsFromDirectory(pathToReportDir: String): ArrayList<AllureResult> {
    val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val allureResults = arrayListOf<AllureResult>()

    File(pathToReportDir).walk().forEach {
        if (it.extension == "json" && it.nameWithoutExtension.endsWith("-result")) {
            val allureResult: AllureResult = mapper.readValue(it)
            log.debug { allureResult }
            allureResults.add(allureResult)
        }
    }
    return allureResults
}