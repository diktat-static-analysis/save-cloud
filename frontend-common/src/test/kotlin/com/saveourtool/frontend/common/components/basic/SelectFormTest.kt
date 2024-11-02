package com.saveourtool.frontend.common.components.basic

import com.saveourtool.common.entities.OrganizationDto
import com.saveourtool.common.v1
import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.externals.*
import com.saveourtool.frontend.common.externals.render
import com.saveourtool.frontend.common.externals.rest
import com.saveourtool.frontend.common.externals.screen
import com.saveourtool.frontend.common.externals.setupWorker
import com.saveourtool.frontend.common.utils.findByTextAndCast
import com.saveourtool.frontend.common.utils.mockMswResponse
import com.saveourtool.frontend.common.utils.wrapper
import web.html.HTMLDivElement
import web.html.HTMLOptionElement
import web.html.HTMLSelectElement
import react.*
import kotlin.js.Promise
import kotlin.test.*

class SelectFormTest {
    @Suppress("TYPE_ALIAS")
    private val selectFormRequired: FC<SelectFormRequiredProps<OrganizationDto>> = selectFormRequired()
    private fun createWorker() = setupWorker(
        rest.get("$apiUrl/organizations/get/list") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response, listOf(
                        OrganizationDto.empty.copy(name = "Test Organization 1"),
                        OrganizationDto.empty,
                        OrganizationDto.empty,
                    )
                )
            }
        }
    )

    @Test
    fun selectFormShouldShowOrganizations(): Promise<Unit> {
        val worker = createWorker()
        return (worker.start() as Promise<*>).then {
            render(
                wrapper.create {
                    selectFormRequired {
                        getData = { context ->
                            context.get(
                                "$apiUrl/organizations/get/list",
                                jsonHeaders,
                                loadingHandler = ::noopLoadingHandler,
                            )
                                .decodeFromJsonString()
                        }
                        dataToString = { it.name }
                        disabled = false
                        formType = InputTypes.ORGANIZATION_NAME
                        validInput = true
                        classes = "col-6 pl-0 pl-2 pr-2"
                        formName = "Organization"
                    }
                }
            )
        }
            .then {
                screen.findByTextAndCast<HTMLOptionElement>("Test Organization 1")
            }
            .then {
                val select = it.parentElement as HTMLSelectElement?
                assertNotNull(select, "`select` element should have been rendered")
                assertEquals(4, select.children.length, "Select should contain all organizations and an initial empty value")
            }
    }

    @Test
    fun componentShouldContainWarningIfNoOrganizations(): Promise<*> {
        val worker = createWorker()
        worker.use(
            rest.get("/api/$v1/organizations/get/list") { _, res, _ ->
                res { response ->
                    mockMswResponse(
                        response, emptyList<OrganizationDto>()
                    )
                }
            }
        )
        return (worker.start() as Promise<*>).then {
            render(
                wrapper.create {
                    selectFormRequired {
                        getData = { context ->
                            context.get(
                                "$apiUrl/organizations/get/list",
                                jsonHeaders,
                                loadingHandler = ::noopLoadingHandler,
                            )
                                .decodeFromJsonString()
                        }
                        dataToString = { it.name }
                        disabled = false
                        formType = InputTypes.ORGANIZATION_NAME
                        validInput = true
                        classes = "col-6 pl-0 pl-2 pr-2"
                        formName = "Organization"
                        notFoundErrorMessage = "You don't have access to any organizations"
                    }
                }
            )
        }
            .then {
                screen.findByTextAndCast<HTMLDivElement>("You don't have access to any organizations")
            }
            .then {
                assertNotNull(it, "Component should display a warning if no organizations are available")
            }
    }
}
