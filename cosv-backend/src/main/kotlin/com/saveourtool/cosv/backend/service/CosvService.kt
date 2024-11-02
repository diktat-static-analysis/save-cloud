package com.saveourtool.cosv.backend.service

import com.saveourtool.common.entities.Organization
import com.saveourtool.common.entities.User
import com.saveourtool.common.entities.cosv.*
import com.saveourtool.common.entitiescosv.CosvGeneratedId
import com.saveourtool.common.service.OrganizationService
import com.saveourtool.common.service.UserService
import com.saveourtool.common.utils.*
import com.saveourtool.cosv.backend.processor.CosvProcessor
import com.saveourtool.cosv.backend.repository.CosvGeneratedIdRepository
import com.saveourtool.cosv.backend.repository.CosvRepository
import com.saveourtool.cosv.backend.repository.CosvSchema
import com.saveourtool.cosv.backend.repository.LnkVulnerabilityMetadataTagRepository
import com.saveourtool.cosv.backend.storage.RawCosvFileStorage
import com.saveourtool.osv4k.*
import com.saveourtool.osv4k.RawOsvSchema as RawCosvSchema

import org.slf4j.Logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toFlux

import java.nio.ByteBuffer
import javax.annotation.PostConstruct

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.serializer

typealias VulnerabilityMetadataDtoList = List<VulnerabilityMetadataDtoWithUserAndOrganization>

/**
 * Service for vulnerabilities
 */
