package data.tms

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Custom fields in TSM
 */
data class CustomFields(
    @JsonProperty("iOS Auto") val iOSAuto: String?,
    @JsonProperty("Android Auto") val androidAuto: String?,
    @JsonProperty("Type") val type: String?,
    @JsonProperty("Comment") val comment: String?,
)

/**
 * Automation status for custom field
 */
enum class AutomationStatus{
    Done;
}