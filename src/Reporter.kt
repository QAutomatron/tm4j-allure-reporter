import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import data.XmlCheckerOutput
import java.io.File

class Reporter(
    private val output: XmlCheckerOutput
) {
    fun saveOutputAsMD(fileName: String) {
        val detailsOpen = "<details>"
        val detailsClose = "</details>"
        File(fileName).printWriter().use { out ->
            out.println("### Zephyr integration check")
            if (output.missingIds.isNotEmpty()) {
                out.println(detailsOpen)
                out.println("<summary>Missing IDs:</summary>")
                out.println("")
                output.missingIds.forEach { out.println("- $it") }
                out.println(detailsClose)
            }
            if (output.duplicates.isNotBlank()) {
                out.println(detailsOpen)
                out.println("<summary>Duplicated IDs:</summary>")
                out.println("")
                out.println(output.duplicates)
                out.println(detailsClose)
            }
            if (output.tsm.shouldBeInCode.isNotEmpty()) {
                out.println(detailsOpen)
                out.println("<summary>Missing Cases in CODE:</summary>")
                out.println("")
                output.tsm.shouldBeInCode.forEach { out.println("- $it") }
                out.println(detailsClose)
            }
            if (output.tsm.shouldBeInZephyr.isNotEmpty()) {
                out.println(detailsOpen)
                out.println("<summary>Missing Cases in ZEPHYR:</summary>")
                out.println("")
                output.tsm.shouldBeInZephyr.forEach { out.println("- $it") }
                out.println(detailsClose)
            }
            if (output.tsm.missingStatus.isNotEmpty()) {
                out.println(detailsOpen)
                out.println("<summary>Missing Labels in ZEPHYR:</summary>")
                out.println("")
                output.tsm.missingStatus.forEach { out.println("- $it") }
                out.println(detailsClose)
            }
            if (output.tsm.deprecatedCase.isNotEmpty()) {
                out.println(detailsOpen)
                out.println("<summary>Deprecated Cases in ZEPHYR:</summary>")
                out.println("")
                output.tsm.deprecatedCase.forEach { out.println("- $it") }
                out.println(detailsClose)
            }
            log.info { "Output saved to file $fileName" }
        }
    }

    fun saveOutputAsJson(fileName: String) {
        val mapper = jacksonObjectMapper()
        val prettyPrinter = DefaultPrettyPrinter()
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
        mapper.setDefaultPrettyPrinter(prettyPrinter)
        File(fileName).writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output))
        log.info { "Output saved to file $fileName" }
    }
}