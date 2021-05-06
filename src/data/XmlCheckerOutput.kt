package data

data class XmlCheckerOutput(
    val missingIds: ArrayList<String> = arrayListOf(),
) {
    var duplicates: String = ""
    lateinit var tsm: TsmOutput
}

data class TsmOutput(
    val missingCase: ArrayList<String> = arrayListOf(),
    val deprecatedCase: ArrayList<String> = arrayListOf(),
    val missingLabel: ArrayList<String> = arrayListOf()
)