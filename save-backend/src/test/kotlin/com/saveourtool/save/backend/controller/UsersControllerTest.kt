package com.saveourtool.save.backend.controller

import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.utils.InfraExtension
import com.saveourtool.common.info.UserInfo
import com.saveourtool.common.v1
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(InfraExtension::class)
class UsersControllerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    @WithMockUser
    fun `new user`() {
        webTestClient.post()
            .uri("/internal/users/new/basic/admin")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk

        webTestClient.get()
            .uri("/api/${v1}/users/admin")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<UserInfo>()
            .consumeWith {
                requireNotNull(it.responseBody)
                Assertions.assertEquals("admin", it.responseBody!!.name)
            }
    }
}
