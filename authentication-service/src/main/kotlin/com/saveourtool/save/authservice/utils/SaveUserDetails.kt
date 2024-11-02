package com.saveourtool.save.authservice.utils

import com.saveourtool.common.entities.User
import com.saveourtool.common.utils.*

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpHeaders
import org.springframework.security.core.CredentialsContainer
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken

/**
 * @property id [com.saveourtool.save.entities.User.id]
 * @property name [com.saveourtool.save.entities.User.name]
 * @property role [com.saveourtool.save.entities.User.role]
 * @property status [com.saveourtool.save.entities.User.status]
 * @property token [com.saveourtool.save.entities.User.password]
 */
class SaveUserDetails(
    val id: Long,
    val name: String,
    val role: String,
    val status: String,
    var token: String?,
) : UserDetails, CredentialsContainer {
    constructor(user: User) : this(
        user.requiredId(),
        user.name,
        user.role.orEmpty(),
        user.status.toString(),
        user.password,
    )

    /**
     * @return [PreAuthenticatedAuthenticationToken]
     */
    fun toPreAuthenticatedAuthenticationToken() =
            PreAuthenticatedAuthenticationToken(this, null, AuthorityUtils.commaSeparatedStringToAuthorityList(role))

    /**
     * Populates `X-Authorization-*` headers
     *
     * @param httpHeaders
     */
    fun populateHeaders(httpHeaders: HttpHeaders) {
        httpHeaders.set(AUTHORIZATION_ID, id.toString())
        httpHeaders.set(AUTHORIZATION_NAME, name)
        httpHeaders.set(AUTHORIZATION_ROLES, role)
        httpHeaders.set(AUTHORIZATION_STATUS, status)
    }

    @JsonIgnore
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = AuthorityUtils.commaSeparatedStringToAuthorityList(role)

    @JsonIgnore
    override fun getPassword(): String? = token

    @JsonIgnore
    override fun getUsername(): String = name

    @JsonIgnore
    override fun isAccountNonExpired(): Boolean = true

    @JsonIgnore
    override fun isAccountNonLocked(): Boolean = true

    @JsonIgnore
    override fun isCredentialsNonExpired(): Boolean = true

    @JsonIgnore
    override fun isEnabled(): Boolean = true

    override fun eraseCredentials() {
        token = null
    }

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val log = getLogger<SaveUserDetails>()

        /**
         * @return [SaveUserDetails] created from values in headers
         */
        fun HttpHeaders.toSaveUserDetails(): SaveUserDetails? {
            return SaveUserDetails(
                id = getSingleHeader(AUTHORIZATION_ID)?.toLong() ?: return logWarnAndReturnEmpty(AUTHORIZATION_ID),
                name = getSingleHeader(AUTHORIZATION_NAME) ?: return logWarnAndReturnEmpty(AUTHORIZATION_NAME),
                role = getSingleHeader(AUTHORIZATION_ROLES) ?: return logWarnAndReturnEmpty(AUTHORIZATION_ROLES),
                status = getSingleHeader(AUTHORIZATION_STATUS) ?: return logWarnAndReturnEmpty(AUTHORIZATION_STATUS),
                token = null,
            )
        }

        private fun HttpHeaders.getSingleHeader(headerName: String) = get(headerName)?.singleOrNull()

        private fun <T> logWarnAndReturnEmpty(missedHeaderName: String): T? {
            log.debug {
                "Header $missedHeaderName is not provided: skipping pre-authenticated save-user authentication"
            }
            return null
        }
    }
}
