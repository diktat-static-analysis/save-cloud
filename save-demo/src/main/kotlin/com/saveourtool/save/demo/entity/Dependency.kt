package com.saveourtool.save.demo.entity

import com.saveourtool.common.spring.entity.BaseEntity
import javax.persistence.*

/**
 * @property demo
 * @property version
 * @property fileName
 * @property fileId
 */
@Entity
class Dependency(
    @ManyToOne
    @JoinColumn(name = "demo_id")
    var demo: Demo,
    var version: String,
    var fileName: String,
    var fileId: Long,
) : BaseEntity()
