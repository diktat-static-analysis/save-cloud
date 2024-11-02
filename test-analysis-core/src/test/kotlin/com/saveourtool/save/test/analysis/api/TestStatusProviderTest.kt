package com.saveourtool.save.test.analysis.api

import com.saveourtool.common.domain.TestResultStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * @see TestStatusProvider
 */
class TestStatusProviderTest {
    @Test
    @Suppress("WRONG_NEWLINES")
    fun `test statuses should be mutually exclusive`() {
        with(TestStatusProvider()) {
            enumValues<TestResultStatus>().forEach { status ->
                assertThat(
                    status.isSuccess()
                            xor status.isFailure()
                            xor status.isIgnored()
                            xor status.isIndeterminate()
                ).describedAs(status.name).isTrue()
            }
        }
    }
}
