package com.saveourtool.common.entities.contest

import kotlin.js.JsExport
import kotlinx.serialization.Serializable

/**
 * Enum of contest status
 */
@Serializable
@JsExport
enum class ContestStatus {
    /**
     * Created contest
     */
    CREATED,

    /**
     * Deleted contest
     */
    DELETED,
    ;
}
