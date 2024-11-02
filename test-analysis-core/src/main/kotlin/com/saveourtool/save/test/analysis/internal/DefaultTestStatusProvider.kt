package com.saveourtool.save.test.analysis.internal

import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.domain.TestResultStatus.FAILED
import com.saveourtool.common.domain.TestResultStatus.IGNORED
import com.saveourtool.common.domain.TestResultStatus.PASSED
import com.saveourtool.save.test.analysis.api.TestRun
import com.saveourtool.save.test.analysis.api.TestStatusProvider

/**
 * The default implementation of [TestStatusProvider] which uses test statuses
 * provided by [TestResultStatus].
 *
 * @see TestStatusProvider
 * @see TestResultStatus
 */
internal class DefaultTestStatusProvider : TestStatusProvider<TestResultStatus> {
    override fun TestResultStatus.isSuccess(): Boolean =
            this == PASSED

    override fun TestResultStatus.isFailure(): Boolean =
            this == FAILED

    override fun TestResultStatus.isIgnored(): Boolean =
            this == IGNORED

    override fun TestResultStatus.isIndeterminate(): Boolean =
            !isSuccess() && !isFailure() && !isIgnored()

    override fun TestRun.isSuccess(): Boolean =
            status.isSuccess()

    override fun TestRun.isFailure(): Boolean =
            status.isFailure()

    override fun TestRun.isIgnored(): Boolean =
            status.isIgnored()

    override fun TestRun.isIndeterminate(): Boolean =
            status.isIndeterminate()
}
