/**
 * A view related to the sign-in view
 */

@file:Suppress(
    "FILE_WILDCARD_IMPORTS",
    "WildcardImport",
    "MAGIC_NUMBER",
    "FILE_NAME_MATCH_CLASS"
)

package com.saveourtool.frontend.common.components.views.welcome

import com.saveourtool.common.info.OauthProviderInfo
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.components.views.welcome.pagers.allSaveWelcomePagers
import com.saveourtool.frontend.common.components.views.welcome.pagers.save.renderGeneralInfoPage
import com.saveourtool.frontend.common.components.views.welcome.pagers.save.renderReadMorePage
import com.saveourtool.frontend.common.externals.animations.*
import com.saveourtool.frontend.common.externals.fontawesome.*
import com.saveourtool.frontend.common.externals.i18next.useTranslation
import com.saveourtool.frontend.common.themes.Colors
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.span
import web.cssom.*

import kotlinx.browser.window

val saveWelcomeView: FC<UserInfoAwarePropsWithChildren> = FC { props ->
    val (t) = useTranslation("welcome")
    useBackground(Style.SAVE_DARK)
    val (oauthProviders, setOauthProviders) = useState<List<OauthProviderInfo>>(emptyList())

    useRequest {
        val oauthProviderInfoList: List<OauthProviderInfo>? = get(
            "${window.location.origin}/sec/oauth-providers",
            Headers(),
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        ).run { if (ok) decodeFromJsonString() else null }
        oauthProviderInfoList?.let { setOauthProviders(it) }
    }

    main {
        className = ClassName("main-content mt-0 ps")
        div {
            className = ClassName("page-header align-items-start")
            style = jso {
                height = "100vh".unsafeCast<Height>()
                background = SAVE_DARK_GRADIENT.unsafeCast<Background>()
                position = Position.relative
            }
            span {
                className = ClassName("mask bg-gradient-dark opacity-6")
            }

            div {
                particles()

                className = ClassName("row justify-content-center")
                // Marketing information
                saveWelcomeMarketingTitle("text-white")

                // Sign-in header
                div {
                    className = ClassName("col-3 mt-5 mb-3")
                    div {
                        className = ClassName("card z-index-0 fadeIn3 fadeInBottom")
                        // if user is not logged in - he needs to input credentials
                        props.userInfo?.let {
                            welcomeUserMenu(props.userInfo, Colors.SAVE_PRIMARY, t) {
                                div {
                                    className = ClassName("text-sm")
                                    menuTextAndLink("Contests".t(), FrontendRoutes.CONTESTS, faCode)
                                    hrNoMargin()
                                    menuTextAndLink("List of Projects".t(), FrontendRoutes.PROJECTS, faExternalLinkAlt)
                                    hrNoMargin()
                                    menuTextAndLink("Benchmarks Archive".t(), FrontendRoutes.AWESOME_BENCHMARKS, faFolderOpen)
                                    hrNoMargin()
                                    menuTextAndLink("Create new organization".t(), FrontendRoutes.CREATE_ORGANIZATION, faUser)
                                    if (props.userInfo.isSuperAdmin()) {
                                        hrNoMargin()
                                        menuTextAndLink("Manage organizations".t(), FrontendRoutes.MANAGE_ORGANIZATIONS, faUser)
                                    }
                                    hrNoMargin()
                                    menuTextAndLink("New project in organization".t(), FrontendRoutes.CREATE_PROJECT, faPlus)
                                }
                            }
                        } ?: inputCredentialsView(oauthProviders, Colors.SAVE_PRIMARY, "/${FrontendRoutes.PROJECTS}", t)
                    }
                }
            }

            chevron("#FFFFFF")
        }

        div {
            className = ClassName("min-vh-100")
            style = jso {
                background = SAVE_LIGHT_GRADIENT.unsafeCast<Background>()
            }

            renderGeneralInfoPage()

            @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
            scrollContainer {
                scrollPage { }
                allSaveWelcomePagers.forEach { pager ->
                    scrollPage { }
                    scrollPage {
                        pager.forEach {
                            animator {
                                animation = it.animation
                                it.renderPage(this)
                            }
                        }
                    }
                }
            }

            renderReadMorePage()
        }
    }
}
