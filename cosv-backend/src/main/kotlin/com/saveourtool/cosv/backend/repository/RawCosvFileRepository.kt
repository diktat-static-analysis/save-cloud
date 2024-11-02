package com.saveourtool.cosv.backend.repository

import com.saveourtool.common.entitiescosv.RawCosvFile
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

/**
 * A repository for [RawCosvFile]
 */
@Repository
interface RawCosvFileRepository : BaseEntityRepository<RawCosvFile> {
    /**
     * @param organizationId id from [RawCosvFile.organizationId]
     * @param userId id from [RawCosvFile.userId]
     * @param fileName [RawCosvFile.fileName]
     * @return found [RawCosvFile] by provided values
     */
    fun findByOrganizationIdAndUserIdAndFileName(organizationId: Long, userId: Long, fileName: String): RawCosvFile?

    /**
     * @param organizationId id from [RawCosvFile.organizationId]
     * @param userId id from [RawCosvFile.userId]
     * @return all [RawCosvFile]s which has provided [RawCosvFile.organizationId]
     */
    fun findAllByOrganizationIdAndUserId(organizationId: Long, userId: Long): Collection<RawCosvFile>

    /**
     * @param organizationId id from [RawCosvFile.organizationId]
     * @param userId id from [RawCosvFile.userId]
     * @param pageRequest
     * @return all [RawCosvFile]s which has provided [RawCosvFile.organizationId]
     */
    fun findAllByOrganizationIdAndUserId(organizationId: Long, userId: Long, pageRequest: PageRequest): Collection<RawCosvFile>
}
