package com.saveourtool.cosv.backend.repository

import com.saveourtool.common.entities.cosv.CosvFileDto
import com.saveourtool.common.entitiescosv.CosvFile
import com.saveourtool.common.utils.*
import com.saveourtool.cosv.backend.storage.CosvFileS3KeyManager
import com.saveourtool.cosv.backend.storage.CosvFileStorage

import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

/**
 * Implementation of [CosvRepository] using [CosvFileStorage]
 */
@Component
class CosvRepositoryInStorage(
    private val cosvFileStorage: CosvFileStorage,
    private val cosvFileS3KeyManager: CosvFileS3KeyManager,
) : CosvRepository {
    private val json = Json {
        prettyPrint = false
    }

    override fun isReady(): Boolean = cosvFileStorage.isInitDone()

    override fun <D, A_E, A_D, A_R_D> save(
        content: CosvSchema<D, A_E, A_D, A_R_D>,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
    ): Mono<CosvFile> = cosvFileStorage.upload(
        content.toCosvFile(),
        json.encodeToString(serializer, content).encodeToByteArray(),
    )

    @OptIn(ExperimentalSerializationApi::class)
    override fun <D, A_E, A_D, A_R_D> download(
        key: CosvFile,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>
    ): CosvSchemaMono<D, A_E, A_D, A_R_D> = cosvFileStorage.download(key)
        .collectToInputStream()
        .map { content -> json.decodeFromStream(serializer, content) }

    override fun downloadAsStream(key: CosvFile): Flux<ByteBuffer> = cosvFileStorage.download(key)

    override fun delete(key: CosvFile): Mono<Unit> = cosvFileStorage.delete(key).filter { it }.thenReturn(Unit)

    override fun deleteAll(identifier: String): Flux<LocalDateTime> = cosvFileStorage.listByIdentifier(identifier)
        .collectList()
        .flatMapMany { keys ->
            cosvFileStorage.deleteAll(keys).flatMapIterable {
                keys.map { it.modified.toKotlinLocalDateTime() }
            }
        }

    override fun listVersions(
        @RequestParam identifier: String,
    ): Flux<CosvFileDto> = blockingToFlux {
        cosvFileS3KeyManager.findAllByIdentifier(identifier).map { it.toDto() }
    }

    override fun downloadAsStream(keyId: Long): Flux<ByteBuffer> = cosvFileStorage.downloadByKeyId(keyId)

    companion object {
        private fun CosvSchema<*, *, *, *>.toCosvFile() = CosvFile(
            identifier = id,
            modified = modified.toJavaLocalDateTime(),
        )
    }
}
