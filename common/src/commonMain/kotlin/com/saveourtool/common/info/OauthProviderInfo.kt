package com.saveourtool.common.info

import kotlin.js.JsExport
import kotlinx.serialization.Serializable

/**
 * Represents public information about OAuth2 provider
 *
 * @property registrationId name of the provider, corresponds to spring-security `registrationId` property
 * @property authorizationLink link that can be used to start authorization process
 */
@Serializable
@JsExport
data class OauthProviderInfo(
    val registrationId: String,
    val authorizationLink: String,
)
