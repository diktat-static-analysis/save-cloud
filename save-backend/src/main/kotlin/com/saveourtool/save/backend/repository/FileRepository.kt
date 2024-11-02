package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.File
import com.saveourtool.common.entities.Project
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository for [File]
 */
@Repository
interface FileRepository : BaseEntityRepository<File> {
    /**
     * @param project
     * @return all [File]s which [project] does contain
     */
    fun findAllByProject(
        project: Project,
    ): List<File>

    /**
     * @param organizationName [File.project.organization.name]
     * @param projectName [File.project.name]
     * @param name [File.name]
     * @param uploadedTime [File.uploadedTime]
     * @return [File] found by provided values or null
     */
    @Suppress(
        "IDENTIFIER_LENGTH",
        "FUNCTION_NAME_INCORRECT_CASE",
        "FunctionNaming",
        "FunctionName",
    )
    fun findByProject_Organization_NameAndProject_NameAndNameAndUploadedTime(
        organizationName: String,
        projectName: String,
        name: String,
        uploadedTime: LocalDateTime,
    ): File?
}
