package com.saveourtool.save.backend.controller

import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.utils.InfraExtension
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.common.info.UserInfo
import com.saveourtool.save.testutils.checkQueues
import com.saveourtool.save.testutils.cleanup
import com.saveourtool.save.testutils.createMockWebServer
import com.saveourtool.save.testutils.enqueue
import com.saveourtool.common.utils.getLogger
import com.saveourtool.common.v1
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.concurrent.TimeUnit

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(InfraExtension::class)
class UsersDetailsControllerTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    @WithMockUser(value = "JohnDoe", roles = ["SUPER_ADMIN"])
    fun `update not approved user`() {
        mutateMockedUser(id = 2)

        val newUserInfo = UserInfo(
            name = "admin",
            email = "example@save.com",
            company = "Example",
        )

        webClient.post()
            .uri("/api/${v1}/users/save")
            .bodyValue(newUserInfo)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    @WithMockUser(value = "user", roles = ["SUPER_ADMIN"])
    fun `new user registration with a free name`() {
        mutateMockedUser(id = 3)

        val newUserInfo = UserInfo(
            name = "Kuleshov",
            email = "example@save.com",
            company = "Example",
        )

        val assertions = mockGatewayUserUpdate()
        webClient.post()
            .uri("/api/${v1}/users/save")
            .bodyValue(newUserInfo)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.OK)
        assertions.forEach { Assertions.assertNotNull(it) }
    }

    @Test
    @WithMockUser(value = "admin", roles = ["SUPER_ADMIN"])
    fun `update not a name but other info`() {
        mutateMockedUser(id = 1)

        val newUserInfo = UserInfo(
            id = 1,
            name = "admin",
            email = "example@save.com",
            company = "Example Company",
        )

        val assertions = mockGatewayUserUpdate()
        webClient.post()
            .uri("/api/${v1}/users/save")
            .bodyValue(newUserInfo)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.OK)
        assertions.forEach { Assertions.assertNotNull(it) }
    }

    @Test
    @WithMockUser(value = "admin", roles = ["SUPER_ADMIN"])
    fun `update existing user with the same name`() {
        mutateMockedUser(id = 1)

        val newUserInfo = UserInfo(
            name = "admin",
            email = "example@save.com",
            company = "Example",
        )

        val assertions = mockGatewayUserUpdate()
        webClient.post()
            .uri("/api/${v1}/users/save")
            .bodyValue(newUserInfo)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.OK)
        assertions.forEach { Assertions.assertNotNull(it) }
    }

    @Test
    @WithMockUser(value = "admin", roles = ["SUPER_ADMIN"])
    fun `update user with taken name`() {
        mutateMockedUser(id = 1)

        val newUserInfo = UserInfo(
            name = "JohnDoe",
            email = "example@save.com",
            company = "Example",
        )

        webClient.post()
            .uri("/api/${v1}/users/save")
            .bodyValue(newUserInfo)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
    }

    companion object {
        private val log: Logger = getLogger<UsersDetailsControllerTest>()

        @JvmStatic
        lateinit var mockServerGateway: MockWebServer

        private fun mockGatewayUserUpdate(): Sequence<RecordedRequest?> {
            mockServerGateway.enqueue(
                "/internal/sec/update",
                MockResponse().setResponseCode(200)
            )
            return sequence {
                yield(mockServerGateway.takeRequest(60, TimeUnit.SECONDS))
            }.onEach {
                log.info("Request $it")
            }
        }

        @AfterEach
        fun cleanup() {
            mockServerGateway.checkQueues()
            mockServerGateway.cleanup()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            mockServerGateway.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerGateway = createMockWebServer()
            mockServerGateway.start()
            registry.add("backend.gatewayUrl") { "http://localhost:${mockServerGateway.port}" }
        }
    }
}
