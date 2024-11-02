@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "FILE_WILDCARD_IMPORTS",
    "WildcardImport",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.frontend.common.components.views.organization

import com.saveourtool.common.test.TestsSourceVersionInfo
import com.saveourtool.common.test.TestsSourceVersionInfoList
import com.saveourtool.common.testsuite.*
import com.saveourtool.common.utils.prettyPrint
import com.saveourtool.frontend.common.externals.fontawesome.*
import com.saveourtool.frontend.common.utils.buttonBuilder

import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.ul
import web.cssom.ClassName
import web.cssom.Cursor

/**
 * Display single TestSuiteSource as list option
 *
 * @param isSelected flag that defines if this test suite source is selected or not
 * @param testSuitesSourceDto
 * @param selectHandler callback invoked on TestSuitesSource selection
 * @param editHandler callback invoked on edit TestSuitesSource button pressed
 * @param fetchHandler callback invoked on fetch button pressed
 * @param refreshHandler callback invoked on refresh button pressed
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TOO_MANY_PARAMETERS",
    "LongParameterList",
    "LongMethod",
)
fun ChildrenBuilder.showTestSuitesSourceAsListElement(
    testSuitesSourceDto: TestSuitesSourceDto,
    isSelected: Boolean,
    selectHandler: (TestSuitesSourceDto) -> Unit,
    editHandler: (TestSuitesSourceDto) -> Unit,
    fetchHandler: (TestSuitesSourceDto) -> Unit,
    refreshHandler: () -> Unit,
) {
    val active = if (isSelected) "list-group-item-secondary" else ""
    li {
        className = ClassName("list-group-item $active")
        div {
            className = ClassName("d-flex w-100 justify-content-between")
            button {
                className = ClassName("btn btn-lg btn-link p-0 mb-1")
                onClick = {
                    selectHandler(testSuitesSourceDto)
                }
                label {
                    style = jso {
                        cursor = "pointer".unsafeCast<Cursor>()
                    }
                    fontAwesomeIcon(
                        if (isSelected) {
                            faArrowLeft
                        } else {
                            faArrowRight
                        }
                    )
                    +("  ${testSuitesSourceDto.name}")
                }
            }

            buttonBuilder(faEdit, null, title = "Edit source") {
                editHandler(testSuitesSourceDto)
            }
        }
        div {
            p {
                +(testSuitesSourceDto.description ?: "Description is not provided.")
            }
        }
        div {
            className = ClassName("clearfix")
            div {
                className = ClassName("float-left")
                buttonBuilder("Fetch new version", "info", isOutline = true, classes = "btn-sm mr-2") {
                    fetchHandler(testSuitesSourceDto)
                }
            }
            if (isSelected) {
                div {
                    className = ClassName("float-left")
                    buttonBuilder(faSyncAlt, "info", isOutline = false, classes = "btn-sm mr-2") {
                        refreshHandler()
                    }
                }
            }
            span {
                className = ClassName("float-right align-bottom")
                asDynamic()["data-toggle"] = "tooltip"
                asDynamic()["data-placement"] = "bottom"
                title = "Organization-creator"
                +(testSuitesSourceDto.organizationName)
            }
        }
    }
}

/**
 * Display list of TestSuiteSources as a list
 *
 * @param testSuitesSources [TestSuitesSourceDtoList]
 * @param selectHandler callback invoked on TestSuitesSource selection
 * @param editHandler callback invoked on edit TestSuitesSource button pressed
 * @param fetchHandler callback invoked on fetch button pressed
 * @param refreshHandler
 */
fun ChildrenBuilder.showTestSuitesSources(
    testSuitesSources: TestSuitesSourceDtoList,
    selectHandler: (TestSuitesSourceDto) -> Unit,
    fetchHandler: (TestSuitesSourceDto) -> Unit,
    editHandler: (TestSuitesSourceDto) -> Unit,
    refreshHandler: () -> Unit,
) {
    div {
        className = ClassName("list-group col-8")
        testSuitesSources.forEach {
            showTestSuitesSourceAsListElement(it, false, selectHandler, editHandler, fetchHandler, refreshHandler)
        }
    }
}

/**
 * Display list of [TestsSourceVersionInfo] of [selectedTestSuiteSource]
 *
 * @param selectedTestSuiteSource
 * @param testsSourceVersionInfoList
 * @param selectHandler callback invoked on TestSuitesSource selection
 * @param editHandler callback invoked on edit TestSuitesSource button pressed
 * @param fetchHandler callback invoked on fetch button pressed
 * @param deleteHandler callback invoked on [TestsSourceVersionInfo] deletion
 * @param refreshHandler
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
fun ChildrenBuilder.showTestsSourceVersionInfoList(
    selectedTestSuiteSource: TestSuitesSourceDto,
    testsSourceVersionInfoList: TestsSourceVersionInfoList,
    selectHandler: (TestSuitesSourceDto) -> Unit,
    editHandler: (TestSuitesSourceDto) -> Unit,
    fetchHandler: (TestSuitesSourceDto) -> Unit,
    deleteHandler: (TestsSourceVersionInfo) -> Unit,
    refreshHandler: () -> Unit,
) {
    ul {
        className = ClassName("list-group col-8")
        showTestSuitesSourceAsListElement(selectedTestSuiteSource, true, selectHandler, editHandler, fetchHandler, refreshHandler)
        if (testsSourceVersionInfoList.isEmpty()) {
            li {
                className = ClassName("list-group-item list-group-item-light")
                +"This source is not fetched yet..."
            }
        } else {
            li {
                className = ClassName("list-group-item list-group-item-light")
                div {
                    className = ClassName("clearfix")
                    div {
                        className = ClassName("float-left")
                        +"Version"
                    }
                    div {
                        className = ClassName("float-right")
                        +"Git commit time"
                    }
                }
            }
            testsSourceVersionInfoList.forEach { testsSourceVersionInfo ->
                li {
                    className = ClassName("list-group-item")
                    div {
                        className = ClassName("clearfix")
                        div {
                            className = ClassName("float-left")
                            +testsSourceVersionInfo.version
                        }
                        buttonBuilder(faTimesCircle, style = null, classes = "float-right btn-sm pt-0 pb-0") {
                            deleteHandler(testsSourceVersionInfo)
                        }
                        div {
                            className = ClassName("float-right")
                            +testsSourceVersionInfo.creationTime.prettyPrint()
                        }
                    }
                }
            }
        }
    }
}
