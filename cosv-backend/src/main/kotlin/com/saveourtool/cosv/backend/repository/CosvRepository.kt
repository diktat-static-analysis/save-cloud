package com.saveourtool.cosv.backend.repository

import com.saveourtool.common.entities.cosv.CosvFileDto
import com.saveourtool.common.entitiescosv.CosvFile
import com.saveourtool.osv4k.OsvSchema

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.KSerializer

typealias CosvSchema<D, A_E, A_D, A_R_D> = OsvSchema<D, A_E, A_D, A_R_D>
typealias AnyCosvSchema = CosvSchema<*, *, *, *>
typealias CosvSchemaMono<D, A_E, A_D, A_R_D> = Mono<CosvSchema<D, A_E, A_D, A_R_D>>
@Suppress("TYPEALIAS_NAME_INCORRECT_CASE")
typealias CosvSchemaKSerializer<D, A_E, A_D, A_R_D> = KSerializer<CosvSchema<D, A_E, A_D, A_R_D>>

/**
 * A repository for COSV
 */
interface CosvRepository {
    /**
     * @return true when repository is ready to
     */
    fun isReady(): Boolean

    /**
     * Saves [content] in repository
     *
     * @param content
     * @param serializer [KSerializer] to encode [content] to JSON
     * @return [Mono] with key for saved COSV
     */
    fun <D, A_E, A_D, A_R_D> save(
        content: CosvSchema<D, A_E, A_D, A_R_D>,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
    ): Mono<CosvFile>

    /**
     * Downloads COSV from repository
     *
     * @param key
     * @param serializer [KSerializer] to decode JSON to [CosvSchema]
     * @return [Mono] with content of COSV
     */
    fun <D, A_E, A_D, A_R_D> download(
        key: CosvFile,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
    ): CosvSchemaMono<D, A_E, A_D, A_R_D>

    /**
     * Downloads COSV from repository as [Flux] of [ByteBuffer] (stream)
     *
     * @param key
     * @return [Flux] of [ByteBuffer] with content of COSV
     */
    fun downloadAsStream(
        key: CosvFile,
    ): Flux<ByteBuffer>

    /**
     * Downloads COSV from repository as [Flux] of [ByteBuffer] (stream)
     *
     * @param keyId
     * @return [Flux] of [ByteBuffer] with content of COSV
     */
    fun downloadAsStream(
        keyId: Long,
    ): Flux<ByteBuffer>

    /**
     * Deletes provided version
     *
     * @param key
     * @return [Mono] without body
     */
    fun delete(
        key: CosvFile
    ): Mono<Unit>

    /**
     * Deletes all COSV files (versions) with provided [identifier]
     *
     * @param identifier
     * @return [Flux] with removed versions
     */
    fun deleteAll(
        identifier: String,
    ): Flux<LocalDateTime>

    /**
     * Get all COSV files (versions) with provided [identifier]
     *
     * @param identifier
     * @return [Flux] with all COSV files versions
     */
    fun listVersions(
        identifier: String,
    ): Flux<CosvFileDto>
}
