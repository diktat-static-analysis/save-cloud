/**
 * This file contains util methods for CosvSchema
 */

package com.saveourtool.common.utils

import com.saveourtool.common.entities.vulnerability.*
import com.saveourtool.common.info.UserInfo
import com.saveourtool.osv4k.*
import com.saveourtool.osv4k.OsvSchema as CosvSchema

import kotlinx.datetime.LocalDateTime

private const val SAVEOURTOOL_PROFILE_PREFIX = "https://saveourtool.com/profile/"

private val timelineEntryTypeMapping = mapOf(
    VulnerabilityDateType.DISCLOSED to TimelineEntryType.disclosed,
    VulnerabilityDateType.FIXED to TimelineEntryType.fixed,
    VulnerabilityDateType.FOUND to TimelineEntryType.found,
    VulnerabilityDateType.INTRODUCED to TimelineEntryType.introduced,
)

val vulnerabilityPrefixes = listOf(
    "CVE-",
)

typealias ManualCosvSchema = CosvSchema<Unit, Unit, Unit, Unit>

/**
 * @return Save's contributors
 */
fun CosvSchema<*, *, *, *>.getSaveContributes(): List<UserInfo> = credits
    ?.mapNotNull { it.asSaveContribute() }
    .orEmpty()

/**
 * @return save's contributor
 */
fun Credit.asSaveContribute(): UserInfo? = contact
    ?.filter { it.startsWith(SAVEOURTOOL_PROFILE_PREFIX) }
    ?.map { it.removePrefix(SAVEOURTOOL_PROFILE_PREFIX) }
    ?.map { UserInfo(it) }
    ?.singleOrNull()

/**
 * @return [Credit]
 */
fun UserInfo.asCredit(): Credit = let {
    Credit(
        name = it.name,
        contact = listOf(
            SAVEOURTOOL_PROFILE_PREFIX + it.name
        ),
        type = CreditType.REPORTER,
    )
}

/**
 * @return list of [Credit]
 */
fun List<UserInfo>.asCredits(): List<Credit> = map { it.asCredit() }

/**
 * @return timeline as [List] of [VulnerabilityDateDto]
 */
fun CosvSchema<*, *, *, *>.getTimeline(): List<VulnerabilityDateDto> = buildList {
    timeline?.map { it.asVulnerabilityDateDto(id) }?.let { addAll(it) }
    published?.asVulnerabilityDateDto(id, VulnerabilityDateType.PUBLISHED)?.run { add(this) }
    withdrawn?.asVulnerabilityDateDto(id, VulnerabilityDateType.WITHDRAWN)?.run { add(this) }
}

/**
 * @return [TimelineEntry]
 */
fun VulnerabilityDateDto.asTimelineEntry(): TimelineEntry = timelineEntryTypeMapping[type]
    ?.let {
        TimelineEntry(
            value = date,
            type = it,
        )
    }
    ?: throw IllegalArgumentException("VulnerabilityDate $type cannot be saved in COSV timeline")

/**
 * @return language as [VulnerabilityLanguage]
 */
fun CosvSchema<*, *, *, *>.getLanguage(): VulnerabilityLanguage? = affected?.firstNotNullOfOrNull { affected ->
    affected.`package`?.language?.let { language ->
        VulnerabilityLanguage.values().firstOrNull { it.value == language }
    }
}

/**
 * @return relatedLink
 */
fun CosvSchema<*, *, *, *>.getRelatedLink(): String? = references
    ?.filter { it.type == ReferenceType.WEB }?.map { it.url }?.firstOrNull()

private fun LocalDateTime.asVulnerabilityDateDto(cosvId: String, type: VulnerabilityDateType) = VulnerabilityDateDto(
    date = this,
    type = type,
    vulnerabilityIdentifier = cosvId,
)

private fun TimelineEntry.asVulnerabilityDateDto(cosvId: String) = value.asVulnerabilityDateDto(cosvId,
    when (type) {
        TimelineEntryType.introduced -> VulnerabilityDateType.INTRODUCED
        TimelineEntryType.found -> VulnerabilityDateType.FOUND
        TimelineEntryType.fixed -> VulnerabilityDateType.FIXED
        TimelineEntryType.disclosed -> VulnerabilityDateType.DISCLOSED
    }
)

/**
 * Validation of [identifier]
 *
 * @param identifier
 * @return true if [identifier] is empty (our own should be set on backend)
 *   or starts with one of [vulnerabilityPrefixes] (reused existed identifier), false otherwise
 */
fun validateIdentifier(identifier: String) = identifier.isEmpty() || vulnerabilityPrefixes.any { identifier.startsWith(it) }
