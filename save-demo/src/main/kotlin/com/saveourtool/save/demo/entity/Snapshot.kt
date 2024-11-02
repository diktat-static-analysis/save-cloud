package com.saveourtool.save.demo.entity

import com.saveourtool.common.spring.entity.BaseEntity
import javax.persistence.Entity

/**
 * @property version
 * @property executableName
 */
@Entity
class Snapshot(
    var version: String,
    var executableName: String,
) : BaseEntity()
