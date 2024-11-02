package com.saveourtool.save.backend.controllers

import com.saveourtool.common.entities.contest.ContestSampleDto
import com.saveourtool.common.utils.StringResponse
import com.saveourtool.common.utils.blockingToFlux
import com.saveourtool.common.utils.blockingToMono
import com.saveourtool.common.v1
import com.saveourtool.save.backend.service.ContestSampleService

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * Controller for working with contests sample.
 */
@RestController
@RequestMapping(path = ["/api/$v1/contests/sample"])
class ContestSampleController(
    private val contestSampleService: ContestSampleService,
) {
    @PostMapping("/save")
    @Operation(
        method = "POST",
        summary = "Save contest sample.",
        description = "Save contest sample.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully saved contest sample")
    @PreAuthorize("permitAll()")
    fun save(
        @RequestBody contestSampleDto: ContestSampleDto,
        authentication: Authentication,
    ): Mono<StringResponse> = blockingToMono {
        contestSampleService.save(contestSampleDto, authentication)
    }.map {
        ResponseEntity.ok("Contest sample was successfully saved")
    }

    @GetMapping("/get/all")
    @Operation(
        method = "GET",
        summary = "Get all contest samples.",
        description = "Get list of all contest samples.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of contest samples.")
    fun getAll() = blockingToFlux {
        contestSampleService.getAll().map { it.toDto() }
    }

    @GetMapping("/get")
    @Operation(
        method = "GET",
        summary = "Get contest sample by id.",
        description = "Get contest sample by id.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched of contest sample.")
    fun getById(
        @RequestParam id: Long,
    ) = blockingToMono {
        contestSampleService.getById(id).toDto()
    }

    @GetMapping("/get-fields/by-sample-id")
    @Operation(
        method = "GET",
        summary = "Get all contest sample field by contest sample id.",
        description = "Get all contest sample field by contest sample id.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of contest sample fields.")
    fun getFieldsBySampleId(
        @RequestParam id: Long,
    ) = blockingToMono {
        contestSampleService.getAllContestSampleFieldByContestSampleId(id).map { it.toDto() }
    }
}
