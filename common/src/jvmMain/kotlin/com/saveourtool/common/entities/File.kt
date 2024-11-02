package com.saveourtool.common.entities

import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.spring.entity.BaseEntityWithDtoWithId

import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name
import kotlinx.datetime.*

/**
 * @property project
 * @property name
 * @property uploadedTime
 * @property sizeBytes
 * @property isExecutable
 */
@Entity
class File(
    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project,

    var name: String,
    var uploadedTime: LocalDateTime,
    var sizeBytes: Long,
    var isExecutable: Boolean,
) : BaseEntityWithDtoWithId<FileDto>() {
    override fun toDto(): FileDto = FileDto(
        projectCoordinates = project.toProjectCoordinates(),
        name = name,
        uploadedTime = uploadedTime.toKotlinLocalDateTime(),
        sizeBytes = sizeBytes,
        isExecutable = isExecutable,
        id = id,
    )
}

/**
 * @param projectResolver
 * @return [File] created from [FileDto]
 */
fun FileDto.toEntity(projectResolver: (ProjectCoordinates) -> Project): File = File(
    project = projectResolver(projectCoordinates),
    name = name,
    uploadedTime = uploadedTime.toJavaLocalDateTime(),
    sizeBytes = sizeBytes,
    isExecutable = isExecutable,
).apply {
    this.id = this@toEntity.id
}

/**
 * @return a [Path] with same name as `this`
 */
fun FileDto.toPath(): Path = Paths.get(name)

/**
 * @param projectCoordinates
 * @return [FileDto] constructed from `this` [Path]
 */
fun Path.toFileDto(projectCoordinates: ProjectCoordinates) = FileDto(
    projectCoordinates = projectCoordinates,
    name = name,
    uploadedTime = getLastModifiedTime().toInstant().toKotlinInstant().toLocalDateTime(TimeZone.UTC),
    sizeBytes = fileSize(),
)
