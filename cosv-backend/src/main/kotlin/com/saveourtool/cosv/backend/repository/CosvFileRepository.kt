package com.saveourtool.cosv.backend.repository

import com.saveourtool.common.entitiescosv.CosvFile
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository for [CosvFile]
 */
@Repository
interface CosvFileRepository : BaseEntityRepository<CosvFile> {
    /**
     * @param identifier
     * @param modifier
     * @return [CosvFile] found by provided values
     */
    fun findByIdentifierAndModified(identifier: String, modifier: LocalDateTime): CosvFile?

    /**
     * @param identifier
     * @return all [CosvFile] with provided [identifier]
     */
    fun findAllByIdentifier(identifier: String): Collection<CosvFile>
}
