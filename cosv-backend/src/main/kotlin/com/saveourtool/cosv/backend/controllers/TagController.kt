package com.saveourtool.cosv.backend.controllers

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.utils.blockingToMono
import com.saveourtool.common.v1
import com.saveourtool.cosv.backend.service.TagService

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for [Tag]s interactions.
 */
@RestController
@ApiSwaggerSupport
@Tags(
    Tag(name = "tags"),
)
@RequestMapping("/api/$v1/tags")
class TagController(
    private val tagService: TagService,
) {
    @Operation(
        method = "GET",
        summary = "Get available vulnerability tags.",
        description = "Get list of tags that are linked with any vulnerability.",
    )
    @Parameters(
        Parameter(name = "prefix", `in` = ParameterIn.QUERY, description = "string prefix of tag name", required = true),
        Parameter(name = "pageSize", `in` = ParameterIn.QUERY, description = "amount of records to be fetched, 3 by default", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully.")
    @ApiResponse(responseCode = "404", description = "User or organization not found.")
    @GetMapping("/vulnerabilities")
    fun getAvailableVulnerabilitiesTags(
        @RequestParam(required = false, defaultValue = "") prefix: String,
        @RequestParam(required = false, defaultValue = "3") pageSize: Int,
    ) = blockingToMono {
        tagService.getVulnerabilityTagsByPrefix(prefix, Pageable.ofSize(pageSize)).map { it.name }.distinct()
    }
}
