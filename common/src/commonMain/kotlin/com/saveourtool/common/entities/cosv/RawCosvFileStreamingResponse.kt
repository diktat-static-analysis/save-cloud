package com.saveourtool.common.entities.cosv

import kotlinx.serialization.Serializable

/**
 * @property progress
 * @property progressMessage
 * @property result
 */
@Serializable
data class RawCosvFileStreamingResponse(
    val progress: Int,
    val progressMessage: String,
    val result: List<RawCosvFileDto>? = null,
) {
    companion object {
        /**
         * Final progress
         */
        const val FINAL_PROGRESS = 100
    }
}
