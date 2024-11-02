package com.saveourtool.common.execution

import kotlinx.serialization.Serializable

/**
 * Types of testing (that can be selected by user)
 */
@Serializable
enum class TestingType {
    CONTEST_MODE,
    PRIVATE_TESTS,
    PUBLIC_TESTS,
    ;
}
