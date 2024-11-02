package com.saveourtool.frontend.common.components.views

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.*
import com.saveourtool.common.info.UserInfo
import com.saveourtool.frontend.common.components.views.organization.OrganizationMenuBar
import com.saveourtool.frontend.common.components.views.organization.OrganizationType
import com.saveourtool.frontend.common.components.views.organization.organizationView
import com.saveourtool.frontend.common.externals.*
import com.saveourtool.frontend.common.utils.apiUrl
import com.saveourtool.frontend.common.utils.mockMswResponse
import com.saveourtool.frontend.common.utils.wrapper
import kotlinx.datetime.LocalDateTime

import react.create

import kotlin.js.Promise
import kotlin.test.*
import js.core.jso

class OrganizationViewTest {
    private val testOrganization = OrganizationDto.empty
        .copy(
            name = "TestOrg",
            dateCreated = LocalDateTime(2022, 6, 1, 12, 25),
        )
    private val testUserInfo = UserInfo(
        "TestUser",
        projects = emptyMap(),
        organizations = mapOf(testOrganization.name to Role.ADMIN),
        globalRole = Role.SUPER_ADMIN,
    )

    @Suppress("TOO_LONG_FUNCTION")
    private fun createWorker() = setupWorker(
        rest.get("$apiUrl/organizations/${testOrganization.name}") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    testOrganization,
                )
            }
        },
        rest.post("$apiUrl/projects/by-filters") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    arrayListOf<ProjectDto>()
                )
            }
        },
        rest.get("$apiUrl/organizations/${testOrganization.name}/users/roles") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    testUserInfo.organizations[testOrganization.name],
                )
            }
        },
        rest.get("$apiUrl/organizations/${testOrganization.name}/users") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    listOf(testUserInfo),
                )
            }
        },
    )

    @Test
    fun shouldShowConfirmationWindowWhenDeletingOrganization(): Promise<*> {
        val worker = createWorker()
        return (worker.start() as Promise<*>).then {
            renderOrganizationView()
        }
            .then {
                screen.findByText(
                    "SETTINGS",
                    waitForOptions = jso {
                        timeout = 15000
                    },
                )
            }
            .then {
                userEvent.click(it)
            }
            .then { _: Unit ->
                screen.findByText("Delete ${testOrganization.name}")
            }
            .then {
                userEvent.click(it)
            }
            .then { _: Unit ->
                screen.findByText("Yes, delete ${testOrganization.name}")
            }
            .then {
                assertNotNull(it, "Should show confirmation window")
            }
            .then {
                worker.stop()
            }
    }

    private fun renderOrganizationView(userInfo: UserInfo = testUserInfo) = wrapper.create {
        organizationView {
            organizationName = testOrganization.name
            currentUserInfo = userInfo
            organizationType = TestOrganizationType
        }
    }
        .let {
            render(it)
        }
}

object TestOrganizationType : OrganizationType {
    override val listTab: Array<OrganizationMenuBar> = arrayOf(
        OrganizationMenuBar.INFO,
        OrganizationMenuBar.TOOLS,
        OrganizationMenuBar.BENCHMARKS,
        OrganizationMenuBar.CONTESTS,
        OrganizationMenuBar.SETTINGS,
        OrganizationMenuBar.ADMIN,
    )
}
