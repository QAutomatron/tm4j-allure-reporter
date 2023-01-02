package data

data class XmlCheckerOutput(
    val missingIds: ArrayList<String> = arrayListOf(),
) {
    var duplicates: String = ""
    var tsm: TsmOutput = TsmOutput()
}

data class TsmOutput(
    val shouldBeInZephyr: ArrayList<String> = arrayListOf(),
    val shouldBeInCode: ArrayList<String> = arrayListOf(),
    val deprecatedCase: ArrayList<String> = arrayListOf(),
    val missingStatus: ArrayList<String> = arrayListOf()
)