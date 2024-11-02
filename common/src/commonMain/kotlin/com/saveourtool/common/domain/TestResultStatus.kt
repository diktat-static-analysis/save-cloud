package com.saveourtool.common.domain

import kotlinx.serialization.Serializable

/**
 * Enum of results status
 */
@Serializable
enum class TestResultStatus {
    /**
     * Test failed
     */
    FAILED,

    /**
     * Test ignore
     */
    IGNORED,

    /**
     * CLI exit 1
     */
    INTERNAL_ERROR,

    /**
     * Test completed successfully
     */
    PASSED,

    /**
     * Ready for testing (after test discovery and execution creation)
     */
    READY_FOR_TESTING,

    /**
     * Running state (execution of test has already started on agent)
     */
    RUNNING,

    /**
     * CLI exit 42
     */
    TEST_ERROR,
    ;
}
