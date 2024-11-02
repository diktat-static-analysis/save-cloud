package com.saveourtool.save.demo.controller

import com.saveourtool.common.configs.ApiSwaggerSupport
import com.saveourtool.common.demo.DemoDto
import com.saveourtool.common.demo.DemoResult
import com.saveourtool.common.demo.DemoRunRequest
import com.saveourtool.common.filters.DemoFilter
import com.saveourtool.common.utils.blockingToFlux
import com.saveourtool.save.demo.runners.RunnerFactory
import com.saveourtool.save.demo.service.DemoService

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

/**
 * Controller for demo
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "demo"),
)
@RestController
@RequestMapping("/api/demo")
class DemoController(
    private val demoService: DemoService,
    private val demoRunnerFactory: RunnerFactory,
) {
    /**
     * @param filter [DemoFilter], [DemoFilter.any] by default
     * @param demoAmount number of [DemoDto]s that should be fetched, [DemoDto.DEFAULT_FETCH_NUMBER] by default
     * @return all [DemoDto]s matching [filter] as [Flux]
     */
    @PostMapping("/demo-list")
    fun getFilteredDemoList(
        @RequestBody(required = false) filter: DemoFilter?,
        @RequestParam(required = false, defaultValue = DemoDto.DEFAULT_FETCH_NUMBER.toString())
        demoAmount: Int = DemoDto.DEFAULT_FETCH_NUMBER,
    ): Flux<DemoDto> = filter.toMono()
        .switchIfEmpty(DemoFilter.any.toMono())
        .flatMapMany { demoFilter ->
            blockingToFlux { demoService.getFiltered(demoFilter, demoAmount) }
        }
        .map { it.toDto() }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param demoRunRequest request data class with all required additional info
     * @return tool output as [DemoResult]
     */
    @PostMapping("/{organizationName}/{projectName}/run")
    fun runDemo(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestBody demoRunRequest: DemoRunRequest,
    ): Mono<DemoResult> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .zipWith(demoService.getRunnerType())
        .flatMap { (demo, runnerType) ->
            demoRunnerFactory.create(demo, "manual", runnerType).run(demoRunRequest)
        }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @return list of available modes to run the demo with
     */
    @GetMapping("/{organizationName}/{projectName}/modes")
    fun getModes(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Flux<String> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .flatMapIterable { demo -> demo.runCommands.map { runCommand -> runCommand.command } }
}
