/**
 * Utilities for testing frontend
 */

package com.saveourtool.frontend.common.utils

import com.saveourtool.frontend.common.components.RequestStatusContext
import com.saveourtool.frontend.common.components.requestStatusContext

import org.w3c.fetch.Response
import react.FC
import react.PropsWithChildren
import react.useState
import web.timers.setTimeout

import kotlin.js.Promise
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.router.MemoryRouter

val wrapper: FC<PropsWithChildren> = FC { props ->
    val (_, setMockState) = useState<Response?>(null)
    val (_, setRedirectToFallbackView) = useState(false)
    val (_, setLoadingCounter) = useState(0)
    MemoryRouter {
        requestStatusContext.Provider {
            value = RequestStatusContext(setMockState, setRedirectToFallbackView, setLoadingCounter)
            +props.children
        }
    }
}

/**
 * Mocks a successful response with serialized value of [value] and returns the same [response]
 *
 * @param response a response object from MSW library
 * @param value value to be added into response body
 * @return response object with configuration applied
 */
inline fun <reified T> mockMswResponse(response: dynamic, value: T): dynamic {
    response.status = 200
    response.headers.set("Content-Type", "application/json")
    response.body = Json.encodeToString(value)
    return response
}

/**
 * @param millis the time to wait, in milliseconds.
 * @return the created `Promise` instance.
 */
fun wait(millis: Int) = Promise { resolve, _ ->
    setTimeout({ resolve(Unit) }, millis)
}
