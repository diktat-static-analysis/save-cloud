package com.saveoourtool.save.demo.agent

import com.saveourtool.common.demo.ServerConfiguration
import com.saveourtool.save.demo.agent.server
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.*

class ServerTest {
    private val serverUrl = "localhost"
    private val serverConfiguration = ServerConfiguration()
    private val server = server(serverConfiguration, true)

    @BeforeTest
    fun startServer() {
        server.start()
    }

    @AfterTest
    fun stopServer() {
        server.stop()
    }

    @OptIn(ExperimentalNativeApi::class)
    @Test
    fun testServerStartup() {
        httpClient().use { client ->
            coroutineScope.launch {
                assert(client.get("$serverUrl:${serverConfiguration.port}/alive").status.isSuccess())
            }
        }
    }

    companion object {
        private val coroutineScope = CoroutineScope(Dispatchers.Default)
        private const val TIMEOUT_MILLIS = 500L
        private fun httpClient() = HttpClient(CIO) { engine { requestTimeout = TIMEOUT_MILLIS } }
    }
}
