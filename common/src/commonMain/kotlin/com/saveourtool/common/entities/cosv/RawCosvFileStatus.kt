package com.saveourtool.common.entities.cosv

/**
 * Status of [RawCosvFileDto]
 */
enum class RawCosvFileStatus {
    /**
     * The platform failed to process a raw cosv file
     */
    FAILED,

    /**
     * A raw cosv file is in progress of processing
     */
    IN_PROGRESS,

    /**
     * A raw cosv file processed by the platform
     */
    PROCESSED,

    /**
     * A raw cosv file only uploaded to the platform
     */
    UPLOADED,
    ;
}
