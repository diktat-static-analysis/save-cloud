package com.saveourtool.save.preprocessor.test.suite

import com.saveourtool.common.test.TestSuiteValidationResult
import com.saveourtool.common.testsuite.TestSuiteDto
import reactor.core.publisher.Flux
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

/**
 * A particular validation check.
 *
 * Implementations _should_:
 *  - make sure the [Flux] returned by [validate] is a hot [Flux], so that
 *    cancelling a particular subscriber (e.g.: in case of a network outage)
 *    doesn't affect validation;
 *  - off-load the actual work to a separate [Scheduler], such as
 *   [Schedulers.boundedElastic].
 *  - be annotated with [TestSuiteValidatorComponent].
 *
 * Implementations _may_:
 *  - inherit from [AbstractTestSuiteValidator].
 *
 * @see TestSuiteValidatorComponent
 * @see AbstractTestSuiteValidator
 */
fun interface TestSuiteValidator {
    /**
     * Validates test suites, returning a [Flux] of intermediate status updates
     * terminated with the final update.
     *
     * @param testSuites the test suites to check.
     * @return the [Flux] of intermediate status updates terminated with the
     *   final update.
     */
    fun validate(testSuites: List<TestSuiteDto>): Flux<TestSuiteValidationResult>
}
