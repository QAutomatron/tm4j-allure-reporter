package data.tms

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Custom fields in TSM
 */
data class CustomFields(
    @JsonProperty("iOS Auto") var iosAutomationStatus: String?,
    @JsonProperty("Android Auto") var androidAutomationStatus: String?,
    @JsonProperty("Type") val type: String?,
    @JsonProperty("Comment") val comment: String?,
)