package data.tms

import mu.KotlinLogging

private val log = KotlinLogging.logger { }

data class TestCasesResponse(
    val maxResults: Int,
    val total: Int,
    val isLast: Boolean,
    val values: ArrayList<TestCaseResponse>
)

data class IdSelf(val id: Long, val self: String)
data class TestCaseResponse(
    val id: Long,
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
            AndroidPlatformName -> customFields?.androidAutomationStatus == status.status
            iOSPlatformName -> customFields?.iosAutomationStatus == status.status
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

    fun toKeyNameString(): String {
        return "$key $name"
    }
}

/**
 * Automation status for custom field
 */
const val AndroidPlatformName = "android"
const val iOSPlatformName = "ios"

enum class AutomationStatus(val status: String?) {
    None(null), Done("Done"), Wont("Won't"), Deprecated("Deprecated"), Duplicate("Duplicate")
}

data class Owner(val self: String, val accountId: String)
data class TestScript(val self: String)