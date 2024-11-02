package com.saveourtool.save.frontend.components.topbar

import com.saveourtool.save.frontend.externals.*
import com.saveourtool.common.info.UserInfo

import web.html.HTMLDivElement
import web.html.HTMLSpanElement
import react.*
import react.router.MemoryRouter

import kotlin.test.*
import js.core.jso

/**
 * [MemoryRouter] is used to enable usage of `useLocation` hook inside the component
 * todo: functionality that is not covered
 * * How breadcrumbs are displayed
 * * `/execution` is trimmed from breadcrumbs
 * * global role is displayed when present
 */
class TopBarTest {
    @Test
    fun topBarShouldRenderWithUserInfo() {
        val rr = render(
            MemoryRouter.create {
                initialEntries = arrayOf(
                    "/"
                )
                topBarComponent {
                    userInfo = UserInfo(name = "Test User")
                }
            }
        )

        val userInfoSpan: HTMLSpanElement? = screen.queryByTextAndCast("Test User")
        assertNotNull(userInfoSpan)

        // push the button, this weird test searches for the button that contains a name "Test User"
        // and "User setting" hint label
        val button = screen.getByRole("button", jso { name = "Test UserUser settings" })
        userEvent.click(button)
        val dropdown = rr.container.querySelector("[aria-labelledby=\"userDropdown\"]") as HTMLDivElement
        assertEquals(4, dropdown.children.length, "When user is logged in, dropdown menu should contain 3 entries")
    }

    @Test
    fun topBarShouldRenderWithoutUserInfo() {
        val rr = render(
            MemoryRouter.create {
                initialEntries = arrayOf(
                    "/"
                )
                topBarComponent {
                    userInfo = null
                }
            }
        )

        val userInfoSpan: HTMLSpanElement? = screen.queryByTextAndCast("Test User")
        assertNull(userInfoSpan)

        // push the button
        userEvent.click(rr.container.querySelector("[id=\"userDropdown\"]"))
        val dropdown = rr.container.querySelector("[aria-labelledby=\"userDropdown\"]") as HTMLDivElement
        assertEquals(1, dropdown.children.length, "When user is not logged in, dropdown menu should contain 1 entry")
    }
}
