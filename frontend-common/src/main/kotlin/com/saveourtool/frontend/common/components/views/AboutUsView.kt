/**
 * View with some info about core team
 */

package com.saveourtool.frontend.common.components.views

import com.saveourtool.frontend.common.components.RequestStatusContext
import com.saveourtool.frontend.common.components.basic.cardComponent
import com.saveourtool.frontend.common.components.basic.markdown
import com.saveourtool.frontend.common.components.requestStatusContext
import com.saveourtool.frontend.common.externals.fontawesome.faGithub
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.utils.particles

import js.core.jso
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.img
import web.cssom.ClassName
import web.cssom.Color
import web.cssom.rem

/**
 * [Props] of [AboutUsView]
 */
external interface AboutUsViewProps : Props

/**
 * [State] of [AboutUsView]
 */
external interface AboutUsViewState : State

/**
 * A component representing "About us" page
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
open class AboutUsView : AbstractView<AboutUsViewProps, AboutUsViewState>() {
    private val developers = listOf(
        Developer("Vlad", "Frolov", "Cheshiriks", "Fullstack"),
        Developer("Peter", "Trifanov", "petertrr", "Arch"),
        Developer("Andrey", "Shcheglov", "0x6675636b796f75676974687562", "Backend"),
        Developer("Sasha", "Frolov", "sanyavertolet", "Fullstack"),
        Developer("Andrey", "Kuleshov", "akuleshov7", "Ideas 😎"),
        Developer("Nariman", "Abdullin", "nulls", "Fullstack"),
        Developer("Alexey", "Votintsev", "Arrgentum", "Frontend"),
        Developer("Kirill", "Gevorkyan", "kgevorkyan", "Backend"),
        Developer("Dmitriy", "Morozovsky", "icemachined", "Sensei"),
    ).sortedBy { it.name }

    /**
     * padding is removed for this card, because of the responsive images (avatars)
     */
    protected val devCard = cardComponent(hasBg = true, isPaddingBottomNull = true)

    /**
     * card with an info about SAVE with padding
     */
    protected val infoCard = cardComponent(hasBg = true, isPaddingBottomNull = true, isNoPadding = false)

    override fun ChildrenBuilder.render() {
        particles()
        renderViewHeader()
        renderSaveourtoolInfo()
        renderDevelopers(NUMBER_OF_COLUMNS)
    }

    /**
     * Simple title above the information card
     */
    protected fun ChildrenBuilder.renderViewHeader() {
        h2 {
            className = ClassName("text-center mt-3")
            style = jso {
                color = Color("#FFFFFF")
            }
            +"About us"
        }
    }

    /**
     * Info rendering
     */
    protected open fun ChildrenBuilder.renderSaveourtoolInfo() {
        div {
            div {
                className = ClassName("mt-3 d-flex justify-content-center align-items-center")
                div {
                    className = ClassName("col-6 p-0")
                    infoCard {
                        div {
                            className = ClassName("m-2 d-flex justify-content-around align-items-center")
                            div {
                                className = ClassName("m-2 d-flex align-items-center align-self-stretch flex-column")
                                img {
                                    src = "/img/save-logo-no-bg.png"
                                    @Suppress("MAGIC_NUMBER")
                                    style = jso {
                                        width = 8.rem
                                    }
                                    className = ClassName("img-fluid mt-auto mb-auto")
                                }
                                a {
                                    className = ClassName("text-center mt-auto mb-2 align-self-end")
                                    href = "mailto:$SAVEOURTOOL_EMAIL"
                                    +SAVEOURTOOL_EMAIL
                                }
                            }
                            markdown(saveourtoolDescription, "flex-wrap")
                        }
                    }
                }
            }
        }
    }

    /**
     * @param columns
     */
    @Suppress("MAGIC_NUMBER")
    protected fun ChildrenBuilder.renderDevelopers(columns: Int) {
        div {
            h4 {
                className = ClassName("text-center mb-1 mt-4 text-white")
                +"Active contributors"
            }
            div {
                className = ClassName("mt-3 d-flex justify-content-around align-items-center")
                div {
                    className = ClassName("col-6 p-1")
                    val numberOfRows = developers.size / columns
                    for (rowIndex in 0..numberOfRows) {
                        div {
                            className = ClassName("row")
                            for (colIndex in 0 until columns) {
                                div {
                                    className = ClassName("col-${12 / columns} p-2")
                                    developers.getOrNull(columns * rowIndex + colIndex)?.let {
                                        renderDeveloperCard(it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @param developer
     */
    open fun ChildrenBuilder.renderDeveloperCard(developer: Developer) {
        devCard {
            div {
                className = ClassName("p-3")
                div {
                    className = ClassName("d-flex justify-content-center")
                    img {
                        src = "$GITHUB_AVATAR_LINK${developer.githubNickname}?size=$DEFAULT_AVATAR_SIZE"
                        className = ClassName("img-fluid border border-dark rounded-circle m-0")
                        @Suppress("MAGIC_NUMBER")
                        style = jso {
                            width = 10.rem
                        }
                    }
                }
                div {
                    className = ClassName("mt-2")
                    h5 {
                        className = ClassName("d-flex justify-content-center text-center")
                        +developer.name
                    }
                    h5 {
                        className = ClassName("d-flex justify-content-center text-center")
                        +developer.surname
                    }
                    h6 {
                        className = ClassName("text-center")
                        +developer.description
                    }
                    a {
                        style = jso {
                            fontSize = 2.rem
                        }
                        className = ClassName("d-flex justify-content-center")
                        href = "$GITHUB_LINK${developer.githubNickname}"
                        fontAwesomeIcon(faGithub)
                    }
                }
            }
        }
    }

    companion object :
        RStatics<AboutUsViewProps, AboutUsViewState, AboutUsView, Context<RequestStatusContext?>>(AboutUsView::class) {
        protected const val DEFAULT_AVATAR_SIZE = "200"
        protected const val GITHUB_AVATAR_LINK = "https://avatars.githubusercontent.com/"
        protected const val GITHUB_LINK = "https://github.com/"
        protected const val MAX_NICKNAME_LENGTH = 15
        protected const val NUMBER_OF_COLUMNS = 3
        protected const val SAVEOURTOOL_EMAIL = "saveourtool@gmail.com"
        protected val saveourtoolDescription = """
            # Save Our Tool!
    
            Our community is mainly focused on Static Analysis tools and the eco-system related to such kind of tools.
            We love Kotlin and mostly everything we develop is connected with Kotlin JVM, Kotlin JS or Kotlin Native.
    
            ### Main Repositories:
            - [diktat](${GITHUB_LINK}saveourtool/diktat) - Automated code checker&fixer for Kotlin
            - [save-cli](${GITHUB_LINK}saveourtool/save-cli) - Unified test framework for Static Analyzers and Compilers
            - [save-cloud](${GITHUB_LINK}saveourtool/save-cloud) - Cloud eco-system for CI/CD and benchmarking of Static Analyzers
            - [awesome-benchmarks](${GITHUB_LINK}saveourtool/awesome-benchmarks) - Curated list of benchmarks for different types of testing
    
        """.trimIndent()

        init {
            AboutUsView.contextType = requestStatusContext
        }
    }
}

/**
 * @property name developer's name
 * @property githubNickname nickname of developer on GitHub
 * @property description brief developer description
 * @property surname
 */
@JsExport
data class Developer(
    val name: String,
    val surname: String,
    val githubNickname: String,
    val description: String = "",
)
