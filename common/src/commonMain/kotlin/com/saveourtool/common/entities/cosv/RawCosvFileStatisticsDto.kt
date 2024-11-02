package com.saveourtool.common.entities.cosv

import kotlinx.serialization.Serializable

/**
 * DTO for statistics data for raw cosv file
 *
 * @property allAvailableFilesCount
 * @property uploadedArchivesCount
 * @property uploadedJsonFilesCount
 * @property processingFilesCount
 * @property duplicateFilesCount
 * @property errorFilesCount
 * @property pendingRemovedFilesCount
 */
@Serializable
data class RawCosvFileStatisticsDto(
    val allAvailableFilesCount: Int,
    val uploadedArchivesCount: Int,
    val uploadedJsonFilesCount: Int,
    val processingFilesCount: Int,
    val pendingRemovedFilesCount: Int,
    val duplicateFilesCount: Int,
    val errorFilesCount: Int,
) {
    companion object {
        val empty = RawCosvFileStatisticsDto(
            allAvailableFilesCount = 0,
            uploadedArchivesCount = 0,
            uploadedJsonFilesCount = 0,
            processingFilesCount = 0,
            pendingRemovedFilesCount = 0,
            duplicateFilesCount = 0,
            errorFilesCount = 0,
        )
    }
}
