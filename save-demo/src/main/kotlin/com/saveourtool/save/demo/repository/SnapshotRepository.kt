package com.saveourtool.save.demo.repository

import com.saveourtool.save.demo.entity.Snapshot
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * JPA repository for [Snapshot] entity.
 */
@Repository
interface SnapshotRepository : BaseEntityRepository<Snapshot> {
    /**
     * @param versionTag
     * @return
     */
    fun findByVersion(versionTag: String): List<Snapshot>

    /**
     * @param versionTag
     * @param executableName
     * @return
     */
    fun findByVersionAndExecutableName(versionTag: String, executableName: String): Snapshot?
}
