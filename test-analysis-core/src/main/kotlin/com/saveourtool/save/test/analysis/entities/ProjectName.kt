package com.saveourtool.save.test.analysis.entities

import com.saveourtool.common.entities.Project

/**
 * Project name, intended to be assignment-incompatible with the regular string.
 *
 * @property value the underlying string value.
 */
@JvmInline
value class ProjectName(val value: String) {
    override fun toString(): String =
            value
}

/**
 * @return the name of this project.
 */
fun Project.name(): ProjectName =
        ProjectName(name)
