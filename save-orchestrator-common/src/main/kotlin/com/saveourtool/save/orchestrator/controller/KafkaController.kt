package com.saveourtool.save.orchestrator.controller

import com.saveourtool.common.kafka.TestExecutionTaskDto
import com.saveourtool.common.v1
import com.saveourtool.save.orchestrator.kafka.KafkaSender

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for working with contests.
 */
@Profile("dev & kafka")
@RestController
@RequestMapping(path = ["/api/$v1/kafka"])
internal class KafkaController(
    private val testExecutionSender: KafkaSender<TestExecutionTaskDto>
) {
    /**
     * @param task
     * @return Organization
     */
    @PostMapping("/sendTestExecutionTask")
    fun sendTestExecutionTask(@RequestBody task: TestExecutionTaskDto) {
        testExecutionSender.sendMessage(task)
    }
}
