/**
 * https://github.com/mswjs/msw
 */

@file:JsModule("msw")
@file:JsNonModule
@file:Suppress(
    "MISSING_KDOC_ON_FUNCTION",
    "KDOC_WITHOUT_PARAM_TAG",
    "KDOC_WITHOUT_RETURN_TAG",
    "KDOC_NO_EMPTY_TAGS",
)

package com.saveourtool.frontend.common.externals

external val rest: dynamic

/**
 * The setupWorker() function is used for client-side mocking. You usually create a worker instance and activate it by calling the start() method on the returned API.
 * Source: https://mswjs.io/docs/api/setup-worker
 */
external fun setupWorker(
    vararg requestHandlers: dynamic
): dynamic
