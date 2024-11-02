package com.saveourtool.save.demo.runners.pod

import com.saveourtool.common.demo.DemoRunRequest
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.service.KubernetesService
import io.ktor.client.statement.*
import org.springframework.context.annotation.Profile

/**
 * Class that implements [PodRunner] and is a [Demo] PodRunner.
 *
 */
@Profile("kubernetes | minikube")
class DemoPodRunner(
    private val kubernetesService: KubernetesService,
    private val demo: Demo,
) : PodRunner {
    override suspend fun sendRunRequest(demoRunRequest: DemoRunRequest): HttpResponse? = kubernetesService.sendRunRequest(demo, demoRunRequest)
}
