package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntity
import com.saveourtool.common.test.TestDto
import com.saveourtool.common.utils.DATABASE_DELIMITER
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property hash
 * @property filePath path to this test relative to the project root
 * @property additionalFiles additional files
 * @property dateAdded
 * @property testSuite
 * @property pluginName name of a plugin which this test belongs to
 */
@Entity
@Suppress("LongParameterList")
class Test(

    var hash: String,

    var filePath: String,

    var pluginName: String,

    var dateAdded: LocalDateTime,

    @ManyToOne
    @JoinColumn(name = "test_suite_id")
    var testSuite: TestSuite,

    var additionalFiles: String,
) : BaseEntity() {
    /**
     * @return [additionalFiles] as a list of strings
     */
    fun additionalFilesAsList() = additionalFiles.split(DATABASE_DELIMITER).filter { it.isNotBlank() }

    /**
     * @return [TestDto] constructed from `this`
     */
    @Suppress("UnsafeCallOnNullableType")
    fun toDto(): TestDto = TestDto(
        filePath,
        pluginName,
        testSuite.id!!,
        hash,
        additionalFilesAsList(),
    )
}
