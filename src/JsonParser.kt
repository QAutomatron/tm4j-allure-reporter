import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import data.allure.AllureResult
import java.io.File

object JsonParser {
    fun getAllureResultsFrom(dir: String): ArrayList<AllureResult> {
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val allureResults = arrayListOf<AllureResult>()

        File(dir).walk().forEach {
            if (it.extension == "json" && it.nameWithoutExtension.endsWith("-result")) {
                val allureResult: AllureResult = mapper.readValue(it)
                log.debug { allureResult }
                allureResults.add(allureResult)
            }
        }
        return allureResults
    }
}