package data.tms

import mu.KotlinLogging
import kotlin.collections.ArrayList

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

    fun isAutomationStatusByPlatformSameAs(status: AutomationStatus, platform: String): Boolean {
        return when (platform.lowercase()) {
            AndroidPlatformName -> customFields?.androidAutomationStatus == status.name
            iOSPlatformName -> customFields?.iosAutomationStatus == status.name
            else -> {
                log.error { "Wrong platform: $platform" }
                false
            }
        }
    }

    fun setAutomationStatusForPlatform(status: AutomationStatus?, platform: String) {
        when (platform) {
            AndroidPlatformName -> customFields?.androidAutomationStatus = status?.name
            iOSPlatformName -> customFields?.iosAutomationStatus = status?.name
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

enum class AutomationStatus {
    Done
}

data class Owner(val self: String, val accountId: String)
data class TestScript(val self: String)