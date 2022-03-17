package data.tms

import mu.KotlinLogging

private val log = KotlinLogging.logger { }

data class TestCasesResponse(
    val maxResults: Int,
    val total: Int,
    val values: ArrayList<TestCaseResponse>
)
data class IdSelf(val id: Long, val self: String)
data class TestCaseResponse(val id: Long,
                            val key: String,
                            val name: String,
                            val project: IdSelf,
                            val createdOn: String?,
                            val objective: String?,
                            val precondition: String?,
                            val estimatedTime: Long?,
                            val priority: IdSelf,
                            val status: IdSelf,
                            val labels: ArrayList<String>,
                            val component: IdSelf?,
                            val folder: IdSelf?,
                            val owner: Owner?,
                            val testScript: TestScript?,
                            val customFields: CustomFields?
                            ) {

    fun isAutomatedByPlatform(platform: String): Boolean {
        return when (platform.toLowerCase()) {
            AndroidPlatformName -> customFields?.androidAutomationStatus == AutomationDoneStatus
            iOSPlatformName -> customFields?.iosAutomationStatus == AutomationDoneStatus
            else -> {
                log.error { "Wrong platform: $platform" }
                false
            }
        }
    }

    fun setAutomationDoneForLabel(platform: String) {
        when (platform) {
            AndroidPlatformName -> customFields?.androidAutomationStatus = AutomationDoneStatus
            iOSPlatformName -> customFields?.iosAutomationStatus = AutomationDoneStatus
            else -> log.error { "Wrong platform: $platform" }
        }
    }
}
/**
 * Automation status for custom field
 */
const val AutomationDoneStatus = "Done"
const val AndroidPlatformName = "android"
const val iOSPlatformName = "ios"

data class Owner(val self: String, val accountId: String)
data class TestScript(val self: String)