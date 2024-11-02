package com.saveourtool.cosv.backend.service

import com.saveourtool.common.entities.Tag
import com.saveourtool.common.entitiescosv.LnkVulnerabilityMetadataTag
import com.saveourtool.common.repository.TagRepository
import com.saveourtool.common.utils.error
import com.saveourtool.common.utils.getLogger
import com.saveourtool.common.utils.orNotFound
import com.saveourtool.common.validation.TAG_ERROR_MESSAGE
import com.saveourtool.common.validation.isValidTag
import com.saveourtool.cosv.backend.repository.LnkVulnerabilityMetadataTagRepository
import com.saveourtool.cosv.backend.repository.VulnerabilityMetadataRepository

import org.slf4j.Logger
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

/**
 * Service for tag
 */
@Service
class TagService(
    private val tagRepository: TagRepository,
    private val vulnerabilityMetadataRepository: VulnerabilityMetadataRepository,
    private val lnkVulnerabilityMetadataTagRepository: LnkVulnerabilityMetadataTagRepository,
) {
    /**
     * @param name name of tag
     * @return Tag
     */
    fun saveTag(name: String) = tagRepository.save(Tag(name))

    /**
     * @param name
     * @return tag with [name]
     */
    fun findTagByName(name: String): Tag? = tagRepository.findByName(name)

    /**
     * @param ids
     * @return list of tag
     */
    fun findAllByIds(ids: List<Long>): List<Tag> = tagRepository.findAllByIdIn(ids)

    /**
     * @param identifier [com.saveourtool.save.entities.cosv.VulnerabilityMetadata.identifier]
     * @param tagName tag to add
     * @return new [LnkVulnerabilityMetadataTag]
     * @throws ResponseStatusException on invalid [tagName] (with [HttpStatus.CONFLICT])
     */
    @Transactional
    fun addVulnerabilityTag(identifier: String, tagName: String): LnkVulnerabilityMetadataTag {
        if (!tagName.isValidTag()) {
            throw ResponseStatusException(HttpStatus.CONFLICT, TAG_ERROR_MESSAGE)
        }
        val metadata = vulnerabilityMetadataRepository.findByIdentifier(identifier).orNotFound {
            "Could not find metadata for vulnerability $identifier"
        }
        val tag = tagRepository.findByName(tagName) ?: tagRepository.save(Tag(tagName))

        return lnkVulnerabilityMetadataTagRepository.save(
            LnkVulnerabilityMetadataTag(metadata, tag.requiredId())
        )
    }

    /**
     * @param identifier [com.saveourtool.save.entities.cosv.VulnerabilityMetadata.identifier]
     * @param tagNames tags to add
     * @return new [LnkVulnerabilityMetadataTag]
     */
    @Transactional
    fun addVulnerabilityTags(identifier: String, tagNames: Set<String>): List<LnkVulnerabilityMetadataTag>? {
        if (tagNames.any { !it.isValidTag() }) {
            log.error { TAG_ERROR_MESSAGE }
            return null
        }

        val metadata = vulnerabilityMetadataRepository.findByIdentifier(identifier) ?: run {
            log.error { "Could not find metadata for vulnerability $identifier" }
            return null
        }

        val links = tagNames.map {
            tagRepository.findByName(it) ?: tagRepository.save(Tag(it))
        }.map {
            LnkVulnerabilityMetadataTag(metadata, it.requiredId())
        }

        return lnkVulnerabilityMetadataTagRepository.saveAll(links)
    }

    /**
     * @param identifier
     * @param tagName
     */
    @Transactional
    fun deleteVulnerabilityTag(identifier: String, tagName: String) {
        val metadata = vulnerabilityMetadataRepository.findByIdentifier(identifier).orNotFound {
            "Could not find metadata for vulnerability $identifier"
        }

        val tag = tagRepository.findByName(tagName).orNotFound { "Could not find tag with name $tagName" }
        val link = lnkVulnerabilityMetadataTagRepository.findByVulnerabilityMetadataIdAndTagId(
            metadata.requiredId(),
            tag.requiredId()
        ).orNotFound { "Tag '$tagName' is not linked with vulnerability $identifier." }

        lnkVulnerabilityMetadataTagRepository.delete(link)
    }

    /**
     * @param prefix [String] that should be matched with tag prefix
     * @param page [Pageable]
     * @return [List] of [Tag]s with [Tag.name] that starts with [prefix]
     */
    @Suppress("UnusedParameter")
    fun getVulnerabilityTagsByPrefix(
        prefix: String,
        page: Pageable,
    ) = tagRepository.findAllByNameStartingWith(prefix)

    companion object {
        private val log: Logger = getLogger<TagService>()
    }
}
