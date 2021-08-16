import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import data.TsmOutput
import data.XmlCheckerOutput
import data.tms.StatusType
import data.tms.TestCasesResponse
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
        // Check missing ids in test names
        val caseIdNamePairs = checkForMissingIds(projectKey, reportDir, suiteNameContains, output)
        // Check duplicate ids in test names
        output.duplicates = checkPairsForDup(caseIdNamePairs)
        // Get case list from Zephyr
        val casesResponse = zephyrClient.getTestCases(projectKey, maxCaseResults)
        // Get deprecated status id
        val deprecatedStatusId = zephyrClient.getStatuses(
            projectKey,
            statusType = StatusType.TEST_CASE
        )?.values?.findLast { it.name.toLowerCase() == "deprecated" }?.id
        // Check labels
        if (casesResponse != null && deprecatedStatusId != null) {
            output.tsm =
                checkIdsLabelStatusInTsm(
                    casesResponse,
                    caseIdNamePairs,
                    automationLabel,
                    updateCases,
                    deprecatedStatusId
                )
        } else {
            log.error { "Case list from Jira is empty or deprecated status id is empty" }
        }

        // Output to file
        val jsonFileName = "zephyr.checker.result.json"
        val mdFileName = "zephyr.checker.result.md"
        saveOutputAsJson(jsonFileName, output)
        saveOutputAsMD(mdFileName, output)
    }

    private fun saveOutputAsMD(fileName: String, output: XmlCheckerOutput) {
        val detailsOpen = "<details>"
        val detailsClose = "</details>"
        File(fileName).printWriter().use { out ->
            out.println("### Zephyr integration check")
            if (output.missingIds.isNotEmpty()) {
                out.println(detailsOpen)
                out.println("<summary>Missing IDs:</summary>")
                out.println("")
                output.missingIds.forEach { out.println("- $it") }
                out.println(detailsClose)
            }
            if (output.duplicates.isNotBlank()) {
                out.println(detailsOpen)
                out.println("<summary>Duplicated IDs:</summary>")
                out.println("")
                out.println(output.duplicates)
                out.println(detailsClose)
            }
            if (output.tsm.missingInCode.isNotEmpty()) {
                out.println(detailsOpen)
                out.println("<summary>Missing Cases in CODE:</summary>")
                out.println("")
                output.tsm.missingInCode.forEach { out.println("- $it") }
                out.println(detailsClose)
            }
            if (output.tsm.missingInZephyr.isNotEmpty()) {
                out.println(detailsOpen)
                out.println("<summary>Missing Cases in ZEPHYR:</summary>")
                out.println("")
                output.tsm.missingInZephyr.forEach { out.println("- $it") }
                out.println(detailsClose)
            }
            if (output.tsm.missingLabel.isNotEmpty()) {
                out.println(detailsOpen)
                out.println("<summary>Missing Labels in ZEPHYR:</summary>")
                out.println("")
                output.tsm.missingLabel.forEach { out.println("- $it") }
                out.println(detailsClose)
            }
            if (output.tsm.deprecatedCase.isNotEmpty()) {
                out.println(detailsOpen)
                out.println("<summary>Deprecated Cases in ZEPHYR:</summary>")
                out.println("")
                output.tsm.deprecatedCase.forEach { out.println("- $it") }
                out.println(detailsClose)
            }
            log.info { "Output saved to file $fileName" }
        }
    }

    private fun saveOutputAsJson(fileName: String, output: XmlCheckerOutput) {
        val mapper = jacksonObjectMapper()
        val prettyPrinter = DefaultPrettyPrinter()
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
        mapper.setDefaultPrettyPrinter(prettyPrinter)
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
     * @param casesResponse response of test cases
     * @param caseIdPairs pairs of ids and test method names
     */
    private fun checkIdsLabelStatusInTsm(
        casesResponse: TestCasesResponse,
        caseIdPairs: ArrayList<Pair<String, String>>,
        automationLabel: String,
        updateCases: Boolean,
        deprecatedStatusId: Long
    ): TsmOutput {
        val tsmOutput = TsmOutput()
        log.info { "Will compare TSM with Code" }
        casesResponse.values.filter { it.labels.contains(automationLabel) && it.status.id != deprecatedStatusId }.forEach {
            zephyrCase ->
            val findCase = caseIdPairs.firstOrNull { it.first == zephyrCase.key }
            val casePairString = "${zephyrCase.key} ${zephyrCase.name}"
            if (findCase == null) {
                log.error { "[Missing case in Code]: $casePairString" }
                tsmOutput.missingInCode.add(casePairString)
            }
        }
        log.info { "Will compare Code with TSM" }
        caseIdPairs.forEach { caseIdPair ->
            val findCase = casesResponse.values.firstOrNull { it.key == caseIdPair.first }
            val casePairString = "${caseIdPair.first} in ${caseIdPair.second}"
            // Check missing case
            if (findCase == null) {
                log.error { "[Missing case in TSM]: $casePairString" }
                tsmOutput.missingInZephyr.add(casePairString)
            } else
                // Check missed automation label
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
                // Check deprecated cases
                if (findCase?.status?.id == deprecatedStatusId) {
                    log.error { "[Deprecated case]: $casePairString" }
                    tsmOutput.deprecatedCase.add(casePairString)
                }
        }
        log.info { "Done" }
        return tsmOutput
    }
}