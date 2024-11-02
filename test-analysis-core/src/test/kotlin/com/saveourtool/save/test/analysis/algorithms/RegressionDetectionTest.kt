package com.saveourtool.save.test.analysis.algorithms

import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.save.test.analysis.api.TestStatusProvider
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

/**
 * @see RegressionDetection
 */
@ExtendWith(MockitoExtension::class)
class RegressionDetectionTest {
    @Test
    fun `minimum run count of zero should result in a failure`(@Mock testStatusProvider: TestStatusProvider<TestResultStatus>) {
        assertThatThrownBy {
            RegressionDetection(minimumRunCount = 0, testStatusProvider)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Minimum run count should be positive: 0")
    }

    @Test
    fun `negative minimum run count should result in a failure`(@Mock testStatusProvider: TestStatusProvider<TestResultStatus>) {
        assertThatThrownBy {
            RegressionDetection(minimumRunCount = -1, testStatusProvider)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Minimum run count should be positive: -1")
    }
}
