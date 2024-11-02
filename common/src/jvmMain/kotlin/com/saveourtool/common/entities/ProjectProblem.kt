package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntityWithDateAndDto
import javax.persistence.*

/**
 * @property name
 * @property description
 * @property critical
 * @property vulnerabilityMetadataId
 * @property project
 * @property userId
 * @property isClosed
 */
@Entity
@Suppress("LongParameterList")
class ProjectProblem(

    var name: String,

    var description: String,

    @Enumerated(EnumType.STRING)
    var critical: ProjectProblemCritical,

    var vulnerabilityMetadataId: Long?,

    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project,

    var userId: Long,

    var isClosed: Boolean,

) : BaseEntityWithDateAndDto<ProjectProblemDto>() {
    override fun toDto() = ProjectProblemDto(
        name = name,
        description = description,
        critical = critical,
        identifier = "",
        organizationName = project.organization.name,
        projectName = project.name,
        isClosed = isClosed,
        id = id,
    )
}
