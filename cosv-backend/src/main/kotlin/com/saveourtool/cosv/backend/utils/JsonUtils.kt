/**
 * File contains util methods for Json
 */

package com.saveourtool.cosv.backend.utils

import kotlinx.serialization.json.*

/**
 * @return [JsonArray] which constructing [JsonArray] if [this] is not [JsonArray]
 */
fun JsonElement.toJsonArrayOrSingle(): JsonArray = if (this is JsonArray) {
    this
} else {
    JsonArray(listOf(this))
}
