package com.saveourtool.frontend.common.utils

import com.saveourtool.frontend.common.externals.render
import com.saveourtool.frontend.common.externals.rest
import com.saveourtool.frontend.common.externals.setupWorker
import react.FC
import react.Props
import react.create
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.browser.window
import kotlinx.serialization.json.Json

/**
 * Tests the way _Server-Sent Events_ (SSE) are parsed.
 *
 * @see useNdjson
 */
class ServerSentEventTest {
    private fun createWorker(): dynamic =
            setupWorker(
                rest.get("${window.location.origin}/test") { req, res, _ ->
                    res { response ->
                        when (req.headers.get(ACCEPT)) {
                            APPLICATION_NDJSON_VALUE -> {
                                response.status = OK
                                response.headers.set(CONTENT_TYPE, APPLICATION_NDJSON_VALUE)

                                /*
                                 * Empty lines in the output should be tolerated.
                                 */
                                response.body = """
                                    |{ "value": "$ASCII" }
                                    |{ "value": "$CYRILLIC" }
                                    |
                                    |{ "value": "$CHINESE" }
                                """.trimMargin()
                            }

                            else -> response.status = BAD_REQUEST
                        }

                        response
                    }
                }
            )

    /**
     * Tests that a response of `application/x-ndjson` `Content-Type` is parsed
     * correctly.
     *
     * @see useNdjson
     */
    @Test
    @JsName("newlineDelimitedJson")
    fun `newline-delimited JSON`(): Promise<Unit> {
        val messages = mutableListOf<TestMessage>()
        var responseStatus: Short = 0

        val testComponent: FC<Props> = FC {
            useNdjson(
                url = "${window.location.origin}/test",
                onCompletion = { responseStatus = OK },
                onError = { response -> responseStatus = response.status },
            ) { message ->
                messages.add(Json.decodeFromString(message))
            }()
        }

        return (createWorker().start() as Promise<*>)
            .then {
                render(
                    wrapper.create {
                        testComponent()
                    }
                )
            }.then {
                wait(200)
            }.then {
                assertEquals(OK, responseStatus, "Request completed with an error")
                assertContentEquals(
                    expected = listOf(
                        TestMessage(ASCII),
                        TestMessage(CYRILLIC),
                        TestMessage(CHINESE),
                    ),
                    actual = messages,
                )
            }
    }

    private companion object {
        private const val OK: Short = 200
        private const val BAD_REQUEST: Short = 400
        private const val ACCEPT = "Accept"
        private const val CONTENT_TYPE = "Content-Type"
        private const val APPLICATION_NDJSON_VALUE = "application/x-ndjson"
        private const val ASCII = "The quick brown fox jumps over the lazy dog"

        @Suppress("MaxLineLength")
        private const val CYRILLIC =
                "\u0421\u044a\u0435\u0448\u044c\u0020\u0436\u0435\u0020\u0435\u0449\u0451\u0020\u044d\u0442\u0438\u0445\u0020\u043c\u044f\u0433\u043a\u0438\u0445\u0020\u0444\u0440\u0430\u043d\u0446\u0443\u0437\u0441\u043a\u0438\u0445\u0020\u0431\u0443\u043b\u043e\u043a\u0020\u0434\u0430\u0020\u0432\u044b\u043f\u0435\u0439\u0020\u0447\u0430\u044e"
        private const val CHINESE = "\u542c\u8bf4\u5988\u5988\u5b66\u4e60\u753b\u753b\u540e\uff0c\u4f1a\u7231\u4e71\u4e70\u5356\u8d35\u827a\u672f\u3002"
    }
}
