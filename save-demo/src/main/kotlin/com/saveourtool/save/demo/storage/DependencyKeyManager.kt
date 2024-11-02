package com.saveourtool.save.demo.storage

import com.saveourtool.common.storage.concatS3Key
import com.saveourtool.common.storage.key.AbstractS3KeyEntityManager
import com.saveourtool.common.utils.BlockingBridge
import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.entity.Dependency
import com.saveourtool.save.demo.repository.DependencyRepository

import org.springframework.stereotype.Component

/**
 * [com.saveourtool.save.storage.key.S3KeyManager] for [DependencyStorage]
 */
@Component
class DependencyKeyManager(
    configProperties: ConfigProperties,
    repository: DependencyRepository,
    blockingBridge: BlockingBridge,
) : AbstractS3KeyEntityManager<Dependency, DependencyRepository>(
    prefix = concatS3Key(configProperties.s3Storage.prefix, "deps"),
    repository = repository,
    blockingBridge = blockingBridge,
) {
    override fun findByContent(key: Dependency): Dependency? =
            repository.findByDemoAndVersionAndFileId(key.demo, key.version, key.fileId)

    /**
     * @param organizationName
     * @param projectName
     * @param version
     * @param fileName
     * @return [Dependency] found by provided values
     */
    fun findDependency(
        organizationName: String,
        projectName: String,
        version: String,
        fileName: String,
    ): Dependency? = repository.findByDemo_OrganizationNameAndDemo_ProjectNameAndVersionAndFileName(
        organizationName,
        projectName,
        version,
        fileName,
    )

    /**
     * @param organizationName
     * @param projectName
     * @param version
     * @return List of [Dependency] found by provided values
     */
    fun findAllDependencies(
        organizationName: String,
        projectName: String,
        version: String
    ): List<Dependency> = repository.findAllByDemo_OrganizationNameAndDemo_ProjectNameAndVersion(
        organizationName,
        projectName,
        version,
    )
}
