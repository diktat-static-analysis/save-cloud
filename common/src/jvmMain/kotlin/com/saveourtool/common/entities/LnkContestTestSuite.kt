package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntity

import com.fasterxml.jackson.annotation.JsonBackReference

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property contest
 * @property testSuite
 */
@Entity
class LnkContestTestSuite(
    @ManyToOne
    @JoinColumn(name = "contest_id")
    @JsonBackReference
    var contest: Contest,

    @ManyToOne
    @JoinColumn(name = "test_suite_id")
    var testSuite: TestSuite,
) : BaseEntity()
