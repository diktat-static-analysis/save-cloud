package com.saveourtool.save.demo.runners.pod

import com.saveourtool.common.demo.DemoResult
import com.saveourtool.common.demo.DemoRunRequest
import com.saveourtool.common.utils.switchIfEmptyToResponseException
import com.saveourtool.save.demo.runners.Runner

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono

import kotlinx.coroutines.reactor.mono

/**
 * Interface that should be implemented by all the runners that use Kubernetes pods in order to run tools for demo.
 */
interface PodRunner : Runner {
    /**
     * @param demoRunRequest params of type [DemoRunRequest]
     * @return tool's report as [DemoResult]
     */
    @Suppress("InjectDispatcher")
    override fun run(demoRunRequest: DemoRunRequest): Mono<DemoResult> = Mono.just(demoRunRequest)
        .doOnNext { logger.info("Sending run request to $it.") }
        .flatMap { mono { runRequest(it) } }
        .switchIfEmptyToResponseException(HttpStatus.INTERNAL_SERVER_ERROR) {
            "Could not fetch demo result."
        }
        .doOnNext { logger.info("Demo finished with [${it.terminationCode}] code.") }

    private suspend fun runRequest(demoRunRequest: DemoRunRequest): DemoResult? = sendRunRequest(demoRunRequest).let { response ->
        response?.takeIf { it.status.isSuccess() }?.body()
    }

    /**
     * @param demoRunRequest run request
     * @return [HttpResponse] if response received, null if something went wrong and response was not received
     */
    suspend fun sendRunRequest(demoRunRequest: DemoRunRequest): HttpResponse?

    companion object {
        private val logger = LoggerFactory.getLogger(PodRunner::class.java)
    }
}
