package data.tms

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
//                            val customFields: CustomFields?
                            )

data class Owner(val self: String, val accountId: String)
data class TestScript(val self: String)