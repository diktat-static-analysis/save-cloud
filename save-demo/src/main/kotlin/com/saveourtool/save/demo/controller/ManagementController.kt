package com.saveourtool.save.demo.controller

import com.saveourtool.common.demo.DemoAgentConfig
import com.saveourtool.common.demo.DemoDto
import com.saveourtool.common.utils.*
import com.saveourtool.save.demo.entity.*
import com.saveourtool.save.demo.service.*

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

/**
 * Internal controller that allows to create demos
 */
@RestController
@RequestMapping("/internal/demo/manager")
class ManagementController(
    private val downloadToolService: DownloadToolService,
    private val demoService: DemoService,
) {
    /**
     * @param demoDto
     * @return [Mono] of [DemoDto] entity
     */
    @PostMapping("/save-or-update")
    fun saveOrUpdate(@RequestBody demoDto: DemoDto): Mono<DemoDto> = demoDto.toMono()
        .requireOrSwitchToResponseException({ validate() }, HttpStatus.CONFLICT) {
            "Demo creation request is invalid: fill project coordinates, run command and file name."
        }
        .asyncEffect { downloadToolService.initializeGithubDownload(it.githubProjectCoordinates, it.vcsTagName) }
        .flatMap {
            blockingToMono { demoService.saveOrUpdateExisting(it.toDemo(), it.runCommands).toDto() }
        }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version of demo
     * @return [Mono] of [StringResponse]
     */
    @PostMapping("/{organizationName}/{projectName}/delete")
    fun delete(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam(required = false, defaultValue = "manual") version: String,
    ): Mono<StringResponse> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .flatMap { demoService.delete(it, version) }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @return [Mono] of [StringResponse]
     */
    @PostMapping("/{organizationName}/{projectName}/start")
    fun start(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<StringResponse> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .flatMap { demoService.start(it) }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @return [Mono] of [Unit]
     */
    @PostMapping("/{organizationName}/{projectName}/stop")
    fun stop(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<Unit> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .map { demoService.stop(it) }

    /**
     * Get [DemoAgentConfig] of [organizationName]/[projectName] demo with [version]
     *
     * This endpoint makes sense only when kubernetes profile is on
     *
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version of demo
     * @return [DemoAgentConfig]
     */
    @GetMapping("/{organizationName}/{projectName}/configure-me")
    fun sendConfiguration(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam(required = false, defaultValue = "manual") version: String,
    ): Mono<DemoAgentConfig> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .map { demoService.getAgentConfiguration(it, version) }
}
