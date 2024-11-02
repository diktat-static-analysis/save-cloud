package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.entities.TestSuite
import com.saveourtool.common.testsuite.TestSuiteDto
import com.saveourtool.common.utils.blockingToMono
import com.saveourtool.save.backend.service.TestSuitesService

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * Controller for test suites
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "test-suites"),
    Tag(name = "internal"),
)
@RestController
@RequestMapping("/internal/test-suites")
class TestSuitesController(
    private val testSuitesService: TestSuitesService,
) {
    @PostMapping("/save")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Save new test suite into DB.",
        description = "Save new test suite into DB.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully saved test suites.")
    fun saveTestSuite(@RequestBody testSuiteDto: TestSuiteDto): Mono<TestSuite> =
            blockingToMono {
                testSuitesService.saveTestSuite(testSuiteDto)
            }
}
