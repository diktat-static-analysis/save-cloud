/**
 * File containing simple html table for additional info displaying
 */

package com.saveourtool.save.frontend.components.basic.graph

import com.saveourtool.common.demo.cpg.CpgNodeAdditionalInfo
import com.saveourtool.frontend.common.externals.fontawesome.faTimes
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon

import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.pre
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr
import web.cssom.ClassName
import web.cssom.FontSize
import web.cssom.Right
import web.cssom.Top
import web.html.ButtonType

private const val NOT_PROVIDED = "NOT_PROVIDED"

@Suppress("TYPE_ALIAS")
private val additionalInfoMapping: Map<String, (String, CpgNodeAdditionalInfo?) -> String?> = mapOf(
    "Code" to { _, info -> info?.code },
    "File" to { applicationName, info -> info?.file?.formatPathToFile(applicationName, "demo") },
    "Comment" to { _, info -> info?.comment },
    "Argument index" to { _, info -> info?.argumentIndex?.toString() },
    "isImplicit" to { _, info -> info?.isImplicit?.toString() },
    "isInferred" to { _, info -> info?.isInferred?.toString() },
    "Location" to { _, info -> info?.location },
)

/**
 * @param nodeName
 * @param applicationName
 * @param additionalInfo
 * @param setSelectedNodeName
 */
internal fun ChildrenBuilder.displayCpgNodeAdditionalInfo(
    nodeName: String?,
    applicationName: String,
    additionalInfo: CpgNodeAdditionalInfo?,
    setSelectedNodeName: (String?) -> Unit,
) {
    button {
        className = ClassName("btn p-0 position-absolute")
        fontAwesomeIcon(faTimes)
        type = "button".unsafeCast<ButtonType>()
        onClick = { setSelectedNodeName(null) }
        style = jso {
            top = "0%".unsafeCast<Top>()
            right = "1%".unsafeCast<Right>()
        }
    }
    table {
        thead {
            tr {
                className = ClassName("bg-dark text-light")
                th {
                    scope = "col"
                    +"Name"
                }
                th {
                    scope = "col"
                    +(nodeName?.takeFirstAndLast() ?: NOT_PROVIDED)
                }
            }
        }
        tbody {
            additionalInfoMapping.map { (label, valueGetter) ->
                label to (valueGetter(applicationName, additionalInfo) ?: NOT_PROVIDED)
            }.forEachIndexed { index, (label, value) ->
                tr {
                    className = if (index % 2 == 1) {
                        ClassName("bg-light")
                    } else {
                        ClassName("bg-white")
                    }
                    td {
                        small {
                            +label
                        }
                    }
                    td {
                        pre {
                            className = ClassName("m-0")
                            style = jso {
                                fontSize = FontSize.small
                            }
                            +value
                        }
                    }
                }
            }
        }
    }
}

private fun String.formatPathToFile(
    applicationName: String,
    missingDelimiterValue: String? = null,
) = missingDelimiterValue?.let {
    substringAfterLast("$applicationName/", missingDelimiterValue)
} ?: substringAfterLast("$applicationName/")

@Suppress("MAGIC_NUMBER")
private fun String.takeFirstAndLast(firstNumber: Int = 10, lastNumber: Int = 10) = takeIf {
    it.isNotBlank() && length > firstNumber + lastNumber + 2
}
    ?.let { it.take(firstNumber) + ".." + it.takeLast(lastNumber) }
    ?: this
