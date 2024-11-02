/**
 * File containing utils for welcome views rendering
 */

package com.saveourtool.frontend.common.components.views.welcome

import com.saveourtool.common.info.OauthProviderInfo
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.validation.FrontendCosvRoutes
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.frontend.common.externals.fontawesome.*
import com.saveourtool.frontend.common.externals.i18next.TranslationFunction
import com.saveourtool.frontend.common.themes.Colors
import com.saveourtool.frontend.common.utils.OauthProvidersFeConfig
import com.saveourtool.frontend.common.utils.processRegistrationId

import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.p
import react.router.dom.Link
import web.cssom.*

const val INPUT_CREDENTIALS_VIEW_CUSTOM_BG = "rgb(240, 240, 240)"

/**
 * @param oauthProviders
 * @param primaryColor color of a shield
 * @param continueLink link for `continue` button
 * @param t [TranslationFunction] received from [com.saveourtool.save.frontend.externals.i18next.useTranslation] hook
 */
@Suppress("TOO_LONG_FUNCTION", "IDENTIFIER_LENGTH")
fun ChildrenBuilder.inputCredentialsView(
    oauthProviders: List<OauthProviderInfo>,
    primaryColor: Colors,
    continueLink: String,
    t: TranslationFunction,
) {
    div {
        className = ClassName("card-header p-0 position-relative mt-n4 mx-3 z-index-2 rounded")
        style = jso {
            background = INPUT_CREDENTIALS_VIEW_CUSTOM_BG.unsafeCast<Background>()
            border = "1px solid".unsafeCast<Border>()
        }
        div {
            className = ClassName("shadow-primary border-radius-lg py-3 pe-1 rounded")
            style = jso {
                backgroundColor = primaryColor.value.unsafeCast<BackgroundColor>()
            }
            h4 {
                className = ClassName("text-white font-weight-bolder text-center mt-2 mb-3")
                +"Sign in with".t()
            }
        }
        div {
            className = ClassName("row")
            oauthProviders.map {
                processRegistrationId(
                    OauthProvidersFeConfig(
                        size = @Suppress("MAGIC_NUMBER") 3.rem,
                        it,
                    )
                )
            }
        }
    }

    div {
        className = ClassName("card-body")
        form {
            className = ClassName("needs-validation")
            div {
                className = ClassName("mt-4 text-sm text-center")
                p {
                    className = ClassName("mb-0")
                    +"Don't have an account?".t()
                }

                div {
                    className = ClassName("text-sm text-center")
                    h4 {
                        style = jso {
                            color = "#3075c0".unsafeCast<Color>()
                        }
                        Link {
                            to = continueLink
                            className = ClassName("text-gradient font-weight-bold ml-2 mr-2")
                            +"Continue".t()
                            fontAwesomeIcon(icon = faSignInAlt)
                        }
                    }
                    +"with limited functionality".t()
                }
            }
        }
    }
}

/**
 * Render nice menu with options built from [renderMenu]
 *
 * @param userInfo current [UserInfo]
 * @param primaryColor color of `Welcome, {username}` shield
 * @param t [TranslationFunction] received from [com.saveourtool.save.frontend.externals.i18next.useTranslation] hook
 * @param renderMenu callback to render menu options
 */
@Suppress("IDENTIFIER_LENGTH")
fun ChildrenBuilder.welcomeUserMenu(
    userInfo: UserInfo?,
    primaryColor: Colors,
    t: TranslationFunction,
    renderMenu: ChildrenBuilder.() -> Unit,
) {
    div {
        className = ClassName("card-header p-0 position-relative mt-n4 mx-3 z-index-2 rounded")
        div {
            className = ClassName("shadow-primary border-radius-lg py-3 pe-1 rounded")
            style = jso {
                backgroundColor = primaryColor.value.unsafeCast<BackgroundColor>()
            }
            h4 {
                className = ClassName("text-white font-weight-bolder text-center mt-2 mb-0")
                div {
                    className = ClassName("row")
                    div {
                        className = ClassName("col text-center px-1 mb-3")
                        Link {
                            className = ClassName("text-gradient font-weight-bold ml-2 mr-2")
                            to = "/${FrontendRoutes.INDEX}"
                            fontAwesomeIcon(icon = faHome) {
                                it.color = "#FFFFFF"
                            }
                        }
                    }
                }
                +"${"Welcome".t()}, ${userInfo?.name}!"
            }
        }
    }

    div {
        className = ClassName("card-body")
        div {
            className = ClassName("text-sm")
            renderMenu()
        }
    }
}

/**
 * Render styled [text] with link by [route]'s [FrontendRoutes.path] and leading [icon]
 *
 * @param text [String] to display
 * @param route that menu options points to
 * @param icon [FontAwesomeIcon] to display
 */
fun ChildrenBuilder.menuTextAndLink(text: String, route: FrontendRoutes, icon: FontAwesomeIconModule) {
    Link {
        className = ClassName("text-gradient font-weight-bold ml-2 mr-2")
        to = "/$route"
        menuText(text, icon)
    }
}

/**
 * Render styled [text] with link by [route]'s [FrontendCosvRoutes.path] and leading [icon]
 *
 * @param text [String] to display
 * @param route that menu options points to
 * @param icon [FontAwesomeIcon] to display
 */
fun ChildrenBuilder.menuTextAndLink(text: String, route: FrontendCosvRoutes, icon: FontAwesomeIconModule) {
    Link {
        className = ClassName("text-gradient font-weight-bold ml-2 mr-2")
        to = "/$route"
        menuText(text, icon)
    }
}

/**
 * Render horizontal line with [Margin] equals to `0.0em`
 */
fun ChildrenBuilder.hrNoMargin() {
    hr {
        style = jso {
            marginTop = "0.0em".unsafeCast<Margin>()
            marginBottom = "0.0em".unsafeCast<Margin>()
        }
    }
}

private fun ChildrenBuilder.menuText(text: String, icon: FontAwesomeIconModule) {
    h4 {
        div {
            className = ClassName("row ml-2 align-items-center")
            style = jso {
                color = "#3075c0".unsafeCast<Color>()
                marginBottom = "0.0em".unsafeCast<Margin>()
            }
            div {
                className = ClassName("col-1 d-flex justify-content-center")
                fontAwesomeIcon(icon = icon)
            }
            div {
                className = ClassName("col-11 d-flex justify-content-start")
                +text
            }
        }
    }
}
