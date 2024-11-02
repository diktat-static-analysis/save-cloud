@file:JvmName("Flow")
@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.common.coroutines.flow

import okio.Buffer
import kotlin.jvm.JvmName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Decodes this byte flow into a flow of strings, assuming UTF-8 encoding.
 * Strings are emitted each time a newline (`\n`) is encountered in the byte
 * flow, or when the flow completes.
 *
 * Malformed byte sequences are replaced with `\uFFFD`.
 *
 * @see ByteArray.decodeToString
 */
fun Flow<Byte>.decodeToString(): Flow<String> =
        flow {
            val accumulator = Buffer()

            collect { value ->
                when (value) {
                    /*
                     * Ignore.
                     */
                    '\r'.code.toByte() -> Unit

                    '\n'.code.toByte() -> {
                        emit(accumulator.readByteArray())
                        accumulator.clear()
                    }

                    else -> accumulator.writeByte(value.toInt())
                }
            }

            if (accumulator.size > 0) {
                emit(accumulator.readByteArray())
            }
        }
            .map(ByteArray::decodeToString)
