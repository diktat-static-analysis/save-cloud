package com.saveourtool.save.test.analysis.api

import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.save.test.analysis.internal.DefaultTestStatusProvider

/**
 * Converts the implementation-specific test status into one of the following
 * flags:
 *
 *  - success,
 *  - failure,
 *  - ignored,
 *  - indeterminate.
 *
 * @param T the implementation-specific test status.
 */
interface TestStatusProvider<T : Enum<T>> {
    /**
     * @return `true` if the implementation-specific test status is a "success".
     * @see TestRun.isSuccess
     */
    fun T.isSuccess(): Boolean

    /**
     * @return `true` if the implementation-specific test status is a "failure".
     * @see TestRun.isFailure
     */
    fun T.isFailure(): Boolean

    /**
     * @return `true` if the implementation-specific test status is "ignored".
     * @see TestRun.isIgnored
     */
    fun T.isIgnored(): Boolean

    /**
     * @return `true` if the implementation-specific test status is neither a
     *   "success", nor a "failure", nor "ignored".
     * @see TestRun.isIndeterminate
     */
    fun T.isIndeterminate(): Boolean

    /**
     * @return `true` if this test run is successful.
     */
    fun TestRun.isSuccess(): Boolean

    /**
     * @return `true` if this test run is a failure.
     */
    fun TestRun.isFailure(): Boolean

    /**
     * @return `true` if this test run is an ignored one (i.e. the test was not
     *   run).
     */
    fun TestRun.isIgnored(): Boolean

    /**
     * @return `true` if this test run is neither [successful][TestRun.isSuccess],
     *   nor a [failure][TestRun.isFailure], nor [ignored][TestRun.isIgnored].
     */
    fun TestRun.isIndeterminate(): Boolean

    companion object Factory {
        /**
         * Creates a new test status provider service.
         *
         * @return a new instance of the default implementation.
         */
        operator fun invoke(): TestStatusProvider<TestResultStatus> =
                DefaultTestStatusProvider()
    }
}
