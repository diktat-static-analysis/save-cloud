/**
 * rendering for Token management card
 */

package com.saveourtool.frontend.common.components.views.usersettings.right

import com.saveourtool.frontend.common.components.views.usersettings.SettingsProps
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.buttonBuilder

import js.core.jso
import react.FC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.p
import react.useState
import web.cssom.ClassName
import web.cssom.TextAlign
import web.cssom.rem
import web.html.ButtonType

// FixMe: add info about last created token

const val TOKEN_TEXT = """
    This Token serves as an integral part of establishing a secure and stable connection 
    between your automated API and the SAVE platform. 
    The purpose of this Token is to provide capability to connect to SAVE Public API from different CI services: 
    to upload benchmarks, get results, etc. Can be extremely useful for your automated CI and testing process.
"""

val tokenSettingsCard: FC<SettingsProps> = FC { props ->
    val (token, setToken) = useState<String>()

    val postToken = useDeferredRequest {
        post(
            "$apiUrl/users/${props.userInfo?.name}/save/token",
            jsonHeaders,
            token,
            loadingHandler = ::loadingHandler,
        )
            .run {
                if (!ok) {
                    setToken(null)
                }
            }
    }

    div {
        className = ClassName("row justify-content-center mt-5")
        img {
            src = "/img/settings_icon2.png"
            style = jso {
                height = 10.rem
                width = 10.rem
            }
        }
    }

    div {
        className = ClassName("row justify-content-center mt-4")
        h2 {
            className = ClassName("text-gray-800")
            +"Personal access token"
        }
    }

    div {
        className = ClassName("row justify-content-center")
        div {
            className = ClassName("col-2 text-right")
            a {
                href = "https://github.com/saveourtool/save-cloud/tree/master/save-api"
                buttonBuilder(
                    "API Readme",
                    style = "outline-secondary",
                    classes = "rounded-pill btn-sm",
                    isOutline = false
                ) { }
            }
        }

        div {
            className = ClassName("col-2 text-left")
            a {
                href = "https://github.com/saveourtool/save-cloud/tree/master/save-api-cli"
                buttonBuilder(
                    "Usage example",
                    style = "outline-secondary",
                    classes = "rounded-pill btn-sm",
                    isOutline = false
                ) { }
            }
        }
    }

    div {
        className = ClassName("row justify-content-center mt-4")
        div {
            className = ClassName("col-5")
            p {
                style = jso {
                    textAlign = TextAlign.justify
                }
                +TOKEN_TEXT
            }
        }
    }

    div {
        className = ClassName("row justify-content-center mt-4")
        div {
            className = ClassName("col-6 text-center")
            button {
                type = ButtonType.button
                className = ClassName("btn btn-primary")
                +"Generate new token"
                onClick = {
                    setToken(generateToken())
                    postToken()
                }
            }
        }
    }

    token?.let {
        div {
            className = ClassName("row justify-content-center mt-4")
            div {
                className = ClassName("col-6")
                form {
                    className = ClassName("form-group text-center")
                    input {
                        value = token
                        required = true
                        className = ClassName("form-control text-center")
                    }
                }
                div {
                    className = ClassName("invalid-feedback d-block text-center")
                    +"This is your unique token. It will be shown to you only once! Please save it."
                }
            }
        }
    }
}

@Suppress("MAGIC_NUMBER")
private fun generateToken(): String {
    var token = "ghp_"
    val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    while (token.length < 40) {
        token += charPool.random()
    }
    return token
}
