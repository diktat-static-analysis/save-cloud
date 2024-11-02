package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.benchmarks.AwesomeBenchmarks
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

/**
 * Repository of tests
 */
@Repository
@Suppress("MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
interface AwesomeBenchmarksRepository : BaseEntityRepository<AwesomeBenchmarks> {
    /**
     * We can't use deleteAll in AwesomeBenchmarks, because it won't work in case of corrupted schema
     */
    @Query(
        value = "delete from save_cloud.${AwesomeBenchmarks.TABLE_NAME}",
        nativeQuery = true,
    )
    @Modifying
    @Transactional
    fun deleteAllBenchmarks()
}
