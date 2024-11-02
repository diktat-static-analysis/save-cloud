package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.common.test.TestDto
import com.saveourtool.common.utils.debug
import com.saveourtool.common.utils.getLogger
import com.saveourtool.common.utils.trace
import com.saveourtool.save.backend.service.TestService

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 *  Controller used to initialize tests
 */
@RestController
@RequestMapping("/internal")
class TestController(
    private val testService: TestService,
    private val meterRegistry: MeterRegistry,
) {
    /**
     * @param testDtos list of [TestDto]s to save into the DB
     */
    @PostMapping("/initializeTests")
    fun initializeTests(@RequestBody testDtos: List<TestDto>) {
        log.debug { "Received ${testDtos.size} tests for initialization" }
        log.trace { "Received the following tests for initialization: $testDtos" }
        meterRegistry.timer("save.backend.saveTests").record {
            testService.saveTests(testDtos)
        }
    }

    companion object {
        private val log: Logger = getLogger<TestController>()
    }
}
