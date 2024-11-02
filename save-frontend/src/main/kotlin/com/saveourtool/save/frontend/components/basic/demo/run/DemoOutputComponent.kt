/**
 * Function component for demo output displaying
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.demo.run

import com.saveourtool.common.demo.DemoResult
import com.saveourtool.frontend.common.externals.fontawesome.*
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.textarea
import web.cssom.ClassName
import web.cssom.Cursor

private const val ROWS_TEXTAREA = 10

/**
 * [FC] to display output of demo run
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "KDOC_WITHOUT_RETURN_TAG",
    "LongMethod",
    "TYPE_ALIAS"
)
val demoOutputComponent: FC<DemoOutputComponentProps> = FC { props ->
    val (selectedTab, setSelectedTab) = useState<DemoOutputTab?>(null)

    useEffect(props.demoResult) { props.demoResult?.let { setSelectedTab(DemoOutputTab.OUTPUT) } }

    div {
        className = ClassName("")
        div {
            className = ClassName("row align-items-center justify-content-center")
            nav {
                className = ClassName("nav nav-tabs mb-4")
                DemoOutputTab.values().forEach { tab ->
                    li {
                        className = ClassName("nav-item")
                        style = selectedTab?.let {
                            jso { cursor = "pointer".unsafeCast<Cursor>() }
                        }
                        val classVal = when {
                            selectedTab == tab -> " active font-weight-bold"
                            props.demoResult == null -> " disabled "
                            else -> ""
                        }
                        p {
                            className = ClassName("nav-link $classVal text-gray-800")
                            onClick = {
                                setSelectedTab { currentlySelectedTab -> tab.takeIf { it != currentlySelectedTab } }
                            }
                            +tab.name
                        }
                    }
                }
            }
        }

        val displayRows = when (selectedTab) {
            null -> null
            DemoOutputTab.OUTPUT -> props.demoResult?.warnings.orEmpty()
            DemoOutputTab.STDOUT -> props.demoResult?.stdout.orEmpty()
            DemoOutputTab.STDERR -> props.demoResult?.stderr.orEmpty()
        }

        displayRows?.let { lines ->
            textarea {
                className = ClassName("form-control")
                rows = ROWS_TEXTAREA
                value = lines.joinToString("\n")
            }
        }
    }
}

/**
 * [demoOutputComponent] [Props]
 */
external interface DemoOutputComponentProps : Props {
    /**
     * [DemoResult] fetched from save-demo
     */
    var demoResult: DemoResult?
}

@Suppress("WRONG_DECLARATIONS_ORDER")
private enum class DemoOutputTab {
    OUTPUT,
    STDOUT,
    STDERR,
    ;
}
