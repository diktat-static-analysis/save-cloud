package com.saveourtool.common.info

import kotlinx.serialization.Serializable

/**
 * @property canCreateContest permission for create contests in organizations
 * @property canDoBulkUpload permission for upload COSV files in organizations
 */
@Serializable
data class UserPermissionsInOrganization(
    val canCreateContest: Boolean,
    val canDoBulkUpload: Boolean,
)
