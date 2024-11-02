package com.saveourtool.common.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import reactor.kotlin.core.publisher.toFlux

class ReactorUtilsTest {

    @Test
    fun bufferUntil() {
        val data = listOf(
            1, 2, 3,
            5, 7,
            2, 5,
            1, 1, 1, 6,
            3,
        )
        val limit = 6

        val result = data.toFlux()
            .bufferAccumulatedUntil { group -> group.sum() < limit }
            .collectList()
            .block()!!

        assertEquals(
            listOf(
                listOf(1, 2, 3),
                listOf(5, 7),
                listOf(2, 5),
                listOf(1, 1, 1, 6),
                listOf(3)
            ),
            result,
        )
    }
}
