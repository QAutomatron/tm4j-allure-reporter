import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import data.xml.TestSuites
import java.io.File
import java.io.InputStream

object XmlParser {
    private val kotlinXmlMapper = XmlMapper(JacksonXmlModule().apply {
        setDefaultUseWrapper(false)
    }).registerKotlinModule()
//    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
//    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun parseFileAs(path: String): TestSuites = kotlinXmlMapper.readValue(
        File(path), TestSuites::class.java)
    fun parseStreamAs(stream: InputStream): TestSuites = kotlinXmlMapper.readValue(stream, TestSuites::class.java)
}