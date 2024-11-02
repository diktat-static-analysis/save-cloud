package com.saveourtool.save.preprocessor.service

import com.saveourtool.common.test.TestSuiteValidationError
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * @see TestSuiteValidationService
 */
@ExtendWith(SpringExtension::class)
@Import(TestSuiteValidationService::class)
@ComponentScan("com.saveourtool.save.preprocessor.test.suite")
class TestSuiteValidationServiceTest {
    @Autowired
    private lateinit var validationService: TestSuiteValidationService

    @Test
    fun `non-empty list of validators`() {
        validationService.validatorTypes shouldNotHaveSize 0
    }

    @Test
    fun `empty list of test suites should result in a single error`() {
        val validationResults = validationService.validateAll(emptyList()).sequential().toIterable().toList()

        validationResults shouldHaveSize 1

        val validationResult = validationResults[0]
        assertInstanceOf(TestSuiteValidationError::class.java, validationResult)
        validationResult as TestSuiteValidationError
        validationResult.message shouldBe "No test suites found"
    }
}
