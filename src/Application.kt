import api.ZephyrClient
import com.apurebase.arkenv.Arkenv
import com.apurebase.arkenv.util.parse
import data.AllureArguments
import data.MainArguments
import data.XmlArguments
import mu.KotlinLogging

val log = KotlinLogging.logger {}
lateinit var zephyrClient: ZephyrClient

fun main(args: Array<String>) {
    // Parse args
    Arkenv.parse(MainArguments, args)
    // Set API client
    zephyrClient = ZephyrClient(MainArguments.token)

    // Set params
    val projectKey = MainArguments.projectKey
    val reportFile = MainArguments.reportFrom
    val mode = MainArguments.mode

    when (mode.lowercase()) {
        "debug" -> {
            log.info { "Debug mode ON. Will not post" }
            return
        }

        "allure" -> {
            log.info { "Allure mode selected" }
            Arkenv.parse(AllureArguments, args)
            AllureReporter.reportAllure(
                reportFile,
                projectKey,
                AllureArguments.cycleName,
                AllureArguments.cycleDescription
            )
        }

        "xml" -> {
            log.info { "XML mode selected" }
            Arkenv.parse(XmlArguments, args)
            log.info { "XML Arguments provided: $XmlArguments" }
            val xmlChecker = XmlChecker(
                reportFile,
                projectKey,
                XmlArguments.platform,
                XmlArguments.updateCases,
                XmlArguments.suiteNameContains
            )
            xmlChecker.check()
        }

        else -> {
            log.info { "No mode selected, please select 'allure', 'debug' or 'xml'" }
        }
    }
}