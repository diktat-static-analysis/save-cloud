package com.saveourtool.save.orchestrator.docker

import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.orchestrator.copyRecursivelyWithAttributes
import com.saveourtool.save.orchestrator.execTimed
import com.saveourtool.save.orchestrator.getHostIp

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.BuildImageResultCallback
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import java.io.File

import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

/**
 * A class that communicates with docker daemon
 */
@Component
class DockerContainerManager(
    private val configProperties: ConfigProperties,
    private val meterRegistry: MeterRegistry,
    private val dockerClient: DockerClient,
) {
    /**
     * Creates a docker image with provided [resources]
     *
     * @param baseImage base docker image from which this image will be built
     * @param baseDir a context dir for Dockerfile
     * @param resourcesTargetPath target path to additional resources. Resources from [baseDir] will be copied into this directory inside the container.
     * @param runCmd command to append to the Dockerfile. Actual entrypoint is added on container creation.
     * @param imageName name which will be assigned to the image
     * @return id of the created docker image
     * @throws DockerException
     */
    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    internal fun buildImageWithResources(baseImage: String = Sdk.Default.toString(),
                                         imageName: String,
                                         baseDir: File?,
                                         resourcesTargetPath: String?,
                                         runCmd: String = "RUN /bin/bash",
    ): String {
        val tmpDir = createTempDirectory().toFile()
        val tmpResourcesDir = tmpDir.absoluteFile.resolve("resources")
        if (baseDir != null) {
            log.debug("Copying ${baseDir.absolutePath} into $tmpResourcesDir")
            copyRecursivelyWithAttributes(baseDir, tmpResourcesDir)
        }
        val dockerFile = createDockerFile(tmpDir, baseImage, resourcesTargetPath, runCmd)
        val hostIp = getHostIp()
        log.debug("Resolved host IP as $hostIp, will add it to the container")
        val buildImageResultCallback: BuildImageResultCallback = try {
            val buildCmd = dockerClient.buildImageCmd(dockerFile)
                .withBaseDirectory(tmpDir)
                .withTags(setOf(imageName))
                // this is required to be able to access host, e.g. if proxy running on the host is required during image build process
                .withExtraHosts(setOf("host.docker.internal:$hostIp"))
            buildCmd.execTimed(meterRegistry, "save.orchestrator.docker.build", "baseImage", baseImage) { record ->
                object : BuildImageResultCallback() {
                    override fun onComplete() {
                        super.onComplete()
                        record()
                    }
                }
            }
        } finally {
            dockerFile.delete()
            tmpDir.deleteRecursively()
        }
        return buildImageResultCallback.awaitImageId()
    }

    private fun createDockerFile(
        dir: File,
        baseImage: String,
        resourcesPath: String?,
        runCmd: String,
    ): File {
        val dockerFileAsText = buildString {
            append("FROM ${configProperties.docker.registry}/$baseImage")
            if (resourcesPath != null)
                append("COPY resources $resourcesPath")
            append(runCmd)
        }
        val dockerFile = createTempFile(dir.toPath()).toFile()
        dockerFile.writeText(dockerFileAsText)
        return dockerFile
    }

    companion object {
        private val log = LoggerFactory.getLogger(DockerContainerManager::class.java)
    }
}
