package com.saveourtool.common.kafka

import com.saveourtool.common.test.TestDto

/**
 * @property tests
 * @property cliArgs
 * @property messageId
 */
data class TestExecutionTaskDto(
    val tests: List<TestDto>,
    val cliArgs: String,
    override val messageId: String? = null
) : KafkaMsg
