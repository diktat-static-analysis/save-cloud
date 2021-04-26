package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Test
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Repository of tests
 */
@Repository
interface TestRepository : BaseEntityRepository<Test>
