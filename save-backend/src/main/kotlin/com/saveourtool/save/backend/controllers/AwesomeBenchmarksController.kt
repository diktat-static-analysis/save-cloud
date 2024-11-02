/**
 * Controller for processing awesome-benchmarks:
 * 1) to put benchamrks to DB
 * 2) to get benchmarks for the Frontend
 */

package com.saveourtool.save.backend.controllers

import com.saveourtool.common.entities.benchmarks.BenchmarkEntity
import com.saveourtool.common.v1
import com.saveourtool.save.backend.service.AwesomeBenchmarksService

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
/**
 * Controller for processing awesome-benchmarks
 */
class AwesomeBenchmarksController(
    private val awesomeBenchmarksService: AwesomeBenchmarksService,
) {
    private val log = LoggerFactory.getLogger(AwesomeBenchmarksController::class.java)

    /**
     * @param benchmarks
     */
    @PostMapping("/internal/upload/awesome-benchmarks")
    fun uploadAwesomeBenchmarks(@RequestBody(required = true) benchmarks: List<BenchmarkEntity>) {
        log.info("Received a request to save awesome-benchmarks to the db")
        awesomeBenchmarksService.saveBenchmarksToDb(benchmarks)
        log.info("Saved requested awesome-benchmarks to the db")
    }

    /**
     * @return all benchmarks from backend to frontend
     */
    @GetMapping(path = ["/api/$v1/awesome-benchmarks"])
    fun getAllAwesomeBenchmarks() =
            awesomeBenchmarksService.getAllBenchmarks()
}
