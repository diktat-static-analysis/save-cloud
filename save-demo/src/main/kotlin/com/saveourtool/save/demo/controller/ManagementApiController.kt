package com.saveourtool.save.demo.controller

import com.saveourtool.common.demo.DemoDto
import com.saveourtool.common.demo.DemoStatus
import com.saveourtool.common.utils.*
import com.saveourtool.save.demo.entity.*
import com.saveourtool.save.demo.service.*

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

/**
 * Controller that allows to query management's api
 */
@Tags(
    Tag(name = "demo"),
    Tag(name = "management"),
)
@RestController
@RequestMapping("/api/demo/manager")
class ManagementApiController(
    private val toolService: ToolService,
    private val demoService: DemoService,
) {
    /**
     * @param organizationName name of GitHub user/organization
     * @param projectName name of GitHub repository
     * @return [Mono] of [DemoStatus] of current demo
     */
    @GetMapping("/{organizationName}/{projectName}/status")
    fun getDemoStatus(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<DemoStatus> = blockingToMono {
        demoService.findBySaveourtoolProject(organizationName, projectName)
    }
        .flatMap { demo -> demoService.getStatus(demo) }
        .defaultIfEmpty(DemoStatus.NOT_CREATED)

    /**
     * @param organizationName name of GitHub user/organization
     * @param projectName name of GitHub repository
     * @return [Mono] of [DemoDto] for [organizationName]/[projectName]
     */
    @GetMapping("/{organizationName}/{projectName}")
    fun getDemoDto(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<DemoDto> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .map { demo ->
            demo.toDto().copy(vcsTagName = "")
        }
        .zipWhen { demoDto ->
            blockingToMono {
                demoDto.githubProjectCoordinates?.let { repo ->
                    toolService.findCurrentVersion(repo)
                } ?: "manual"
            }
        }
        .map { (demoDto, currentVersion) ->
            demoDto.copy(vcsTagName = currentVersion)
        }
}
