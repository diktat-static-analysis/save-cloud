/**
 * Test utilities for orchestrator
 */

package com.saveourtool.save.orchestrator.utils

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.SyncDockerCmd
import com.github.dockerjava.api.exception.DockerException
import com.saveourtool.common.utils.EmptyResponse
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono

/**
 * [EmptyResponse] as [Mono]
 */
internal val emptyResponseAsMono: Mono<EmptyResponse> = Mono.just(ResponseEntity.ok().build())

/**
 * @return [R] result of [C] or null, [DockerException] is ignored
 */
internal fun <R, C : SyncDockerCmd<R>> C.silentlyExec(): R? = try {
    this.exec()
} catch (_: DockerException) {
    // ignoring exception
    null
}

/**
 * Removes [containerId] with volumes and ignoring exceptions
 */
internal fun DockerClient.silentlyCleanupContainer(containerId: String) {
    removeContainerCmd(containerId).withForce(true).withRemoveVolumes(true).silentlyExec()
}
