@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
)

package com.saveourtool.frontend.common.components.views.organization

import com.saveourtool.common.domain.Role
import com.saveourtool.common.entities.OrganizationDto
import com.saveourtool.common.utils.AvatarType
import com.saveourtool.common.utils.CONTENT_LENGTH_CUSTOM
import com.saveourtool.common.utils.FILE_PART_NAME
import com.saveourtool.frontend.common.components.basic.AVATAR_ORGANIZATION_PLACEHOLDER
import com.saveourtool.frontend.common.components.basic.avatarForm
import com.saveourtool.frontend.common.components.views.usersettings.AVATAR_TITLE
import com.saveourtool.frontend.common.utils.*

import js.core.jso
import org.w3c.fetch.Headers
import react.FC
import react.Props
import react.StateSetter
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.label
import react.useState
import web.cssom.*
import web.file.File
import web.http.FormData

import kotlinx.browser.window

internal val renderOrganizationMenuBar: FC<RenderOrganizationMenuBarProps> = FC { props ->

    val (avatarFile, setAvatarFile) = useState<File?>(null)

    val fetchAvatarUpload = useDeferredRequest {
        avatarFile?.let { file ->
            val response = post(
                url = "$apiUrl/avatar/upload",
                params = jso<dynamic> {
                    owner = props.organizationName
                    this.type = AvatarType.ORGANIZATION
                },
                Headers().apply { append(CONTENT_LENGTH_CUSTOM, file.size.toString()) },
                FormData().apply { set(FILE_PART_NAME, file) },
                loadingHandler = ::noopLoadingHandler,
            )
            if (response.ok) {
                window.location.reload()
            }
        }
    }

    avatarForm {
        isOpen = props.isAvatarWindowOpen
        title = AVATAR_TITLE
        onCloseWindow = {
            props.setIsAvatarWindowOpen(true)
        }
        imageUpload = { file ->
            setAvatarFile(file)
            fetchAvatarUpload()
        }
    }

    div {
        className = ClassName("row d-flex")
        div {
            className = ClassName("col-3 ml-auto justify-content-center")
            style = jso {
                display = Display.flex
                alignItems = AlignItems.center
            }
            label {
                className = ClassName("btn")
                title = AVATAR_TITLE
                onClick = {
                    props.setIsAvatarWindowOpen(true)
                }
                img {
                    className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                    src = props.avatar
                    style = jso {
                        height = "10rem".unsafeCast<Height>()
                        width = "10rem".unsafeCast<Width>()
                    }
                    onError = {
                        props.setAvatar(AVATAR_ORGANIZATION_PLACEHOLDER)
                    }
                }
            }

            h1 {
                className = ClassName("h3 mb-0 text-gray-800 ml-2")
                +(props.organization?.name ?: "N/A")
            }
        }

        val listTabs = props.valuesOrganizationMenuBar.filter {
            // The Settings tab should only be seen by a person with the admin role or higher in this organization.
            // The Admin tab is seen only by super-admin
            (it != OrganizationMenuBar.SETTINGS || props.selfRole.isHigherOrEqualThan(Role.ADMIN)) &&
                    (it != OrganizationMenuBar.ADMIN || props.selfRole.isSuperAdmin())
        }

        div {
            className = ClassName("col-auto mx-0")
            tab(
                props.selectedMenu.name,
                listTabs.map { it.name },
                "nav nav-tabs mt-3"
            ) { value ->
                props.setSelectedMenu { OrganizationMenuBar.valueOf(value) }
            }
        }

        div {
            className = ClassName("col-3 mr-auto justify-content-center align-items-center")
        }
    }
}

/**
 * RenderOrganizationMenuBar component props
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface RenderOrganizationMenuBarProps : Props {
    var isAvatarWindowOpen: Boolean
    var setIsAvatarWindowOpen: StateSetter<Boolean>
    var avatar: String
    var setAvatar: StateSetter<String>
    var organization: OrganizationDto?
    var valuesOrganizationMenuBar: Array<OrganizationMenuBar>
    var selectedMenu: OrganizationMenuBar
    var setSelectedMenu: StateSetter<OrganizationMenuBar>
    var selfRole: Role
    var organizationName: String
}
