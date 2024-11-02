package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntity

import com.fasterxml.jackson.annotation.JsonBackReference

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * @property name
 * @property user
 * @property source
 */
@Entity
@Table(schema = "save_cloud", name = "original_login")
class OriginalLogin(
    var name: String,
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonBackReference
    var user: User,
    var source: String,
) : BaseEntity()
