package com.saveourtool.common.entitiescosv

import com.saveourtool.common.spring.entity.BaseEntityWithDate
import javax.persistence.Entity
import javax.persistence.Table

/**
 * Entity for generated id of vulnerability
 */
@Entity
@Table(schema = "cosv", name = "cosv_generated_id")
class CosvGeneratedId : BaseEntityWithDate() {
    /**
     * @return Vulnerability identifier for saved entity
     */
    fun getIdentifier(): String = "COSV-${requiredCreateDate().year}-${requiredId()}"
}