@Service
@Suppress("LongParameterList")
class CosvService(
    private val rawCosvFileStorage: RawCosvFileStorage,
    private val cosvRepository: CosvRepository,
    private val userService: UserService,
    private val organizationService: OrganizationService,
    private val tagService: TagService,
    private val cosvProcessor: CosvProcessor,
    private val vulnerabilityMetadataService: VulnerabilityMetadataService,
    private val vulnerabilityRatingService: VulnerabilityRatingService,
    private val lnkVulnerabilityMetadataTagRepository: LnkVulnerabilityMetadataTagRepository,
    private val cosvGeneratedIdRepository: CosvGeneratedIdRepository,
) {
    private val scheduler = Schedulers.boundedElastic()

    /**
     * Init method to restore all raw cosv files with `in progress` state to process
     */
    @PostConstruct
    fun restoreProcessing() {
        waitReactivelyUntil(
            interval = initCheckingInterval,
            numberOfChecks = (initMaxTime / initCheckingInterval).toLong(),
        ) {
            rawCosvFileStorage.isInitDone() && cosvRepository.isReady()
        }
            .filter { it }
            .flatMap {
                doRestoreProcessing()
                    .map {
                        log.info {
                            "Processed all ${RawCosvFileStatus.IN_PROGRESS} files from storage ${RawCosvFileStorage::class.simpleName} after restart"
                        }
                    }
            }
            .lazyDefaultIfEmpty {
                log.warn {
                    "Storage ${RawCosvFileStorage::class.simpleName} and repository ${CosvRepository::class.simpleName} are not initialized in $initMaxTime"
                }
            }
            .subscribeOn(scheduler)
            .subscribe()
    }

    private fun doRestoreProcessing(): Mono<Unit> = rawCosvFileStorage.list()
        .filter { it.status == RawCosvFileStatus.IN_PROGRESS }
        .groupBy { rawCosvFile ->
            rawCosvFile.userName to rawCosvFile.organizationName
        }
        .flatMap { groupedFlux ->
            val (userName, organizationName) = groupedFlux.key()
            blockingToMono {
                userService.getUserByName(userName) to organizationService.getOrganizationByName(organizationName)
            }
                .flatMap { (user, organization) ->
                    groupedFlux
                        .flatMap {
                            doProcess(it.requiredId(), user, organization)
                        }
                        .collectList()
                        .map { it.flatten() }
                        .blockingMap {
                            vulnerabilityRatingService.addRatingForBulkUpload(user, organization, it.size)
                        }
                }
        }
        .thenJust(Unit)

    /**
     * @return generated identifier for COSV
     */
    @Transactional
    fun generateIdentifier(): String = cosvGeneratedIdRepository.saveAndFlush(CosvGeneratedId()).getIdentifier()

    /**
     * Method to process all raw cosv files, add ecosystem tags to new vulnerabilities, update user rating
     *
     * @param rawCosvFileIds
     * @param user
     * @param organization
     * @return empty [Mono]
     */
    fun processAndAddTagsAndUpdateRating(
        rawCosvFileIds: Collection<Long>,
        user: User,
        organization: Organization,
    ): Mono<Unit> = rawCosvFileIds.toFlux()
        .flatMap { rawCosvFileId ->
            validateUserAndOrganization(rawCosvFileId, user, organization)
                .flatMap {
                    doProcess(rawCosvFileId, user, organization)
                }
        }
        .collectList()
        .map { it.flatten() }
        .flatMap { metadataList ->
            Mono.just(metadataList.size).doOnSuccess {
                metadataList.toFlux().flatMap { metadataDto ->
                    getVulnerabilityExt(metadataDto.vulnerabilityMetadataDto.identifier).mapNotNull { vulnerabilityExt ->
                        vulnerabilityExt.cosv.affected?.mapNotNull { it.`package`?.ecosystem }?.toSet()?.let {
                            vulnerabilityExt.metadataDto.vulnerabilityMetadataDto.identifier to it
                        }
                    }
                }
                    .collectList()
                    .blockingMap { identifierToTagsList ->
                        identifierToTagsList.forEach { (identifier, tags) ->
                            tagService.addVulnerabilityTags(identifier, tags)
                        }
                    }
                    .subscribeOn(scheduler)
                    .subscribe()
            }
        }
        .blockingMap {
            vulnerabilityRatingService.addRatingForBulkUpload(user, organization, it)
        }
        .map {
            log.debug {
                "Finished processing raw COSV files $rawCosvFileIds"
            }
        }

    private fun validateUserAndOrganization(
        rawCosvFileId: Long,
        user: User,
        organization: Organization,
    ): Mono<*> = rawCosvFileStorage.getOrganizationIdAndOwnerId(rawCosvFileId)
        .filter { (organizationIdForRawCosvFile, userIdForRawCosvFile) ->
            organization.requiredId() == organizationIdForRawCosvFile && user.requiredId() == userIdForRawCosvFile
        }
        .switchIfEmpty {
            log.error {
                "Submitter ${user.name} is not the owner of the raw cosv file id=$rawCosvFileId or submitted to another organization ${organization.name}"
            }
            Mono.empty()
        }

    private fun doProcess(
        rawCosvFileId: Long,
        user: User,
        organization: Organization,
    ): Mono<VulnerabilityMetadataDtoList> = rawCosvFileStorage.downloadById(rawCosvFileId)
        .collectToInputStream()
        .flatMap { inputStream ->
            val errorMessage by lazy {
                "Failed to process raw COSV file with id: $rawCosvFileId"
            }
            Mono.fromCallable {
                cosvProcessor.decode(inputStream)
            }
                .flatMapIterable { it }
                .flatMap { save(it, user, organization, isAutoApprove = true) }
                .onErrorResume { error ->
                    val cause = error.firstCauseOrThis()
                    rawCosvFileStorage.update(rawCosvFileId, RawCosvFileStatus.FAILED, "$errorMessage is due to ${cause.message}")
                        .then(Mono.error(error))
                }
                .collectList()
                .flatMap { metadataList ->
                    rawCosvFileStorage.update(
                        rawCosvFileId,
                        RawCosvFileStatus.PROCESSED,
                        "Processed as ${metadataList.map(VulnerabilityMetadataDtoWithUserAndOrganization::vulnerabilityMetadataDto).map(VulnerabilityMetadataDto::identifier)}"
                    )
                        .flatMap {
                            rawCosvFileStorage.deleteById(rawCosvFileId)
                        }
                        .thenReturn(metadataList)
                }
                .onErrorResume { error ->
                    log.error(error) { errorMessage }
                    Mono.just(emptyList())
                }
        }

    /**
     * @param cosvId
     * @param updater
     * @return [Mono] with new metadata
     */
    fun update(
        cosvId: String,
        updater: (RawCosvSchema) -> Mono<RawCosvSchema>,
    ): Mono<VulnerabilityMetadataDtoWithUserAndOrganization> = getVulnerabilityExt(cosvId)
        .blockingMap { rawCosvExt ->
            rawCosvExt to Pair(
                userService.getUserByName(rawCosvExt.metadataDto.user.name),
                rawCosvExt.metadataDto.organization?.let { organization ->
                    organizationService.getOrganizationByName(organization.name)
                }
            )
        }
        .flatMap { (rawCosvExt, infoFromDatabase) ->
            val (owner, organization) = infoFromDatabase
            updater(rawCosvExt.cosv)
                .flatMap { newCosv ->
                    save(
                        cosv = newCosv.copy(modified = getCurrentLocalDateTime().truncatedToMills()),
                        user = owner,
                        organization = organization,
                    )
                }
        }

    /**
     * @param cosv
     * @param user
     * @param organization
     * @return metadata for saved COSV
     */
    fun saveManual(
        cosv: ManualCosvSchema,
        user: User,
        organization: Organization?,
    ): Mono<VulnerabilityMetadataDtoWithUserAndOrganization> = save(cosv, user, organization)

    private inline fun <reified D, reified A_E, reified A_D, reified A_R_D> save(
        cosv: CosvSchema<D, A_E, A_D, A_R_D>,
        user: User,
        organization: Organization?,
        isAutoApprove: Boolean = false,
    ): Mono<VulnerabilityMetadataDtoWithUserAndOrganization> = cosvRepository.save(cosv, serializer())
        .flatMap { key ->
            blockingToMono {
                vulnerabilityMetadataService.createOrUpdate(key, cosv, user, organization, isAutoApprove)
                    .toDtoWithUserAndOrganization(user.toUserInfo(), organization?.toDto())
            }
                .onErrorResume { error ->
                    log.error(error) {
                        "Failed to save/update metadata for $key, cleaning up"
                    }
                    cosvRepository.delete(key).then(Mono.error(error))
                }
        }

    /**
     * Finds extended raw cosv with [CosvSchema.id] and max [CosvSchema.modified]
     *
     * @param identifier
     * @return [Mono] with [VulnerabilityExt]
     */
    fun getVulnerabilityExt(identifier: String): Mono<VulnerabilityExt> = blockingToMono { vulnerabilityMetadataService.findByIdentifier(identifier) }
        .flatMap { metadata ->
            cosvRepository.download(metadata.latestCosvFile, serializer<RawCosvSchema>()).blockingMap { content ->
                val links = lnkVulnerabilityMetadataTagRepository
                    .findAllByVulnerabilityMetadataIdentifier(identifier)
                    .map { it.tagId }

                val tags = tagService.findAllByIds(links).map { it.name }.toSet()
                val user = userService.findById(metadata.userId).toUserInfo()
                val organization = metadata.organizationId?.let { organizationService.getOrganizationById(it).toDto() }

                VulnerabilityExt(
                    metadataDto = VulnerabilityMetadataDtoWithUserAndOrganization(metadata.toDto().copy(tags = tags), user, organization),
                    cosv = content,
                    // FixMe: need to fix bug here when mapping is empty
                    saveContributors = content.getSaveContributes().map { userService.getUserByName(it.name).toUserInfo() },
                )
            }
        }

    /**
     * @param identifier
     * @return [Flux] of [ByteBuffer] with COSV's content
     */
    fun getVulnerabilityAsCosvStream(identifier: String): Flux<ByteBuffer> = blockingToMono { vulnerabilityMetadataService.findByIdentifier(identifier) }
        .flatMapMany { metadata -> cosvRepository.downloadAsStream(metadata.latestCosvFile) }

    /**
     * @param cosvFileId
     * @return [Flux] of [ByteBuffer] with COSV's content
     */
    fun getVulnerabilityVersionAsCosvStream(cosvFileId: Long): Flux<ByteBuffer> = cosvRepository.downloadAsStream(cosvFileId)

    /**
     * @param identifier
     * @return list of cosv files
     */
    fun listVersions(identifier: String): Flux<CosvFileDto> = cosvRepository.listVersions(identifier)

    companion object {
        private val log: Logger = getLogger<CosvService>()
        private val initCheckingInterval = 1.seconds
        private val initMaxTime = 5.minutes

        private fun Throwable.firstCauseOrThis(): Throwable = generateSequence(this, Throwable::cause).last()
    }
}
