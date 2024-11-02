package com.saveourtool.save.demo.repository

import com.saveourtool.common.spring.repository.BaseEntityRepository
import com.saveourtool.save.demo.entity.Snapshot

import org.springframework.stereotype.Repository

/**
 * JPA repository for [Snapshot] entity.
 */
@Repository
interface SnapshotRepository : BaseEntityRepository<Snapshot> {
    /**
     * @param versionTag
     * @param executableName
     * @return [Snapshot] if such entity with [versionTag] and [executableName] was found, null otherwise
     */
    fun findByVersionAndExecutableName(versionTag: String, executableName: String): Snapshot?
}
