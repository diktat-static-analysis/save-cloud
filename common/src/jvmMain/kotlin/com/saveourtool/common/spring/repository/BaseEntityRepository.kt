package com.saveourtool.common.spring.repository

import com.saveourtool.common.spring.entity.BaseEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.QueryByExampleExecutor

/**
 * JPA repositories for the subclasses of the BaseEntity
 */
@NoRepositoryBean
interface BaseEntityRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T>, QueryByExampleExecutor<T>
