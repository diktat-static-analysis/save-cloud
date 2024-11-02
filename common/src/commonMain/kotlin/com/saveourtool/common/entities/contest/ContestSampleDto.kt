package com.saveourtool.common.entities.contest

import kotlinx.serialization.Serializable

/**
 * @property id name of contest sample
 * @property name name of contest sample
 * @property description description of contest sample
 * @property fields list of fields
 */
@Serializable
data class ContestSampleDto(
    val id: Long,
    val name: String,
    val description: String?,
    val fields: List<ContestSampleFieldDto> = emptyList(),
) {
    companion object {
        val empty = ContestSampleDto(
            -1,
            "",
            null,
        )
    }
}
