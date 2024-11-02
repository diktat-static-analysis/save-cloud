package com.saveourtool.common.entities

import com.saveourtool.common.spring.entity.BaseEntityWithDto
import java.time.LocalDateTime
import javax.persistence.*

import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * @property name organization
 * @property status
 * @property dateCreated date created organization
 * @property avatar
 * @property description
 * @property canCreateContests
 * @property canBulkUpload ability to bulk upload cosv files
 * @property rating
 */
@Entity
@Serializable
@Table(schema = "save_cloud", name = "organization")
data class Organization(
    var name: String,
    @Enumerated(EnumType.STRING)
    var status: OrganizationStatus,
    @Contextual
    @Column(name = "date_created")
    var dateCreated: LocalDateTime,
    var avatar: String? = null,
    var description: String? = null,
    @Column(name = "can_create_contests")
    var canCreateContests: Boolean = false,
    @Column(name = "can_bulk_upload")
    var canBulkUpload: Boolean = false,
    var rating: Long = 0,
) : BaseEntityWithDto<OrganizationDto>() {
    /**
     * @return [OrganizationDto]
     */
    override fun toDto() = OrganizationDto(
        name = name,
        status = status,
        dateCreated = dateCreated.toKotlinLocalDateTime(),
        avatar = avatar,
        description = description.orEmpty(),
        canCreateContests = canCreateContests,
        canBulkUpload = canBulkUpload,
        rating = rating,
    )

    companion object {
        /**
         * Create a stub for testing.
         *
         * @param id id of created organization
         * @return an organization
         */
        fun stub(
            id: Long?,
        ) = OrganizationDto.empty
            .copy(
                name = "stub"
            )
            .toOrganization()
            .apply {
                this.id = id
            }
    }
}

/**
 * @return [Organization] from [OrganizationDto]
 */
fun OrganizationDto.toOrganization() = Organization(
    name = name,
    status = status,
    dateCreated = dateCreated.toJavaLocalDateTime(),
    avatar = avatar,
    description = description,
    canCreateContests = canCreateContests,
    canBulkUpload = canBulkUpload,
    rating = rating,
)
