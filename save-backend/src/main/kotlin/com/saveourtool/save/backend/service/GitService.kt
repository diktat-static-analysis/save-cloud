package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.GitRepository
import com.saveourtool.save.backend.utils.switchToNotFoundIfEmpty
import com.saveourtool.save.entities.Git
import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Organization
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Service of git
 */
@Service
@RestController
class GitService(private val gitRepository: GitRepository) {
    /**
     * @param organization
     * @return list of gits by organization if exists
     */
    fun getAllByOrganization(organization: Organization): List<Git> = gitRepository.findAllByOrganizationId(organization.requiredId())

    /**
     * @param organization
     * @param url
     * @return list of gits by organization if exists
     */
    fun getByOrganizationAndUrl(organization: Organization, url: String): Git = gitRepository.findByOrganizationAndUrl(organization, url)
        ?: throw NoSuchElementException("There is no git credential with url $url in ${organization.name}")

    /**
     * @param organization associate Git with this organization
     * @param gitDto
     * @return saved or updated git
     */
    fun upsert(organization: Organization, gitDto: GitDto): Git =
            Git(
                url = gitDto.url,
                username = gitDto.username,
                password = gitDto.password,
                organization = organization,
            ).also {
                it.id = gitRepository.findByOrganizationAndUrl(organization, gitDto.url)?.id
            }.let { gitRepository.save(it) }

    /**
     * @param organization
     * @param url git url
     * @throws NoSuchElementException
     */
    fun delete(organization: Organization, url: String) {
        gitRepository.delete(getByOrganizationAndUrl(organization, url))
    }

    @GetMapping("/internal/git")
    fun getById(@RequestParam id: Long): Mono<GitDto> = Mono.justOrEmpty<Git>(gitRepository.findByIdOrNull(id))
        .map { it.toDto() }
        .switchToNotFoundIfEmpty {
            "Git entity not found by id $id"
        }

    /**
     * @param url
     * @return some git credential with provided [url] or null
     */
    fun findByUrl(url: String): Git? = gitRepository.findAllByUrl(url).firstOrNull()
}
