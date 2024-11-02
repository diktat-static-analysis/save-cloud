@file:Suppress("SAY_NO_TO_VAR")

package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.common.entities.GitDto
import com.saveourtool.save.preprocessor.utils.RepositoryVolume

import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.core.ParameterizedTypeReference
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(controllers = [GitPreprocessorController::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureWebTestClient(timeout = "60000")
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
class GitPreprocessorControllerTest(
    @Autowired private var webClient: WebTestClient,
) : RepositoryVolume {
    @Test
    fun testCheckConnectivity() {
        var user = "test"
        var token = "test-token"
        var url = "https://github.com/saveourtool/save-cloud"
        doTestCheckConnectivity(user, token, url, true)

        user = "akuleshov7"
        token = "test-token"
        url = "https://github.com/saveourtool/save-cloud111"
        doTestCheckConnectivity(user, token, url, false)
    }

    private fun doTestCheckConnectivity(
        user: String,
        token: String,
        url: String,
        value: Boolean,
    ) {
        webClient
            .post()
            .uri("/git/check-connectivity")
            .bodyValue(GitDto(
                url,
                user,
                token,
            ))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ParameterizedTypeReference.forType<Boolean>(Boolean::class.java))
            .value<Nothing> {
                Assertions.assertEquals(it, value)
            }
    }

    @Test
    fun testDetectDefaultBranchName() {
        doTestDetectDefaultBranchName("https://github.com/saveourtool/save-cloud", "master")

        doTestDetectDefaultBranchName("https://github.com/saveourtool/save-cli", "main")
    }

    private fun doTestDetectDefaultBranchName(
        url: String,
        value: String,
    ) {
        webClient
            .post()
            .uri("/git/default-branch-name")
            .bodyValue(GitDto(url))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ParameterizedTypeReference.forType<String>(String::class.java))
            .value<Nothing> {
                Assertions.assertEquals(it, value)
            }
    }
}
