package com.saveourtool.common.entities

import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.spring.entity.BaseEntityWithDto

import javax.persistence.*

import kotlinx.serialization.Serializable

/**
 * @property name
 * @property url
 * @property description description of the project, may be absent
 * @property status status of project
 * @property public
 * @property email
 * @property numberOfContainers
 * @property organization
 * @property contestRating global rating based on all contest results associated with this project
 */
@Entity
@Serializable
@Table(schema = "save_cloud", name = "project")
data class Project(
    var name: String,
    var url: String?,
    var description: String?,
    @Enumerated(EnumType.STRING)
    var status: ProjectStatus,
    var public: Boolean = true,
    var email: String? = null,
    @Column(name = "number_of_containers")
    var numberOfContainers: Int = 3,

    @ManyToOne
    @JoinColumn(name = "organization_id")
    var organization: Organization,
    @Column(name = "contest_rating")
    var contestRating: Double = 0.0,
) : BaseEntityWithDto<ProjectDto>() {
    /**
     * @return [ProjectDto] from [Project]
     */
    override fun toDto() = ProjectDto(
        name = name,
        url = url.orEmpty(),
        description = description.orEmpty(),
        status = status,
        isPublic = public,
        email = email.orEmpty(),
        numberOfContainers = numberOfContainers,
        organizationName = organization.name,
        contestRating = contestRating,
    )

    /**
     * Return the shortest unique representation of this [Project] as a string
     */
    fun shortToString() = "[organization=${organization.name},name=$name]"

    /**
     * @return [ProjectCoordinates] is built for current entity
     */
    fun toProjectCoordinates(): ProjectCoordinates = ProjectCoordinates(
        organizationName = organization.name,
        projectName = name,
    )

    companion object {
        /**
         * Create a stub for testing. Since all fields are mutable, only required ones can be set after calling this method.
         *
         * @param id id of created project
         * @param organization
         * @return a project
         */
        fun stub(
            id: Long?,
            organization: Organization = Organization.stub(null)
        ) = ProjectDto.empty
            .copy(
                name = "stub"
            )
            .toProject(organization)
            .apply {
                this.id = id
            }
    }
}

/**
 * @param organization organization that is an owner of a given project
 * @return [Project] from [ProjectDto]
 */
fun ProjectDto.toProject(
    organization: Organization,
) = Project(
    name = name,
    url = url,
    description = description,
    status = status,
    public = isPublic,
    email = email,
    numberOfContainers = numberOfContainers,
    organization = organization,
    contestRating = contestRating,
)
