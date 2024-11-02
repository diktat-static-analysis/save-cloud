/**
 * A view with project creation details
 */

@file:Suppress("MAGIC_NUMBER", "WildcardImport", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.common.entities.benchmarks.BenchmarkCategoryEnum
import com.saveourtool.common.utils.AwesomeBenchmarks
import com.saveourtool.common.utils.DATABASE_DELIMITER
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.components.RequestStatusContext
import com.saveourtool.frontend.common.components.requestStatusContext
import com.saveourtool.frontend.common.components.views.AbstractView
import com.saveourtool.frontend.common.externals.fontawesome.*
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import org.w3c.fetch.Headers
import react.*
import react.dom.*
import react.dom.aria.ariaDescribedBy
import react.dom.aria.ariaLabel
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.ol
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.strong
import react.dom.html.ReactHTML.ul
import react.router.dom.Link
import remix.run.router.Location
import web.cssom.ClassName
import web.cssom.Cursor
import web.cssom.FontWeight
import web.cssom.rem
import web.html.ButtonType
import web.html.InputType

import kotlinx.coroutines.launch

const val ALL_LANGS = "all"

/**
 * [PropsWithChildren] of [AwesomeBenchmarksView]
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface AwesomeBenchmarksProps : PropsWithChildren {
    var location: Location<*>
}

/**
 * [State] of [AwesomeBenchmarksView]
 *
 */
external interface AwesomeBenchmarksState : State {
    /**
     * list of benchmarks from DB
     */
    var benchmarks: List<AwesomeBenchmarks>

    /**
     * list of unique languages from benchmarks
     */
    var languages: List<String>

    /**
     * Selected language
     */
    var lang: String

    /**
     * Contains the paths of default and other tabs
     */
    var selectedMenu: BenchmarkCategoryEnum
}

/**
 * A functional Component for project creation view
 *
 * @return a functional component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class AwesomeBenchmarksView : AbstractView<AwesomeBenchmarksProps, AwesomeBenchmarksState>() {
    init {
        state.selectedMenu = BenchmarkCategoryEnum.defaultTab
        state.lang = ALL_LANGS
        state.benchmarks = emptyList()
    }

    override fun componentDidMount() {
        super.componentDidMount()

        scope.launch { getBenchmarks() }
    }

    @Suppress("TOO_LONG_FUNCTION", "EMPTY_BLOCK_STRUCTURE_ERROR", "LongMethod")
    override fun ChildrenBuilder.render() {
        particles()

        main {
            className = ClassName("main-content mt-0 ps text-gray-800")
            div {
                className = ClassName("page-header align-items-start min-vh-100")
                div {
                    className = ClassName("row justify-content-center")
                    div {
                        className = ClassName("col-6")
                        div {
                            className = ClassName("row mb-2")
                            div {
                                className = ClassName("col-6")
                                div {
                                    className = ClassName("card flex-md-row mb-1 box-shadow")
                                    style = jso {
                                        height = 14.rem
                                    }
                                    div {
                                        className = ClassName("card-body d-flex flex-column align-items-start")
                                        strong {
                                            className = ClassName("d-inline-block mb-2 text-info")
                                            +"Total Benchmarks:"
                                        }
                                        h1 {
                                            className = ClassName("mb-0")
                                            a {
                                                className = ClassName("text-dark")
                                                href = "#"
                                                +state.benchmarks.count().toString()
                                            }
                                        }
                                        p {
                                            className = ClassName("card-text mb-auto")
                                            +"Checkout updates and new benchmarks."
                                        }
                                        a {
                                            href = "https://github.com/saveourtool/awesome-benchmarks/pulls"
                                            +"Check the GitHub"
                                        }
                                    }
                                    img {
                                        className = ClassName("card-img-right flex-auto d-none d-md-block")
                                        src = "/img/undraw_result_re_uj08.svg"
                                        style = jso {
                                            width = 12.rem
                                        }
                                    }
                                }
                            }
                            div {
                                className = ClassName("col-6")
                                div {
                                    className = ClassName("card flex-md-row mb-1 box-shadow")
                                    style = jso {
                                        height = 14.rem
                                    }

                                    div {
                                        className = ClassName("card-body d-flex flex-column align-items-start")
                                        strong {
                                            className = ClassName("d-inline-block mb-2 text-success")
                                            +"""News"""
                                        }
                                        h3 {
                                            className = ClassName("mb-0")
                                            a {
                                                className = ClassName("text-dark")
                                                href = "#"
                                                +"SAVE"
                                            }
                                        }
                                        p {
                                            className = ClassName("card-text mb-auto")
                                            +"Checkout latest updates in SAVE project."
                                        }
                                        a {
                                            href = "https://github.com/saveourtool/save-cloud"
                                            +"SAVE-cloud "
                                        }
                                        a {
                                            href = "https://github.com/saveourtool/save"
                                            +" SAVE-cli"
                                        }
                                    }
                                    img {
                                        className = ClassName("card-img-right flex-auto d-none d-md-block")
                                        src = "/img/undraw_happy_news_re_tsbd.svg"
                                        style = jso {
                                            width = 12.rem
                                        }
                                    }
                                }
                            }
                        }
                        span {
                            className = ClassName("mask opacity-6")
                            form {
                                className = ClassName("d-none d-inline-block form-inline w-100 navbar-search")
                                div {
                                    className = ClassName("input-group")
                                    input {
                                        className = ClassName("form-control bg-light border-0 small")
                                        type = InputType.text
                                        placeholder = "Search for the benchmark..."
                                        ariaLabel = "Search"
                                        ariaDescribedBy = "basic-addon2"
                                    }
                                    div {
                                        className = ClassName("input-group-append")
                                        button {
                                            className = ClassName("btn btn-outline-primary")
                                            type = ButtonType.button
                                            fontAwesomeIcon(icon = faSearch, classes = "trash-alt")
                                        }
                                    }
                                }
                            }
                        }

                        div {
                            className = ClassName("col container card o-hidden border-0 shadow-lg my-2 card-body p-0")
                            div {
                                className = ClassName("p-5 text-center")
                                h1 {
                                    className = ClassName("h3 text-gray-900 mb-5")
                                    +"Awesome Benchmarks Archive"
                                }

                                div {
                                    className = ClassName("row")
                                    nav {
                                        className = ClassName("nav nav-tabs mb-4")
                                        BenchmarkCategoryEnum.values().forEach { value ->
                                            li {
                                                className = ClassName("nav-item")
                                                val classVal = if (state.selectedMenu == value) {
                                                    " active font-weight-bold"
                                                } else {
                                                    ""
                                                }
                                                p {
                                                    className = ClassName("nav-link $classVal text-gray-800")
                                                    onClick = {
                                                        if (state.selectedMenu != value) {
                                                            setState { selectedMenu = value }
                                                        }
                                                    }
                                                    style = jso {
                                                        cursor = "pointer".unsafeCast<Cursor>()
                                                    }
                                                    +value.name
                                                }
                                            }
                                        }
                                    }
                                }

                                div {
                                    className = ClassName("row mt-3")
                                    div {
                                        className = ClassName("col-8")
                                        var matchingBenchmarksCount = 0
                                        // Nice icons for programming languages: https://devicon.dev
                                        state.benchmarks.forEach { benchmark ->
                                            if ((state.selectedMenu == BenchmarkCategoryEnum.ALL || state.selectedMenu == benchmark.category) &&
                                                    (state.lang == ALL_LANGS || state.lang == benchmark.language)
                                            ) {
                                                ++matchingBenchmarksCount
                                                div {
                                                    className = ClassName("media text-muted pb-3")
                                                    img {
                                                        className = ClassName("rounded")
                                                        src = "/img/undraw_code_inspection_bdl7.svg"
                                                        asDynamic()["data-holder-rendered"] = "true"
                                                        style = jso {
                                                            width = 4.2.rem
                                                        }
                                                    }

                                                    p {
                                                        className = ClassName("media-body pb-3 mb-0 small lh-125 border-bottom border-gray text-left")
                                                        strong {
                                                            className = ClassName("d-block text-gray-dark")
                                                            +benchmark.name
                                                        }
                                                        +benchmark.description
                                                        div {
                                                            className = ClassName("navbar-landing mt-2")
                                                            // FixMe: links should be limited with the length of the div
                                                            benchmark.tags.split(DATABASE_DELIMITER).map { " #$it " }.forEach {
                                                                Link {
                                                                    to = "/${FrontendRoutes.AWESOME_BENCHMARKS}"
                                                                    +it
                                                                }
                                                            }
                                                        }
                                                        div {
                                                            className = ClassName("navbar-landing mt-3")
                                                            a {
                                                                className = ClassName("btn-sm btn-outline-primary mr-2")
                                                                href = benchmark.documentation
                                                                +"""Docs"""
                                                            }
                                                            a {
                                                                className = ClassName("btn-sm btn-info mr-2")
                                                                href = benchmark.sources
                                                                +"""Sources"""
                                                            }
                                                            a {
                                                                className = ClassName("btn-sm btn-success ml-auto")
                                                                href = benchmark.homepage
                                                                +"""More """
                                                                fontAwesomeIcon(icon = faArrowRight)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (matchingBenchmarksCount == 0) {
                                            p {
                                                className = ClassName("media-body font-weight-bold mb-0 small lh-125 text-left")
                                                +"No matching data was found - please select different filters"
                                            }
                                        }
                                    }
                                    div {
                                        className = ClassName("col-4")
                                        ul {
                                            className = ClassName("list-group")
                                            val languages = state.benchmarks.map { it.language }
                                            // FixMe: optimize this code (create maps with numbers only once). May be even store this data in DB?
                                            languages.distinct().forEach { language ->

                                                li {
                                                    className = ClassName("list-group-item d-flex justify-content-between align-items-center")
                                                    onClick = {
                                                        if (state.lang != language) {
                                                            setState {
                                                                lang = language
                                                            }
                                                        } else {
                                                            setState {
                                                                lang = ALL_LANGS
                                                            }
                                                        }
                                                    }

                                                    style = jso {
                                                        cursor = "pointer".unsafeCast<Cursor>()
                                                        if (state.lang == language) {
                                                            fontWeight = "bold".unsafeCast<FontWeight>()
                                                        }
                                                    }

                                                    +language.replace(
                                                        "language independent",
                                                        "lang independent"
                                                    )
                                                    span {
                                                        className = ClassName("badge badge-primary badge-pill")
                                                        +state.benchmarks.count { it.language == language }
                                                            .toString()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    div {
                        className = ClassName("col-4 mb-4")
                        div {
                            className = ClassName("card shadow mb-4")
                            div {
                                className = ClassName("card-header py-3")
                                h6 {
                                    className = ClassName("m-0 font-weight-bold text-primary")
                                    +"Purpose of this list"
                                }
                            }
                            div {
                                className = ClassName("card-body")
                                p {
                                    +"""As a group of enthusiasts who create """

                                    a {
                                        href = "https://github.com/saveourtool/"
                                        +"""dev-tools"""
                                    }

                                    +""" (including static analysis tools),
                                            we have seen a lack of materials related to testing scenarios or benchmarks that can be used to evaluate and test our applications.
                                            
                                            So we decided to create this """

                                    a {
                                        href = "https://github.com/saveourtool/awesome-benchmarks"
                                        +"""curated list of standards, tests and benchmarks"""
                                    }

                                    +""" that can be used for testing and evaluating dev tools.
                                            Our focus is mainly on the code analysis, but is not limited by this category,
                                             in this list we are trying to collect all benchmarks that could be useful 
                                             for creators of dev-tools."""
                                }

                                div {
                                    className = ClassName("text-center")
                                    img {
                                        className = ClassName("img-fluid px-3 px-sm-4 mt-3 mb-4")
                                        style = jso {
                                            width = 20.rem
                                        }
                                        src = "/img/undraw_programming_re_kg9v.svg"
                                        alt = "..."
                                    }
                                }
                            }
                        }
                        div {
                            className = ClassName("card shadow mb-4")
                            div {
                                className = ClassName("card-header py-3")
                                h6 {
                                    className = ClassName("m-0 font-weight-bold text-primary")
                                    +"Easy contribution steps"
                                }
                            }
                            div {
                                className = ClassName("card-body")
                                ol {
                                    li {
                                        fontAwesomeIcon(icon = faGithub)
                                        +""" Go to the"""
                                        Link {
                                            to = "https://github.com/saveourtool/awesome-benchmarks"
                                            +""" ${FrontendRoutes.AWESOME_BENCHMARKS} """
                                        }
                                        +"""repository"""
                                    }
                                    li {
                                        fontAwesomeIcon(icon = faCopy)
                                        +""" Create a fork to your account"""
                                    }
                                    li {
                                        fontAwesomeIcon(icon = faPlus)
                                        +""" Create the description in a proper format"""
                                    }
                                    li {
                                        fontAwesomeIcon(icon = faFolderOpen)
                                        +""" Add your benchmark to"""
                                        a {
                                            href = "https://github.com/saveourtool/awesome-benchmarks/tree/main/benchmarks"
                                            +""" benchmarks """
                                        }
                                        +"""dir"""
                                    }
                                    li {
                                        fontAwesomeIcon(icon = faCheckCircle)
                                        +""" Validate the format with """
                                        a {
                                            href = "https://docs.gradle.org/current/userguide/command_line_interface.html"
                                            +"""`./gradlew build`"""
                                        }
                                    }
                                    li {
                                        fontAwesomeIcon(icon = faArrowRight)
                                        +""" Create the PR to the main repo"""
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getBenchmarks() {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        val response: List<AwesomeBenchmarks> = get(
            "$apiUrl/${FrontendRoutes.AWESOME_BENCHMARKS}",
            headers,
            loadingHandler = ::classLoadingHandler,
        ).decodeFromJsonString()

        setState {
            benchmarks = response
        }
    }

    companion object : RStatics<AwesomeBenchmarksProps, AwesomeBenchmarksState, AwesomeBenchmarksView, Context<RequestStatusContext?>>(AwesomeBenchmarksView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
