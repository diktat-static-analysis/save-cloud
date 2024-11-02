package com.saveourtool.save.demo.utils

import com.saveourtool.save.demo.config.KubernetesConfig
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.common.domain.Sdk
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Tests for [ByteReadChannel.toByteBufferFlux].
 *
 * @see ByteReadChannel.toByteBufferFlux
 */
class HttpUtilsTest {
    @Test
    fun `empty channel`() {
        val emptyChannel: ByteReadChannel = ByteReadChannel.Empty

        val byteBuffersOrNull: List<ByteBuffer>? = emptyChannel.toByteBufferFlux().collectList().block()
        assertThat(byteBuffersOrNull).isNotNull.isEmpty()
    }

    @Test
    fun `closed channel`() {
        val closedStream = ByteArrayInputStream(byteArrayOf()).apply {
            close()
        }

        assertThat(closedStream.read()).isEqualTo(-1)

        val closedChannel = closedStream.toByteReadChannel()

        val byteBuffersOrNull: List<ByteBuffer>? = closedChannel.toByteBufferFlux().collectList().block()
        assertThat(byteBuffersOrNull).isNotNull.isEmpty()
    }

    @Test
    fun `exception thrown when blocking`() {
        val errorMessage = "Message"

        val faultyChannel = object : InputStream() {
            override fun read() =
                    throw IOException(errorMessage)
        }.toByteReadChannel()

        val buffersOrNone: Mono<List<ByteBuffer>> = faultyChannel.toByteBufferFlux().collectList()

        assertThatThrownBy {
            buffersOrNone.block()
        }.isInstanceOf(RuntimeException::class.java)
            .hasMessage("${IOException::class.qualifiedName}: $errorMessage")
            .hasCauseExactlyInstanceOf(IOException::class.java)
    }

    @Test
    fun addressToDnsResolutionTest() {
        val resolvableAddress = addressToDnsResolution("192.168.0.1", stubKubernetesConfig)
        with(stubKubernetesConfig) {
            assertThat(resolvableAddress).isEqualTo(
                "192-168-0-1.${agentSubdomainName}.${currentNamespace}.svc.cluster.local"
            )
        }
    }

    @Test
    fun addressByServiceNameTest() {
        val resolvableAddress = addressByServiceName(stubDemo, stubKubernetesConfig)
        assertThat(resolvableAddress).isEqualTo(
            "${serviceNameForDemo(stubDemo)}.${stubKubernetesConfig.agentNamespace}.svc.cluster.local"
        )
    }

    companion object {
        private val stubDemo = Demo(
            "organization",
            "project",
            Sdk.Default.toString(),
            "",
            null,
            null,
            null,
            null,
        )

        private val stubKubernetesConfig = KubernetesConfig(
            "",
            "",
            "my-namespace",
            false,
            "my-subdomain-name",
            23456,
            agentNamespace = "agent-namespace",
        )
    }
}
