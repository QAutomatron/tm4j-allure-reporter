package data.tms

data class TestStatusesResponse(
    val values: ArrayList<TestStatusResponse>
)

data class TestStatusResponse(
    val id: Long,
    val name: String
)