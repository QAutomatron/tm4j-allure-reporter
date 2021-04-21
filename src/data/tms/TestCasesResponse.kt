package data.tms

import com.fasterxml.jackson.annotation.JsonProperty

data class TestCasesResponse(
    val maxResults: Int,
    val total: Int,
    val values: ArrayList<TestCaseResponse>
)
data class IdSelf(val id: Int, val self: String)
data class TestCaseResponse(val id: Int,
                            val key: String,
                            val name: String,
                            val project: IdSelf,
                            val priority: IdSelf,
                            val status: IdSelf,
                            val labels: ArrayList<String>,
//                            val customFields: CustomFields?
                            )