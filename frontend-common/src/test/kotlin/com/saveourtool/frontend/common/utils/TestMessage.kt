package com.saveourtool.frontend.common.utils

import kotlinx.serialization.Serializable

/**
 * A JSON-serializable entity used by [ServerSentEventTest].
 *
 * @see ServerSentEventTest
 */
@Serializable
data class TestMessage(val value: String)
