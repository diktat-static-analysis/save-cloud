package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.Test
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

import java.util.Optional

/**
 * Repository of tests
 */
@Repository
@Suppress("MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
interface TestRepository : BaseEntityRepository<Test> {
    fun findByHashAndFilePathAndTestSuiteIdAndPluginName(
        hash: String,
        filePath: String,
        testSuiteId: Long,
        pluginName: String
    ): Optional<Test>

    fun findAllByTestSuiteId(testSuiteId: Long): List<Test>

    fun findFirstByTestSuiteId(testSuiteId: Long): Optional<Test>
}
