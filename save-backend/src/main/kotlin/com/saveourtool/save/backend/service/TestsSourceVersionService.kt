package com.saveourtool.save.backend.service

import com.saveourtool.common.entities.TestsSourceSnapshot
import com.saveourtool.common.entities.TestsSourceVersion
import com.saveourtool.common.entities.TestsSourceVersion.Companion.toEntity
import com.saveourtool.common.repository.UserRepository
import com.saveourtool.common.test.*
import com.saveourtool.common.test.TestsSourceVersionInfo
import com.saveourtool.common.utils.*
import com.saveourtool.save.backend.repository.TestSuitesSourceRepository
import com.saveourtool.save.backend.repository.TestsSourceSnapshotRepository
import com.saveourtool.save.backend.repository.TestsSourceVersionRepository
import com.saveourtool.save.backend.storage.TestsSourceSnapshotStorage

import org.slf4j.Logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for [TestsSourceVersionInfo]
 */
@Service
class TestsSourceVersionService(
    private val snapshotRepository: TestsSourceSnapshotRepository,
    private val versionRepository: TestsSourceVersionRepository,
    private val userRepository: UserRepository,
    private val snapshotStorage: TestsSourceSnapshotStorage,
    private val sourceRepository: TestSuitesSourceRepository,
) {
    /**
     * @param snapshotId [TestsSourceSnapshot.id]
     * @return [TestsSourceSnapshot] found by provided values
     */
    fun getSnapshotEntity(
        snapshotId: Long,
    ): TestsSourceSnapshot = snapshotRepository.getByIdOrNotFound(snapshotId)

    /**
     * @param sourceId ID of [com.saveourtool.save.entities.TestSuitesSource]
     * @param commitId [TestsSourceSnapshot.commitId]
     * @return [TestsSourceSnapshotDto] found by provided values
     */
    fun findSnapshot(
        sourceId: Long,
        commitId: String,
    ): TestsSourceSnapshotDto? = snapshotRepository.findBySourceIdAndCommitId(sourceId, commitId)?.toDto()

    /**
     * @param sourceId
     * @param version
     * @return true if there is [TestsSourceVersionDto] with provided values, otherwise -- false
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun doesVersionExist(
        sourceId: Long,
        version: String,
    ): Boolean = versionRepository.findBySnapshot_SourceIdAndName(sourceId, version) != null

    /**
     * @param organizationName
     * @param sourceName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun getAllAsInfo(
        organizationName: String,
        sourceName: String? = null,
    ): List<TestsSourceVersionInfo> {
        val testsSourceVersions = sourceName
            ?.let { versionRepository.findAllBySnapshot_Source_OrganizationNameAndSnapshot_SourceName(organizationName, it) }
            ?: versionRepository.findAllBySnapshot_Source_OrganizationName(organizationName)
        return testsSourceVersions.map(TestsSourceVersion::toInfo)
    }

    /**
     * @param organizationName
     * @param sourceName
     * @return all fetched version of [TestsSourceVersion] found by provided values
     */
    fun getAllVersions(
        organizationName: String,
        sourceName: String,
    ): Set<String> = versionRepository.findAllBySnapshot_Source_OrganizationNameAndSnapshot_SourceName(
        organizationName,
        sourceName
    ).mapTo(LinkedHashSet(), TestsSourceVersion::name)

    /**
     * @param snapshotId
     * @return all fetched version of [TestsSourceVersion] found by provided values
     */
    fun getAllVersions(
        snapshotId: Long,
    ): Set<String> = versionRepository.findAllBySnapshotId(snapshotId).mapTo(LinkedHashSet(), TestsSourceVersion::name)

    /**
     * Deletes [TestsSourceVersionDto] and [TestsSourceSnapshotDto] if there are no another [TestsSourceVersionDto] related to it
     *
     * @param organizationName
     * @param sourceName
     * @param version
     */
    @Transactional
    fun delete(
        organizationName: String,
        sourceName: String,
        version: String,
    ) {
        val versionEntity =
                versionRepository.findBySnapshot_Source_OrganizationNameAndSnapshot_SourceNameAndName(
                    organizationName = organizationName,
                    sourceName = sourceName,
                    version = version,
                ).orNotFound {
                    "Not found ${TestsSourceVersion::class.simpleName} with version $version in $organizationName/$sourceName"
                }
        doDelete(versionEntity)
    }

    private fun doDelete(versionEntity: TestsSourceVersion) {
        versionRepository.delete(versionEntity)
        val snapshotEntity = versionEntity.snapshot
        if (versionRepository.findAllBySnapshotId(snapshotEntity.requiredId()).isEmpty()) {
            val snapshot = snapshotEntity.toDto()
            snapshotStorage.delete(snapshot)
                .map { deleted ->
                    if (!deleted) {
                        log.warn {
                            "Failed to delete snapshot: $snapshot"
                        }
                    }
                }
                .subscribe()
        }
    }

    /**
     * Saves [TestsSourceVersion] created from provided [TestsSourceVersionDto]
     *
     * @param version the version to save.
     * @return `true` if the [version] was saved, `false` if the version with
     *   the same [name][TestsSourceVersionDto.name] and numeric
     *   [snapshot id][TestsSourceVersionDto.snapshotId] already exists.
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    @Transactional
    fun save(
        version: TestsSourceVersionDto,
    ): Boolean {
        versionRepository.findBySnapshotIdAndName(version.snapshotId, version.name)?.run {
            require(snapshot.requiredId() == version.snapshotId) {
                "Try to save a new $version, but already existed another one linked to another snapshotId: ${toDto()}"
            }
            return false
        }
        val entity = version.toEntity(
            snapshotResolver = snapshotRepository::getByIdOrNotFound,
            userResolver = userRepository::getByIdOrNotFound
        )
        val savedEntity = versionRepository.save(entity)
        // need to update latestFetchedVersion in source
        savedEntity.snapshot.source
            .apply {
                latestFetchedVersion = savedEntity.name
            }
            .let {
                sourceRepository.save(it)
            }
        return true
    }

    companion object {
        private val log: Logger = getLogger<TestsSourceVersionService>()
    }
}
