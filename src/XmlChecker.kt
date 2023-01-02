import data.TsmOutput
import data.XmlCheckerOutput
import data.tms.AutomationStatus
import data.tms.StatusType
import data.tms.TestCaseResponse
import data.tms.TestCasesResponse
import data.xml.TestSuite
import mu.KotlinLogging

class XmlChecker(
    private val reportFile: String,
    private val projectKey: String,
    private val platform: String,
    private val updateCases: Boolean,
    private val suiteNameShouldContainString: String?,
    private val maxCaseResultsFromAPI: Int = 2000,
    private val jsonFileName: String = "zephyr.checker.result.json",
    private val mdFileName: String = "zephyr.checker.result.md"
) {
    private val log = KotlinLogging.logger { }
    private val tsmOutput = TsmOutput()
    private val xmlCheckerOutput = XmlCheckerOutput()
    private var deprecatedStatusId: Long? = null
    fun check() {
        // Check missing ids in test names
        val caseIdNamePairs = checkForMissingIds()
        // Check duplicate ids in test names
        xmlCheckerOutput.duplicates = checkPairsForDuplicates(caseIdNamePairs)
        // Get case list from Zephyr
        val casesResponse = zephyrClient.getTestCases(projectKey, maxCaseResultsFromAPI)
        // Get deprecated status id
        initDeprecatedStatusId()
        // Check labels
        when {
            casesResponse == null -> {
                log.error { "Case list from Jira is empty. Exiting..." }
            }

            deprecatedStatusId == null -> {
                log.error { "Deprecated status id is empty. Exiting..." }
            }

            else -> {
                xmlCheckerOutput.tsm =
                    checkIdsLabelStatusInTsm(
                        casesResponse,
                        caseIdNamePairs,
                    )
            }
        }
        // Output to file
        val reporter = Reporter(xmlCheckerOutput)
        reporter.saveOutputAsJson(jsonFileName)
        reporter.saveOutputAsMD(mdFileName)
    }

    private fun initDeprecatedStatusId() {
        deprecatedStatusId = zephyrClient.getStatuses(
            projectKey,
            statusType = StatusType.TEST_CASE
        )?.values?.findLast { it.name.lowercase() == "deprecated" }?.id
    }

    private fun checkForMissingIds(): ArrayList<Pair<String, String>> {
        val pattern = "${projectKey}_T\\d+".toRegex()
        val suites = XmlParser.parseFileAs(reportFile)
        val caseIdNamePairs = arrayListOf<Pair<String, String>>()
        // Filter suites by name
        val filteredSuites: List<TestSuite> = suites.filterIssuesBySuiteName(suiteNameShouldContainString)
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
                    log.error { "[Missing ID]: ${testCase.classname}.${caseName}" }
                    xmlCheckerOutput.missingIds.add("${testCase.classname}.${caseName}")
                }
            }
        }
        return caseIdNamePairs
    }

    private fun checkPairsForDuplicates(caseIdPairs: ArrayList<Pair<String, String>>): String {
        log.info { "Checking duplicated ids" }
        val duplicateIds = "${(caseIdPairs.groupingBy { it.first }.eachCount().filter { it.value > 1 })}"
        log.error { "Duplicates: $duplicateIds" }
        return duplicateIds
    }

    /**
     * Get all TC ids from TSM and compare them with CaseIdsPairs
     * @param casesResponse response of test cases
     * @param caseIdPairs pairs of ids and test method names
     */
    private fun checkIdsLabelStatusInTsm(
        casesResponse: TestCasesResponse,
        caseIdPairs: ArrayList<Pair<String, String>>
    ): TsmOutput {
        findCasesMissingInCode(casesResponse, caseIdPairs)
        log.info { "Comparing Code against Zephyr" }
        caseIdPairs.forEach { caseIdPair ->
            val findCase = casesResponse.values.firstOrNull { it.key == caseIdPair.first }
            val casePairString = "${caseIdPair.first} in ${caseIdPair.second}"
            // Check missing case
            if (findCase == null) {
                log.error { "[Missing in Zephyr]: $casePairString" }
                tsmOutput.shouldBeInZephyr.add(casePairString)
            } else
            // Check missed automation status
                if (!findCase.isAutomationStatusByPlatformSameAs(AutomationStatus.Done, platform)) {
                    var updateStatus = "[Update DISABLED]"
                    // Save cases with missed labels
                    log.error { "[Missing status for $platform]: $casePairString" }
                    // Update in Zephyr
                    if (updateCases) {
                        findCase.setAutomationStatusForPlatform(AutomationStatus.Done, platform)
                        val result = zephyrClient.updateCase(findCase)
                        updateStatus = getUpdateStatusStringFromResult(result)
                    }
                    tsmOutput.missingStatus.add("$updateStatus | $casePairString")
                }
            // Check deprecated cases
            checkIfCaseIsDeprecated(findCase, casePairString)
        }
        log.info { "Done" }
        return tsmOutput
    }

    private fun checkIfCaseIsDeprecated(findCase: TestCaseResponse?, casePairString: String) {
        if (findCase?.status?.id == deprecatedStatusId) {
            log.error { "[Deprecated]: $casePairString" }
            tsmOutput.deprecatedCase.add(casePairString)
        }
    }

    private fun findCasesMissingInCode(
        casesResponse: TestCasesResponse,
        caseIdPairs: ArrayList<Pair<String, String>>,
    ) {
        log.info { "Comparing TSM against Code" }
        casesResponse.values.filter {
            it.isAutomationStatusByPlatformSameAs(
                AutomationStatus.Done,
                platform
            ) && it.status.id != deprecatedStatusId
        }
            .forEach { zephyrCase ->
                val findCase = caseIdPairs.firstOrNull { it.first == zephyrCase.key }
                if (findCase == null) {
                    var updateStatus = "[Update DISABLED]"
                    log.error { "[Missing case in Code]: ${zephyrCase.toKeyNameString()}" }
                    if (updateCases) {
                        zephyrCase.setAutomationStatusForPlatform(null, platform)
                        val result = zephyrClient.updateCase(zephyrCase)
                        updateStatus = getUpdateStatusStringFromResult(result)
                    }
                    tsmOutput.shouldBeInCode.add("$updateStatus | ${zephyrCase.toKeyNameString()}")
                }
            }
    }

    private fun getUpdateStatusStringFromResult(result: Boolean): String {
        return if (result) {
            "[Update OK]"
        } else {
            "[Update FAILED]"
        }
    }
}