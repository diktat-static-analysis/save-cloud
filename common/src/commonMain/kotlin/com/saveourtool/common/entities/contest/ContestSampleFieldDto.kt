package com.saveourtool.common.entities.contest

import kotlinx.serialization.Serializable

/**
 * @property name name of field
 * @property type type of field
 */
@Serializable
data class ContestSampleFieldDto(
    val name: String,
    val type: ContestSampleFieldType,
) {
    companion object {
        val empty = ContestSampleFieldDto(
            "",
            ContestSampleFieldType.NUMBER,
        )
    }
}
