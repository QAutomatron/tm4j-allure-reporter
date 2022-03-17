package data

import com.apurebase.arkenv.util.argument

object MainArguments {
    val token: String by argument()
    val projectKey: String by argument()
    val reportFrom: String by argument()
    val mode: String by argument()
}

object AllureArguments {
    val cycleName: String by argument()
    val cycleDescription: String by argument()
}

object XmlArguments {
    val platform: String by argument()
    val updateCases: Boolean by argument()
    val suiteNameContains: String? by argument()

    override fun toString() = "platform: [$platform]; updateCases: [$updateCases], suiteNameContains: [$suiteNameContains]"
}