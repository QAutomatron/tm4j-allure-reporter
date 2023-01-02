package data.xml

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

const val issueKey = "key"

@JacksonXmlRootElement(localName = "testsuites")
data class TestSuites(
    @get: JacksonXmlProperty(isAttribute = true) val name: String?,
    @get: JacksonXmlProperty(isAttribute = true) val tests: Int,
    @get: JacksonXmlProperty(isAttribute = true) val time: String?,
    @get: JacksonXmlProperty(isAttribute = true) val failures: String,
    @get: JacksonXmlProperty(isAttribute = true) val retries: String?,
    @JacksonXmlProperty(localName = "testsuite") val testSuites: ArrayList<TestSuite> = arrayListOf()
) {
    internal fun filterIssuesBySuiteName(suiteNameContains: String?): List<TestSuite> {
        return if (suiteNameContains != null) {
            this.testSuites.filter { it.name.contains(suiteNameContains) }
        } else {
            this.testSuites
        }
    }
}

data class TestSuite(
    @get: JacksonXmlProperty(isAttribute = true) val errors: String?,
    @get: JacksonXmlProperty(isAttribute = true) val failures: String,
    @get: JacksonXmlProperty(isAttribute = true) val hostname: String?,
    @get: JacksonXmlProperty(isAttribute = true) val timestamp: String?,
    @get: JacksonXmlProperty(isAttribute = true) val skipped: String?,
    @get: JacksonXmlProperty(isAttribute = true) val tests: String,
    @get: JacksonXmlProperty(isAttribute = true) val time: String?,
    @get: JacksonXmlProperty(isAttribute = true) val name: String,
    @JacksonXmlProperty(localName = "properties") val properties: Properties?,
    @JacksonXmlProperty(localName = "testcase") val testcase: ArrayList<TestCase> = arrayListOf()
) {
    val caseStatus: String?
        get() {
            val singleTest = tests == "1"
            val skipped = skipped == "1"
            val failed = failures == "1"
            return if (!failed && singleTest && !skipped) {
                "Pass"
            } else if (failed && singleTest && !skipped) {
                "Fail"
            } else if (!failed && singleTest && skipped) {
                "Not Executed"
            } else {
                null
            }
        }

    fun isPropertyValid(projectKey: String) =
        properties != null && properties.property[0].name == issueKey && properties.property[0].value.startsWith("$projectKey-T")
}

data class TestCase(
    @get: JacksonXmlProperty(isAttribute = true) val classname: String,
    @get: JacksonXmlProperty(isAttribute = true) val name: String,
    @get: JacksonXmlProperty(isAttribute = true) val time: String?,
    @get: JacksonXmlProperty(isAttribute = true) val retries: String?,
    @get: JacksonXmlProperty val failure: String?,
    @get:JacksonXmlProperty val skipped: String?
)

data class Property(
    @get: JacksonXmlProperty(isAttribute = true) val name: String,
    @get: JacksonXmlProperty(isAttribute = true) val value: String
)
data class Properties(
    @JacksonXmlProperty(localName = "property") val property: ArrayList<Property> = arrayListOf()
)