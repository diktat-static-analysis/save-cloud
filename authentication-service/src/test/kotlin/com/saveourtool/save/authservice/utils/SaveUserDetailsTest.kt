package com.saveourtool.save.authservice.utils

import com.saveourtool.save.authservice.utils.SaveUserDetails.Companion.toSaveUserDetails
import com.saveourtool.common.utils.AUTHORIZATION_ID
import com.saveourtool.common.utils.AUTHORIZATION_NAME
import com.saveourtool.common.utils.AUTHORIZATION_ROLES
import com.saveourtool.common.utils.AUTHORIZATION_STATUS
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders

class SaveUserDetailsTest {
    @Test
    fun toSaveUserDetailsValid() {
        val httpHeaders = HttpHeaders()
        httpHeaders[AUTHORIZATION_ID] = "123"
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"
        httpHeaders[AUTHORIZATION_STATUS] = "ACTIVE"
        val result = httpHeaders.toSaveUserDetails()

        Assertions.assertNotNull(result)
        Assertions.assertEquals(123, result?.id)
        Assertions.assertEquals("name", result?.name)
        Assertions.assertEquals("ROLE", result?.role)
        Assertions.assertEquals("ACTIVE", result?.status)
    }

    @Test
    fun toSaveUserDetailsDuplicate() {
        val httpHeaders = HttpHeaders()
        httpHeaders[AUTHORIZATION_ID] = listOf("123", "321")
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"
        httpHeaders[AUTHORIZATION_STATUS] = "ACTIVE"

        Assertions.assertNull(httpHeaders.toSaveUserDetails())
    }

    @Test
    fun toSaveUserDetailsMissed() {
        val httpHeaders = HttpHeaders()
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"
        httpHeaders[AUTHORIZATION_STATUS] = "ACTIVE"

        Assertions.assertNull(httpHeaders.toSaveUserDetails())
    }

    @Test
    fun toSaveUserDetailsInvalid() {
        val httpHeaders = HttpHeaders()
        httpHeaders[AUTHORIZATION_ID] = "not_integer"
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"
        httpHeaders[AUTHORIZATION_STATUS] = "ACTIVE"

        assertThrows<NumberFormatException> {
            httpHeaders.toSaveUserDetails()
        }
    }

    @Test
    fun populateHeaders() {
        val SaveUserDetails = SaveUserDetails(
            id = 123,
            name = "name",
            role = "ROLE",
            status = "ACTIVE",
            token = "N/A",
        )
        val httpHeaders = HttpHeaders()
        SaveUserDetails.populateHeaders(httpHeaders)

        Assertions.assertEquals(listOf("123"), httpHeaders[AUTHORIZATION_ID])
        Assertions.assertEquals(listOf("name"), httpHeaders[AUTHORIZATION_NAME])
        Assertions.assertEquals(listOf("ROLE"), httpHeaders[AUTHORIZATION_ROLES])
        Assertions.assertEquals(listOf("ACTIVE"), httpHeaders[AUTHORIZATION_STATUS])
        Assertions.assertEquals(
            setOf(AUTHORIZATION_ID, AUTHORIZATION_NAME, AUTHORIZATION_ROLES, AUTHORIZATION_STATUS),
            httpHeaders.keys,
        )
    }
}
