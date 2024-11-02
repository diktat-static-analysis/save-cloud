package com.saveourtool.save.preprocessor.service

import com.saveourtool.common.test.TestSuiteValidationError
import com.saveourtool.common.test.TestSuiteValidationResult
import com.saveourtool.common.testsuite.TestSuiteDto
import com.saveourtool.common.utils.getLogger
import com.saveourtool.save.preprocessor.test.suite.TestSuiteValidator

import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.ParallelFlux
import reactor.core.scheduler.Schedulers

import java.lang.Runtime.getRuntime

import kotlin.math.min

/**
 * Validates test suites discovered by [TestDiscoveringService].
 *
 * @see TestDiscoveringService
 */
@Service
@ManagedResource
@Suppress("WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES")
class TestSuiteValidationService(private val validators: Array<out TestSuiteValidator>) {
    init {
        if (validators.isEmpty()) {
            logger.warn("No test suite validators configured.")
        }
    }

    @Suppress(
        "CUSTOM_GETTERS_SETTERS",
        "WRONG_INDENTATION",
    )
    private val parallelism: Int
        get() =
            when {
                validators.isEmpty() -> 1
                else -> min(validators.size, getRuntime().availableProcessors())
            }

    /**
     * @return the class names of discovered validators.
     */
    @Suppress(
        "CUSTOM_GETTERS_SETTERS",
        "WRONG_INDENTATION",
    )
    @get:ManagedAttribute
    val validatorTypes: List<String>
        get() =
            validators.asSequence()
                .map(TestSuiteValidator::javaClass)
                .map(Class<TestSuiteValidator>::getName)
                .toList()

    /**
     * Invokes all discovered validators and checks [testSuites].
     *
     * @param testSuites the test suites to check.
     * @return the [Flux] of intermediate status updates terminated with the
     *   final update for each check discovered.
     */
    fun validateAll(testSuites: List<TestSuiteDto>): ParallelFlux<TestSuiteValidationResult> =
            when {
                testSuites.isEmpty() -> Flux.just<TestSuiteValidationResult>(
                    TestSuiteValidationError(javaClass.name, "Common", "No test suites found")
                ).parallel(parallelism)

                validators.isEmpty() -> Flux.empty<TestSuiteValidationResult>().parallel(parallelism)

                else -> validators.asSequence()
                    .map { validator ->
                        validator.validate(testSuites)
                    }
                    .reduce { left, right ->
                        left.mergeWith(right)
                    }
                    .parallel(parallelism)
                    .runOn(Schedulers.parallel())
            }

    private companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val logger = getLogger<TestSuiteValidationService>()
    }
}
