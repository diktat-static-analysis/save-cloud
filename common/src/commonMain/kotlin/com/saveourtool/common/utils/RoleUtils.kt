/**
 * Utilities of Role entity
 */

package com.saveourtool.common.utils

import com.saveourtool.common.domain.Role

/**
 * @param oneRole
 * @param anotherRole
 * @return [Role] that has higher priority
 */
fun getHighestRole(oneRole: Role?, anotherRole: Role?): Role =
        listOf(oneRole ?: Role.NONE, anotherRole ?: Role.NONE).maxByOrNull { it.priority }!!
