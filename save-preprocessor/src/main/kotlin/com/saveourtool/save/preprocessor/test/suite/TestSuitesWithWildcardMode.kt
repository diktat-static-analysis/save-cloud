package com.saveourtool.save.preprocessor.test.suite

import com.saveourtool.common.test.TestSuiteValidationProgress
import com.saveourtool.common.test.TestSuiteValidationResult
import com.saveourtool.common.testsuite.TestSuiteDto
import com.saveourtool.common.utils.getLogger

/**
 * Test suites with wildcard mode.
 */
@TestSuiteValidatorComponent
class TestSuitesWithWildcardMode : AbstractTestSuiteValidator() {
    override fun validate(
        testSuites: List<TestSuiteDto>,
        onStatusUpdate: (status: TestSuiteValidationResult) -> Unit,
    ) {
        require(testSuites.isNotEmpty())

        @Suppress("MAGIC_NUMBER")
        for (i in 0..10) {
            val status = TestSuiteValidationProgress(javaClass.name, CHECK_NAME, i * 10)
            logger.info("Emitting \"$status\"...")
            onStatusUpdate(status)
            Thread.sleep(500L)
        }
    }

    private companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val logger = getLogger<TestSuitesWithWildcardMode>()
        private const val CHECK_NAME = "Searching for test suites with wildcard mode"
    }
}
