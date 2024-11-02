@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.frontend.common.components.basic.contests

import com.saveourtool.common.test.TestFilesContent
import com.saveourtool.common.testsuite.TestSuiteVersioned
import com.saveourtool.frontend.common.components.basic.*
import com.saveourtool.frontend.common.externals.markdown.reactMarkdown
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import web.cssom.ClassName

/**
 * Component that allows to see public tests
 */
val publicTestComponent = publicTestComponent()

private val backgroundCard = cardComponent(hasBg = true, isPaddingBottomNull = true)

private val publicTestCard = cardComponent(hasBg = true, isBordered = true, isPaddingBottomNull = true)

/**
 *  Contest creation component props
 */
external interface PublicTestComponentProps : Props {
    /**
     * Name of current contest
     */
    var contestName: String

    /**
     * List of test suites attached to current contest
     */
    var contestTestSuites: List<TestSuiteVersioned>
}

private fun ChildrenBuilder.displayTestLines(header: String, lines: List<String>, language: String? = null) = div {
    div {
        className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
        +header
    }
    val reactMarkdownOptions: dynamic = jso {
        this.children = wrapTestLines(lines, language)
        this.rehypePlugins = arrayOf(::rehypeHighlightPlugin)
    }
    publicTestCard {
        +reactMarkdown(reactMarkdownOptions)
    }
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "AVOID_NULL_CHECKS"
)
private fun publicTestComponent() = FC<PublicTestComponentProps> { props ->
    val (selectedTestSuite, setSelectedTestSuite) = useState<TestSuiteVersioned?>(null)
    val (publicTest, setPublicTest) = useState<TestFilesContent?>(null)

    useRequest(dependencies = arrayOf(selectedTestSuite)) {
        selectedTestSuite?.let { selectedTestSuite ->
            val response = get(
                "$apiUrl/contests/${props.contestName}/public-test?testSuiteId=${selectedTestSuite.id}",
                jsonHeaders,
                loadingHandler = ::loadingHandler,
                responseHandler = ::noopResponseHandler,
            )
            if (response.ok) {
                val testFilesContent: TestFilesContent = response.decodeFromJsonString()
                setPublicTest(testFilesContent)
            } else {
                setPublicTest(TestFilesContent.empty)
            }
        }
    }

    if (props.contestTestSuites.isEmpty()) {
        h6 {
            className = ClassName("text-center")
            +"No public tests are provided yet."
        }
    } else {
        div {
            className = ClassName("d-flex justify-content-center")
            // ========== Test Suite Selector ==========
            div {
                className = ClassName("col-6")
                showAvailableTestSuites(
                    props.contestTestSuites,
                    selectedTestSuite?.let { listOf(it) } ?: emptyList(),
                    null,
                ) { testSuite ->
                    if (testSuite == selectedTestSuite) {
                        setSelectedTestSuite(null)
                        setPublicTest(null)
                    } else {
                        setSelectedTestSuite(testSuite)
                    }
                }
            }

            // ========== Public test card ==========
            div {
                className = ClassName("col-6")
                publicTest?.let { publicTest ->
                    div {
                        if (publicTest.testLines.isEmpty()) {
                            div {
                                className = ClassName("text-center")
                                +"Public tests are not provided for this test suite"
                            }
                        } else {
                            backgroundCard {
                                div {
                                    className = ClassName("ml-2 mr-2")
                                    div {
                                        className = ClassName("mt-3 mb-3")
                                        displayTestLines("Test", publicTest.testLines, publicTest.language)
                                    }
                                    publicTest.expectedLines?.let {
                                        div {
                                            className = ClassName("mt-3 mb-2")
                                            displayTestLines("Expected", it, publicTest.language)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun wrapTestLines(testLines: List<String>, language: String?) = """
    |```${ language ?: "" }
    |${testLines.joinToString("\n")}
    |```""".trimMargin()
