package com.saveourtool.common.coroutines.flow

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class FlowTest {
    /**
     * @see decodeToString
     */
    @Test
    @JsName("decodeToStringMultiple")
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `decode to string - multiple lines`() = runTest {
        val data = """
            |$ASCII
            |$CYRILLIC
            |
            |$CHINESE
        """.trimMargin()

        val byteFlow = data
            .encodeToByteArray()
            .asSequence()
            .asFlow()

        val stringFlow = byteFlow.decodeToString()

        val lines = stringFlow.toList(mutableListOf())

        assertContentEquals(
            expected = listOf(ASCII, CYRILLIC, "", CHINESE),
            actual = lines,
        )
    }

    /**
     * A single line without newline (`\n`) characters.
     *
     * @see decodeToString
     */
    @Test
    @JsName("decodeToStringSingle")
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `decode to string - single line`() = runTest {
        val byteFlow = ASCII
            .encodeToByteArray()
            .asSequence()
            .asFlow()

        val stringFlow = byteFlow.decodeToString()

        val lines = stringFlow.toList(mutableListOf())

        assertContentEquals(
            expected = listOf(ASCII),
            actual = lines,
        )
    }

    /**
     * @see decodeToString
     */
    @Test
    @JsName("decodeToStringMalformed")
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `decode to string - malformed input`() = runTest {
        /*
         * 0xC0, 0x80 is an invalid UTF-8 byte sequence,
         * see https://github.com/frohoff/jdk8u-jdk/blob/master/test/sun/nio/cs/TestUTF8.java#L276
         *
         * For more examples,
         * see https://www.w3.org/2001/06/utf-8-wrong/UTF-8-test.html
         */
        val byteFlow = byteArrayOf(
            0xC0.toByte(),
            0x80.toByte(),
        )
            .asSequence()
            .asFlow()

        val stringFlow = byteFlow.decodeToString()

        val lines = stringFlow.toList(mutableListOf())

        assertEquals(1, lines.size)
        assertEquals("\uFFFD\uFFFD", lines[0])
    }

    private companion object {
        private const val ASCII = "The quick brown fox jumps over the lazy dog"

        @Suppress("MaxLineLength")
        private const val CYRILLIC =
                "\u0421\u044a\u0435\u0448\u044c\u0020\u0436\u0435\u0020\u0435\u0449\u0451\u0020\u044d\u0442\u0438\u0445\u0020\u043c\u044f\u0433\u043a\u0438\u0445\u0020\u0444\u0440\u0430\u043d\u0446\u0443\u0437\u0441\u043a\u0438\u0445\u0020\u0431\u0443\u043b\u043e\u043a\u0020\u0434\u0430\u0020\u0432\u044b\u043f\u0435\u0439\u0020\u0447\u0430\u044e"
        private const val CHINESE = "\u542c\u8bf4\u5988\u5988\u5b66\u4e60\u753b\u753b\u540e\uff0c\u4f1a\u7231\u4e71\u4e70\u5356\u8d35\u827a\u672f\u3002"
    }
}
