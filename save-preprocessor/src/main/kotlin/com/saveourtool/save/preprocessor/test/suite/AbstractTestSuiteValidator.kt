package com.saveourtool.save.preprocessor.test.suite

import com.saveourtool.common.test.TestSuiteValidationResult
import com.saveourtool.common.testsuite.TestSuiteDto
import com.saveourtool.common.utils.getLogger
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

/**
 * The common part of [TestSuiteValidator] implementations.
 */
abstract class AbstractTestSuiteValidator : TestSuiteValidator {
    private val logger = getLogger(javaClass)

    /**
     * Validates test suites.
     *
     * @param testSuites the test suites to check.
     * @param onStatusUpdate the callback to invoke when there's a validation
     *   status update.
     */
    protected abstract fun validate(
        testSuites: List<TestSuiteDto>,
        onStatusUpdate: (status: TestSuiteValidationResult) -> Unit,
    )

    final override fun validate(testSuites: List<TestSuiteDto>): Flux<TestSuiteValidationResult> =
            Flux
                .create { sink ->
                    validate(testSuites) { status ->
                        sink.next(status)
                    }
                    sink.complete()
                }

                /*
                 * Should never be invoked, since this will be a hot Flux.
                 */
                .doOnCancel {
                    logger.warn("Validator ${javaClass.simpleName} cancelled.")
                }

                /*
                 * Off-load from the main thread.
                 */
                .subscribeOn(Schedulers.boundedElastic())

                /*-
                 * Turn this cold Flux into a hot one.
                 *
                 * `cache()` is identical to `replay(history = Int.MAX_VALUE).autoConnect(minSubscribers = 1)`.
                 *
                 * We want `replay()` instead of `publish()`, so that late
                 * subscribers, if any, will observe early published data.
                 */
                .cache()
}
