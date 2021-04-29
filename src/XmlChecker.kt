import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import data.TsmOutput
import data.XmlCheckerOutput
import data.xml.TestSuite
import mu.KotlinLogging
import java.io.File


object XmlChecker {
    private val log = KotlinLogging.logger { }
    private const val maxCaseResults = 1500

    fun checkXml(
        reportDir: String,
        projectKey: String,
        automationLabel: String,
        updateCases: Boolean,
        suiteNameContains: String?
    ) {
        val output = XmlCheckerOutput()
        val caseIdNamePairs = checkForMissingIds(projectKey, reportDir, suiteNameContains, output)
        output.duplicates = checkPairsForDup(caseIdNamePairs)
        output.tsm = checkIdsAndLabelInTsm(projectKey, caseIdNamePairs, automationLabel, maxResults = maxCaseResults, updateCases)

        // Output to file
        val mapper = jacksonObjectMapper()
        val prettyPrinter = DefaultPrettyPrinter()
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
        mapper.setDefaultPrettyPrinter(prettyPrinter)
        val fileName = "zephyr.checker.result.json"
        File(fileName).writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output))
        log.info { "Output saved to file $fileName" }
    }

    private fun checkForMissingIds(
        projectKey: String,
        reportDir: String,
        suiteNameContains: String?,
        output: XmlCheckerOutput
    ): ArrayList<Pair<String, String>> {
        val pattern = "${projectKey}_T\\d+".toRegex()
        val suites = XmlParser.parseFileAs(reportDir)
        val caseIdNamePairs = arrayListOf<Pair<String, String>>()
        val filteredSuites: List<TestSuite> = if (suiteNameContains != null) {
            log.info { "Test suite name should contain [$suiteNameContains]" }
            suites.testSuites.filter { it.name.contains(suiteNameContains) }
        } else {
            suites.testSuites
        }
        log.debug { "Filtered suites size: ${filteredSuites.size}" }
        filteredSuites.forEach {
            it.testcase.forEach { testCase ->
                val caseName = testCase.name
                if (pattern.containsMatchIn(caseName)) {
                    caseIdNamePairs.add(
                        Pair(
                            pattern.find(caseName)?.groupValues?.get(0).toString().replace("_", "-"),
                            caseName
                        )
                    )
                } else {
                    log.error { "[Missing case Id]: ${testCase.classname}.${caseName}" }
                    output.missingIds.add("${testCase.classname}.${caseName}")
                }
            }
        }
        return caseIdNamePairs
    }

    private fun checkPairsForDup(caseIdPairs: ArrayList<Pair<String, String>>): String {
        // Found duplicates
        log.info { "Checking duplicated ids" }
        val dupIds = "${(caseIdPairs.groupingBy { it.first }.eachCount().filter { it.value > 1 })}"
        val duplicatesString = "Duplicates: $dupIds"
        log.error { duplicatesString }
        return dupIds
    }

    /**
     * Get all TC ids from TSM and compare them with CaseIdsPairs
     * @param projectKey project key in tsm
     * @param caseIdPairs pairs of ids and test method names
     * @param maxResults maximum items retrieved from TSM per request
     */
    private fun checkIdsAndLabelInTsm(
        projectKey: String,
        caseIdPairs: ArrayList<Pair<String, String>>,
        automationLabel: String,
        maxResults: Int,
        updateCases: Boolean
    ): TsmOutput {
        // Check for existed cases in TSM
        log.info { "Will compare TC with TSM" }
        val tsmOutput = TsmOutput()
        val casesResponse = zephyrClient.getTestCases(projectKey, maxResults)
        if (casesResponse == null) {
            log.error { "TC list from TSM is null. Nothing will happens" }
            return tsmOutput
        } else {
            log.info { "Cases from TSM loaded" }
            log.info { "Max results: ${casesResponse.maxResults}" }
            val casesKeysFromJira = arrayListOf<String>()
            casesResponse.values.forEach { casesKeysFromJira.add(it.key) }

            // Check
            caseIdPairs.forEach { caseIdPair ->
                val findCase = casesResponse.values.firstOrNull { it.key == caseIdPair.first }
                val casePairString = "${caseIdPair.first} from ${caseIdPair.second}"
                if (findCase == null) {
                    log.error { "[Missing case in TSM]: $casePairString" }
                    tsmOutput.missingCase.add(casePairString)
                } else
                    if (!findCase.labels.contains(automationLabel)) {
                        log.error { "[Missing automation label $automationLabel]: $casePairString" }
                        tsmOutput.missingLabel.add(casePairString)
                        val updatedCase = findCase.copy(labels = findCase.labels.apply { add(automationLabel) })
                        if (updateCases) {
                            zephyrClient.updateCase(updatedCase)
                        } else {
                            log.info { "Case update disabled, won't update TSM" }
                        }
                    }
            }
            log.info { "Done" }
        }
        return tsmOutput
    }
}