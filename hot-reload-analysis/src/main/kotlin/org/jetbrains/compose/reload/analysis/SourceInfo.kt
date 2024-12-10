package org.jetbrains.compose.reload.analysis

import org.jetbrains.compose.reload.core.createLogger

data class SourceInfo(
    val functionName: String,
    val isLambda: Boolean,
    val isInline: Boolean,
    val parameters: Parameters?,
    val locations: List<Location>,
    val fileName: String,
    val hash: String?
) {
    data class Location(val line: Int, val start: Int, val offset: Int = -1)
    data class Parameters(val data: String, val offset: Int)
}

internal fun SourceInfo.Parameters.asSequence(): Sequence<Int> {
    return this.data.asSequence()
}

/**
 * Parses parameters sequence:
 * - `1,2,3` > `[1, 2, 3]`
 * - `` > `[]`
 * - `!3` > `[0, 1, 2]`
 * - `1,!3` > `[1, 0, 1, 2]`
 */
internal fun String.asSequence(): Sequence<Int> {
    return if (this.isEmpty()) emptySequence()
    else this.split(",").asSequence().flatMap {
        if (it.startsWith("!")) {
            val fact = it.drop(1).toIntOrDefault(0)
            List(fact) { n -> n }
        } else listOf(it.toIntOrDefault(-1))
    }
}

private val stringNameRegex = Regex("""^\(([^)]+)\)""")
private val intNameRegex = Regex("^\\d+")
private val paramsRegex = Regex("""^P\(([^)]+)\)(\d+)""")
private val regexLocations = Regex("""@(\d+)[Ll](\d+),?(\d+)?(?=@|:)""")

/**
 * Source info consists of the next parts:
 * 1. Optional `C` which indicates that function is inlined
 * 2. Function name, always starts from `C` and follow one of the formats:
 *    * `C(NAME)`, where `NAME` is any string, for example `C(remember)`
 *    * `CNAME`, where `NAME` is number, for example `C55`. Also such name indicates that the function is lambda
 *    * Or no name.
 * 3. Optional parameters info, which have format `P(INFO)OFFSET`, where `INFO` is list of integers and factorials and `OFFSET` is number.
 *    For example `P(1,!2,3)222`. See [asSequence]
 * 4. List of locations where every location has format `@LINELSTART,OFFSET` where:
 *    * every location starts with `@`
 *    * followed by `LINE` which represent number
 *    * `L` char
 *    * `START` - any number
 *    * `,` char
 *    * Optional `OFFSET`
 * 5. Then `#` char
 * 6. Optional string, which represents hash
 *
 * See [org.jetbrains.compose.reload.analysis.tests.SourceInfoParsingTest] for detailed examples.
 */
internal fun parseSourceInfo(rawInfo: String): SourceInfo {

    var info = rawInfo
    var isInline = false
    var isLambda = false
    var functionName = ""
    var parameters: SourceInfo.Parameters? = null
    val locations = mutableListOf<SourceInfo.Location>()
    var fileName: String?
    var hash: String? = null

    info = info.dropFirst("CC") { isInline = it != null }

    if (!isInline) info = info.dropFirst("C")

    if (!info.startsWith('(')) isLambda = true

    if (isLambda) {
        info = info.dropFirst(intNameRegex) { lambdaId ->
            functionName = lambdaId ?: ""
        }
    } else {
        info = info.dropFirst(stringNameRegex) {
            val dropLast = it?.drop(1)?.dropLast(1)
            functionName = if (dropLast != null) dropLast else {
                logger.warn("Unable to parse source info function name: $info")
                "invalid-named-function-name"
            }
        }
    }

    info = info.dropFirstGroup(paramsRegex) {
        val params = it.getOrNull(1)
        val paramsN = it.getOrNull(2)?.toIntOrNull()
        if (params != null && paramsN != null) {
            parameters = SourceInfo.Parameters(params, paramsN)
        }
    }

    info = info.dropAllGroups(regexLocations) { groups ->
        groups.chunked(4).forEach { loc ->
            loc[1].toIntOrNull()
            locations.add(
                SourceInfo.Location(
                    line = loc.getOrNull(1).toIntOrDefault(-1),
                    start = loc.getOrNull(2).toIntOrDefault(-1),
                    offset = loc.getOrNull(3).toIntOrDefault(-1),
                )
            )
        }
    }

    info = info.dropFirst(":")

    info.split('#').run {
        fileName = this.getOrNull(0)
        if (this.size == 2) hash = this.getOrNull(1)
    }

    return SourceInfo(
        functionName = functionName,
        isLambda = isLambda,
        isInline = isInline,
        parameters = parameters,
        locations = locations,
        fileName = fileName ?: "invalid-file-name",
        hash = hash
    )

}

private fun String.dropFirst(str: String, andGet: (String?) -> Unit = {}): String {
    return if (this.startsWith(str)) this.replaceFirst(str, "").also { andGet(str) }
    else this.also { andGet(null) }
}

private fun String.dropFirst(regex: Regex, andGet: (String?) -> Unit = {}): String {
    val match = regex.find(this)
    return if (match != null) this.removeRange(match.range).also { andGet(match.value) }
    else this.also { andGet(null) }
}

private fun String.dropFirstGroup(regex: Regex, andGet: (List<String>) -> Unit = {}): String {
    val matchResult = regex.find(this)
    return if (matchResult != null) {
        regex.replace(this, "").also { andGet(matchResult.groupValues) }
    } else {
        this.also { andGet(emptyList()) }
    }
}

private fun String.dropAllGroups(regex: Regex, andGet: (List<String>) -> Unit = {}): String {
    val matchResult = regex.findAll(this).toList()
    return if (matchResult.isNotEmpty()) {
        regex.replace(this, "")
            .also { andGet(matchResult.flatMap { match -> match.groups.map { group -> group?.value ?: "" } }) }
    } else {
        this.also { andGet(emptyList()) }
    }
}

private fun String?.toIntOrDefault(i: Int = 0): Int = this?.toIntOrNull() ?: i

private val logger = createLogger()