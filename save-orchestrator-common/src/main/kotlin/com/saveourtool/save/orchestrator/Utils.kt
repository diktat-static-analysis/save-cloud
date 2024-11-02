/**
 * Utilities for orchestrator
 */

package com.saveourtool.save.orchestrator

import com.saveourtool.common.agent.AgentEnvName
import com.saveourtool.save.orchestrator.config.ConfigProperties.AgentSettings

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.AsyncDockerCmd
import com.github.dockerjava.api.command.ListImagesCmd
import com.github.dockerjava.api.command.SyncDockerCmd
import com.github.dockerjava.api.model.Image
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.function.Supplier
import java.util.zip.GZIPOutputStream

internal const val DOCKER_METRIC_PREFIX = "save.orchestrator.docker"

/**
 * Execute this async docker command while recording its execution duration.
 *
 * @param meterRegistry a registry to record data to
 * @param name name of the timer
 * @param tags additional tags for the timer (command name in form of Java class name is assigned automatically)
 * @param resultCallbackProducer a function returning result callback. Should call its argument to record duration.
 * @return async result
 */
inline fun <reified CMD_T : AsyncDockerCmd<CMD_T, A_RES_T>, RC_T : ResultCallback<A_RES_T>, A_RES_T> CMD_T.execTimed(
    meterRegistry: MeterRegistry,
    name: String,
    vararg tags: String,
    resultCallbackProducer: (() -> Unit) -> RC_T,
): RC_T {
    val timer = meterRegistry.timer(name, "cmd", "${CMD_T::class.simpleName}", *tags)
    val sample = Timer.start(meterRegistry)
    return exec(resultCallbackProducer {
        sample.stop(timer)
    })
}

/**
 * Execute this sync docker command while recording its execution duration.
 *
 * @param meterRegistry a registry to record data to
 * @param name name of the timer
 * @param tags additional tags for the timer (command name in form of Java class name is assigned automatically)
 * @return sync result
 */
inline fun <reified CMD_T : SyncDockerCmd<RES_T>, RES_T : Any?> CMD_T.execTimed(
    meterRegistry: MeterRegistry,
    name: String,
    vararg tags: String,
): RES_T {
    val timer = meterRegistry.timer(name, "cmd", "${CMD_T::class.simpleName}", *tags)
    val result = timer.record(Supplier {
        exec()
    })
    // nullability should be specified at call site
    return result as RES_T
}

/**
 * @param imageId id (not name) of the image to look for
 * @param meterRegistry a [MeterRegistry] to record the operation to
 * @return list of images
 */
internal fun DockerClient.findImage(imageId: String, meterRegistry: MeterRegistry) = listImagesCmd()
    .execTimed<ListImagesCmd, MutableList<Image>>(meterRegistry, "$DOCKER_METRIC_PREFIX.image.list")
    .find {
        // fixme: sometimes createImageCmd returns short id without prefix, sometimes full and with prefix.
        it.id.replaceFirst("sha256:", "").startsWith(imageId.replaceFirst("sha256:", ""))
    }

/**
 * Build map of env variables that can be read by save-agent to override settings from properties file
 *
 * @param agentSettings configuration of save-agent loaded from save-orchestrator
 * @param executionId ID of [com.saveourtool.save.entities.Execution] which to be processed
 * @return map of env variables with their values
 */
internal fun fillAgentPropertiesFromConfiguration(
    agentSettings: AgentSettings,
    executionId: Long,
): Map<AgentEnvName, String> = buildMap {
    put(AgentEnvName.EXECUTION_ID, executionId.toString())

    with(agentSettings) {
        put(AgentEnvName.HEARTBEAT_URL, heartbeatUrl)
        debug?.let { put(AgentEnvName.DEBUG, it.toString()) }
    }
}

/**
 * Add [files] to .tar.gz archive and return the underlying [ByteArrayOutputStream]
 *
 * @param files files to be added to archive
 * @return resulting [ByteArrayOutputStream]
 */
internal fun createTgzStream(vararg files: File): ByteArrayOutputStream {
    val out = ByteArrayOutputStream()
    BufferedOutputStream(out).use { buffOut ->
        GZIPOutputStream(buffOut).use { gzOut ->
            TarArchiveOutputStream(gzOut).use { tgzOut ->
                files.forEach {
                    tgzOut.putArchiveEntry(TarArchiveEntry(it, it.name))
                    Files.copy(it.toPath(), tgzOut)
                    tgzOut.closeArchiveEntry()
                }
                tgzOut.finish()
            }
            gzOut.finish()
        }
        buffOut.flush()
    }
    return out
}
