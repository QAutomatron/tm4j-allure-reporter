import api.JiraApiClient
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.isServerError
import com.natpryce.konfig.*
import data.allure.AllureResult
import data.jira.JiraResult
import mu.KotlinLogging
import java.io.File

// Arguments
val jiraApiKey = Key("jiraApiKey", stringType)
val projectKey = Key("projectKey", stringType)
val cycleName = Key("cycleName", stringType)
val cycleDescription = Key("cycleDescription", stringType)
val reportDir = Key("reportDir", stringType)
val debug = Key("debug", booleanType)

val log = KotlinLogging.logger {}

fun main(args: Array<String>) {
    val config = loadConfig(args)
    // Set API key
    JiraApiClient.apiKey = config[jiraApiKey]

    // Set params
    val projectKey = config[projectKey]
    val cycleName = config[cycleName]
    val cycleDescription = config[cycleDescription]
    val debug = config[debug]

    // Skip if debug
    if (debug) {
        log.info { "Debug mode ON. Will not post" }
        return
    } else {
        log.info { "Debug mode OFF. Will post results" }
    }

    // Get results
    val allureResults = getResultsFromDirectory(config[reportDir])
    val jiraResults = arrayListOf<JiraResult>()
    allureResults.forEach { jiraResults.add(it.toJiraResult()) }

    // Create TC
    val createdTestCycle =
        JiraApiClient.createTestCycle(
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
        val retryResults = arrayListOf<JiraApiClient.TestCasePostResult>()
        postedResults.filter { !it.posted && it.response.isServerError}.forEach {
            val result = JiraApiClient.postTestExecution(it.jiraResultRequest)
            retryResults.add(result)
        }
        log.info { "Results NOT posted after retry ${retryResults.filter { !it.posted }.size}" }
    } else {
        log.info("Test Cycle not created. Will not post results")
    }
}

private fun loadConfig(args: Array<String>): Configuration {
    val (config) = parseArgs(
        args,
        CommandLineOption(jiraApiKey),
        CommandLineOption(projectKey),
        CommandLineOption(cycleName),
        CommandLineOption(cycleDescription),
        CommandLineOption(reportDir),
        CommandLineOption(debug)
    )

    // Config
    log.info { "Arguments:" }
//    log.info { "jiraApiKey: ${config[jiraApiKey]}" }
    log.info { "projectKey: ${config[projectKey]}" }
    log.info { "cycleName: ${config[cycleName]}" }
    log.info { "cycleDescription: ${config[cycleDescription]}" }
    log.info { "reportDir: ${config[reportDir]}" }

    return config
}

/**
 * Post results to Jira. Return array of not posted.
 */
fun postResultsToJira(projectKey: String, testCycleKey: String, resultsToPost: ArrayList<JiraResult>): ArrayList<JiraApiClient.TestCasePostResult> {
    val postedResults = arrayListOf<JiraApiClient.TestCasePostResult>()
    resultsToPost.forEach {
        if (it.testCaseKey.isNotEmpty()) {
            val result = JiraApiClient.postTestExecution(projectKey, testCycleKey, it)
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