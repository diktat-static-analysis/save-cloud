package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.ResultService
import org.cqfn.save.entities.Result
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Controller to work with test result
 *
 * @param resultService service for result
 */
@RestController
class ResultController(private val resultService: ResultService) {
    /**
     * @param results list of test result
     * @return [Mono] with respone
     */
    @PostMapping(value = ["/result"])
    fun saveResult(@RequestBody results: List<Result>): ResponseEntity<String> {
        resultService.addResults(results)
        return ResponseEntity.ok().body("Save")
    }
}
