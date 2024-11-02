/**
 * This file contains serializers for [kotlinx.datetime.LocalDateTime] and [java.time.LocalDateTime]
 */

@file:Suppress(
    "FUNCTION_NAME_INCORRECT_CASE",
    "OBJECT_NAME_INCORRECT",
    "TYPEALIAS_NAME_INCORRECT_CASE",
)

package com.saveourtool.common.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModuleBuilder

typealias KDuration = kotlin.time.Duration
typealias KLocalDateTime = kotlinx.datetime.LocalDateTime
typealias JDuration = java.time.Duration
typealias JLocalDateTime = java.time.LocalDateTime
typealias JInstant = java.time.Instant

private object JLocalDateTimeKSerializer : KSerializer<JLocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("timestamp", PrimitiveKind.LONG)
    private val kotlinxSerializer = KLocalDateTime.serializer()

    override fun deserialize(decoder: Decoder): JLocalDateTime = kotlinxSerializer.deserialize(decoder).toJavaLocalDateTime()

    override fun serialize(encoder: Encoder, value: JLocalDateTime) = kotlinxSerializer.serialize(encoder, value.toKotlinLocalDateTime())
}

private object KDurationJsonSerializer : JsonSerializer<KDuration>() {
    override fun serialize(value: KDuration?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        gen?.codec?.writeValue(gen, value?.toJavaDuration())
    }
}

private object KDurationJsonDeserializer : JsonDeserializer<KDuration>() {
    @Suppress("UnsafeCallOnNullableType")
    override fun deserialize(parser: JsonParser?, ctxt: DeserializationContext?): KDuration =
            parser
                ?.codec
                ?.readValue(parser, JDuration::class.java)
                ?.toKotlinDuration()!!
}

private object KLocalDateTimeJsonSerializer : JsonSerializer<KLocalDateTime>() {
    override fun serialize(value: KLocalDateTime?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        gen?.codec?.writeValue(gen, value?.toJavaLocalDateTime())
    }
}

private object KLocalDateTimeJsonDeserializer : JsonDeserializer<KLocalDateTime>() {
    @Suppress("UnsafeCallOnNullableType")
    override fun deserialize(parser: JsonParser?, ctxt: DeserializationContext?): KLocalDateTime = parser
        ?.codec
        ?.readValue(parser, JLocalDateTime::class.java)
        ?.toKotlinLocalDateTime()!!
}

/**
 * Registers serializer for [java.time.LocalDateTime]
 */
fun SerializersModuleBuilder.supportJLocalDateTime() {
    contextual(JLocalDateTime::class, JLocalDateTimeKSerializer)
}

/**
 * Registers serializer for [kotlinx.datetime.LocalDateTime]
 * We can't use kotlinx.serialization yet https://github.com/saveourtool/save-cloud/issues/908
 *
 * @return original builder
 */
fun Jackson2ObjectMapperBuilder.supportKLocalDateTime(): Jackson2ObjectMapperBuilder = this
    .serializerByType(KLocalDateTime::class.java, KLocalDateTimeJsonSerializer)
    .deserializerByType(KLocalDateTime::class.java, KLocalDateTimeJsonDeserializer)

/**
 * Reads and writes [kotlin.time.Duration] by reading/writing an instance of
 * [java.time.Duration].
 * _Jackson_ can't handle [kotlin.time.Duration] automatically yet.
 * The resulting behaviour will be identical to that of `kotlinx.serialization`
 * (both _JVM_ and _JS Legacy_).
 *
 * @return the original builder.
 */
fun Jackson2ObjectMapperBuilder.supportKDuration(): Jackson2ObjectMapperBuilder =
        serializerByType(KDuration::class.java, KDurationJsonSerializer)
            .deserializerByType(KDuration::class.java, KDurationJsonDeserializer)

/**
 * @return [java.time.Instant] from [java.time.LocalDateTime] at default [java.time.ZoneId]
 */
fun JLocalDateTime.toInstantAtDefaultZone(): JInstant = atZone(java.time.ZoneId.systemDefault()).toInstant()
