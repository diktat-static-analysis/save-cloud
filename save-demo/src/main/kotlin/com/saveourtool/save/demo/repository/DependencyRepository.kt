package com.saveourtool.save.demo.repository

import com.saveourtool.common.spring.repository.BaseEntityRepository
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.entity.Dependency

import org.springframework.stereotype.Repository

/**
 * JPA repository for [Dependency] entity.
 */
@Repository
interface DependencyRepository : BaseEntityRepository<Dependency> {
    /**
     * @param organizationName
     * @param projectName
     * @param version
     * @param fileName
     * @return [Dependency] for [organizationName]/[projectName] demo's specific [version] run
     */
    @Suppress(
        "IDENTIFIER_LENGTH",
        "FUNCTION_NAME_INCORRECT_CASE",
        "FunctionNaming",
        "FunctionName",
    )
    fun findByDemo_OrganizationNameAndDemo_ProjectNameAndVersionAndFileName(
        organizationName: String,
        projectName: String,
        version: String,
        fileName: String,
    ): Dependency?

    /**
     * @param demo
     * @param version
     * @param fileId
     * @return [Dependency] for [demo]'s specific [version] run
     */
    fun findByDemoAndVersionAndFileId(demo: Demo, version: String, fileId: Long): Dependency?

    /**
     * @param organizationName
     * @param projectName
     * @param version
     * @return list of [Dependency] for [organizationName]/[projectName] demo's specific [version] run
     */
    @Suppress(
        "IDENTIFIER_LENGTH",
        "FUNCTION_NAME_INCORRECT_CASE",
        "FunctionNaming",
        "FunctionName",
    )
    fun findAllByDemo_OrganizationNameAndDemo_ProjectNameAndVersion(
        organizationName: String,
        projectName: String,
        version: String,
    ): List<Dependency>
}
