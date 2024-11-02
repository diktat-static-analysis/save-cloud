package com.saveourtool.save.backend.service

import com.saveourtool.common.entities.ContestSample
import com.saveourtool.common.entities.ContestSampleField
import com.saveourtool.common.entities.contest.ContestSampleDto
import com.saveourtool.common.repository.UserRepository
import com.saveourtool.common.utils.getByIdOrNotFound
import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.backend.repository.contest.ContestSampleFieldRepository
import com.saveourtool.save.backend.repository.contest.ContestSampleRepository

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for contests sample
 *
 * @param contestSampleRepository
 * @param contestSampleFieldRepository
 * @param userRepository
 */
@Service
class ContestSampleService(
    private val contestSampleRepository: ContestSampleRepository,
    private val contestSampleFieldRepository: ContestSampleFieldRepository,
    private val userRepository: UserRepository,
) {
    /**
     * @param contestSampleDto dto of new contest sample
     * @param authentication auth info of a current user
     */
    @Transactional
    fun save(
        contestSampleDto: ContestSampleDto,
        authentication: Authentication,
    ) {
        val userId = authentication.userId()
        val user = userRepository.getByIdOrNotFound(userId)
        val contestSample = ContestSample(
            name = contestSampleDto.name,
            description = contestSampleDto.description,
            user = user,
        )

        val contestSampleNew = contestSampleRepository.saveAndFlush(contestSample)

        val contestSampleFields = contestSampleDto.fields.map {
            ContestSampleField(
                contestSample = contestSampleNew,
                name = it.name,
                type = it.type,
                userId = user.requiredId(),
            )
        }

        contestSampleFieldRepository.saveAll(contestSampleFields)
    }

    /**
     * @return all contest samples
     */
    fun getAll() = contestSampleRepository.findAll()

    /**
     * @param id contest sample id
     * @return contest sample by id
     */
    fun getById(id: Long) = contestSampleRepository.getByIdOrNotFound(id)

    /**
     * @param id contest sample id
     * @return list of contest sample field by contest sample id
     */
    fun getAllContestSampleFieldByContestSampleId(id: Long) = contestSampleFieldRepository.findByContestSampleId(id)
}
