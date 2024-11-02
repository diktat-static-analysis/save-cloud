package com.saveourtool.frontend.common.utils

import com.saveourtool.frontend.common.externals.render
import com.saveourtool.frontend.common.externals.rest
import com.saveourtool.frontend.common.externals.setupWorker

import org.w3c.fetch.Headers
import react.FC
import react.Props
import react.create
import react.useEffect
import react.useState

import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.browser.window

class UseRequestTest {
    private var requestCount = 0
    private fun createWorker() = setupWorker(
        rest.get("${window.location.origin}/test") { _, res, _ ->
            res { response ->
                response.status = 200
                requestCount += 1
                response
            }
        }
    )

    @Test
    fun test(): Promise<Unit> {
        val worker = createWorker()
        val testComponent: FC<Props> = FC {
            val (sendSecond, setSendSecond) = useState(false)
            val (sendThird, setSendThird) = useState(false)
            useRequest(dependencies = arrayOf(sendSecond)) {
                get("${window.location.origin}/test", Headers(), ::noopLoadingHandler)
                if (!sendSecond) {
                    assertEquals(1, requestCount)
                    setSendSecond(true)
                } else if (!sendThird) {
                    assertEquals(2, requestCount)
                    setSendThird(true)
                }
            }
            val doSendThird = useDeferredRequest {
                get("${window.location.origin}/test", Headers(), ::noopLoadingHandler)
            }
            useEffect(sendThird) {
                if (sendThird) {
                    doSendThird()
                }
            }
        }

        return (worker.start() as Promise<*>).then {
            render(
                wrapper.create {
                    testComponent()
                }
            )
        }.then {
            wait(200)
        }.then {
            assertEquals(3, requestCount)
        }
    }
}
