/**
 * View with some info about core team
 */

package com.saveourtool.frontend.common.components.mobile

import com.saveourtool.frontend.common.components.basic.markdown
import com.saveourtool.frontend.common.components.views.AboutUsView
import com.saveourtool.frontend.common.components.views.Developer
import com.saveourtool.frontend.common.externals.fontawesome.faGithub
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.utils.particles
import js.core.jso

import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.img
import web.cssom.ClassName
import web.cssom.rem

/**
 * A component representing "About us" page
 * If you need mobile version in future - use:
 * val isMobile = window.matchMedia("only screen and (max-width:950px)").matches
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class AboutUsMobileView : AboutUsView() {
    override fun ChildrenBuilder.render() {
        particles()
        renderViewHeader()
        renderSaveourtoolInfo()
        renderDevelopers(NUMBER_OF_COLUMNS)
    }

    override fun ChildrenBuilder.renderSaveourtoolInfo() {
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

    override fun ChildrenBuilder.renderDeveloperCard(developer: Developer) {
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
                    style = jso {
                        fontSize = 2.rem
                    }
                    h6 {
                        className = ClassName("d-flex justify-content-center text-center")
                        +developer.name
                    }
                    a {
                        className = ClassName("d-flex justify-content-center")
                        href = "$GITHUB_LINK${developer.githubNickname}"
                        fontAwesomeIcon(faGithub)
                    }
                }
            }
        }
    }

    companion object {
        private const val NUMBER_OF_COLUMNS = 2
    }
}
