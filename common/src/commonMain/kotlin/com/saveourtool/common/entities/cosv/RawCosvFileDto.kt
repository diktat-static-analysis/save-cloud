package com.saveourtool.common.entities.cosv

import com.saveourtool.common.entities.DtoWithId
import com.saveourtool.common.utils.ZIP_ARCHIVE_EXTENSION
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * DTO for raw cosv file
 *
 * @property fileName
 * @property userName
 * @property organizationName
 * @property status
 * @property statusMessage
 * @property updateDate
 * @property contentLength
 * @property id
 */
@Serializable
data class RawCosvFileDto(
    val fileName: String,
    val userName: String,
    val organizationName: String,
    val status: RawCosvFileStatus = RawCosvFileStatus.UPLOADED,
    val statusMessage: String? = null,
    val updateDate: LocalDateTime? = null,
    val contentLength: Long? = null,
    override val id: Long? = null,
) : DtoWithId() {
    /**
     * @return non-nullable [contentLength]
     */
    fun requiredContentLength(): Long = requireNotNull(contentLength) {
        "contentLength is not provided: $this"
    }

    companion object {
        /**
         * Extracted as extension to avoid Jackson issue with encoding this field
         *
         * @return true if this raw cosv file is uploaded zip archive, checking by [fileName]
         */
        fun RawCosvFileDto.isZipArchive(): Boolean = fileName.endsWith(ZIP_ARCHIVE_EXTENSION, ignoreCase = true)

        /**
         * Extracted as extension to avoid Jackson issue with encoding this field
         *
         * @return true if this raw cosv file is uploaded json file, checking by [fileName]
         */
        fun RawCosvFileDto.isUploadedJsonFile(): Boolean = !isZipArchive() && status == RawCosvFileStatus.UPLOADED

        /**
         * Extracted as extension to avoid Jackson issue with encoding this field
         *
         * @return true if this raw cosv file is still processing, checking by [fileName]
         */
        fun RawCosvFileDto.isProcessing(): Boolean = status == RawCosvFileStatus.IN_PROGRESS

        /**
         * Extracted as extension to avoid Jackson issue with encoding this field
         *
         * @return true if this raw cosv file is pending to be removed, checking by [fileName]
         */
        fun RawCosvFileDto.isPendingRemoved(): Boolean = status == RawCosvFileStatus.PROCESSED

        /**
         * Extracted as extension to avoid Jackson issue with encoding this field
         *
         * @return true if this raw cosv file is duplicate and with such ID already uploaded, checking by [fileName]
         */
        fun RawCosvFileDto.isDuplicate(): Boolean = status == RawCosvFileStatus.FAILED && statusMessage?.contains("Duplicate entry") == true

        /**
         * Extracted as extension to avoid Jackson issue with encoding this field
         *
         * @return true if this raw cosv file has any other errors excluding duplicate error, checking by [fileName]
         */
        fun RawCosvFileDto.isHasErrors(): Boolean = status == RawCosvFileStatus.FAILED && (statusMessage == null || !statusMessage.contains("Duplicate entry"))
    }
}
