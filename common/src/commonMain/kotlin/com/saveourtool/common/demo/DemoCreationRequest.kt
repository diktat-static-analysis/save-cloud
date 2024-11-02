package com.saveourtool.common.demo

import com.saveourtool.common.entities.FileDto
import kotlinx.serialization.Serializable

/**
 * @property demoDto
 * @property manuallyUploadedFileDtos
 */
@Serializable
data class DemoCreationRequest(
    val demoDto: DemoDto,
    val manuallyUploadedFileDtos: List<FileDto>,
)
