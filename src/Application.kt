import JsonParser.getAllureResultsFrom
import api.ZephyrClient
import com.apurebase.arkenv.Arkenv
import com.apurebase.arkenv.util.parse
import com.github.kittinunf.fuel.core.isServerError
import data.AllureArguments
import data.AllureReporter
import data.MainArguments
import data.XmlArguments
import data.tms.JiraResult
import mu.KotlinLogging

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
            AllureReporter.reportAllure(reportDir, projectKey, AllureArguments.cycleName, AllureArguments.cycleDescription)
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