/**
 * Component for save-demo statistics render
 */

package com.saveourtool.save.frontend.components.basic.demo.welcome

import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.noopLoadingHandler
import com.saveourtool.frontend.common.utils.noopResponseHandler

import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.strong
import react.useState
import web.cssom.ClassName

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
internal val statistics: FC<Props> = FC {
    val (activeDemoAmount, setActiveDemoAmount) = useState(0)
    useRequest {
        val active: Int = get(
            url = "$demoApiUrl/stats/active",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        ).decodeFromJsonString()
        setActiveDemoAmount(active)
    }

    val (createdDemoAmount, setCreatedDemoAmount) = useState(0)
    useRequest {
        val created: Int = get(
            url = "$apiUrl/stats/created",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        ).decodeFromJsonString()
        setCreatedDemoAmount(created)
    }

    div {
        className = ClassName("card")
        div {
            className = ClassName("row mx-3")
            div {
                className = ClassName("col-6 mt-2 mb-2")
                div {
                    className = ClassName("row justify-content-center")
                    strong {
                        className = ClassName("d-inline-block mb-2 card-text")
                        +"Active demo:"
                    }
                }
                div {
                    className = ClassName("row justify-content-center")
                    h1 {
                        className = ClassName("text-dark")
                        +activeDemoAmount.toString()
                    }
                }
            }
            div {
                className = ClassName("col-6 mt-2")
                div {
                    className = ClassName("row justify-content-center")
                    strong {
                        className = ClassName("d-inline-block mb-2 card-text ")
                        +"Created demo:"
                    }
                }
                div {
                    className = ClassName("row justify-content-center")
                    h1 {
                        className = ClassName("text-dark")
                        +createdDemoAmount.toString()
                    }
                }
            }
        }
    }
}
