package com.saveourtool.save.demo.repository

import com.saveourtool.common.spring.repository.BaseEntityRepository
import com.saveourtool.save.demo.entity.RunCommand

import org.springframework.stereotype.Repository

/**
 * JPA repository for [RunCommand] entity.
 */
@Repository
interface RunCommandRepository : BaseEntityRepository<RunCommand>
