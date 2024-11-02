/**
 * All routs for the mobile version of the frontend
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "EXTENSION_FUNCTION_WITH_CLASS")

package com.saveourtool.cosv.frontend.routing

import com.saveourtool.common.validation.FrontendCosvRoutes.*
import com.saveourtool.common.validation.FrontendRoutes
import com.saveourtool.cosv.frontend.components.basic.CosvOrganizationType
import com.saveourtool.cosv.frontend.components.views.vuln.*
import com.saveourtool.cosv.frontend.components.views.vuln.toprating.topRatingView
import com.saveourtool.cosv.frontend.components.views.vuln.vulnerabilityCollectionView
import com.saveourtool.cosv.frontend.components.views.welcome.vulnWelcomeView
import com.saveourtool.frontend.common.components.views.FallbackView
import com.saveourtool.frontend.common.components.views.organization.createOrganizationView
import com.saveourtool.frontend.common.components.views.organization.organizationView
import com.saveourtool.frontend.common.components.views.registrationView
import com.saveourtool.frontend.common.components.views.thanksForRegistrationView
import com.saveourtool.frontend.common.components.views.userprofile.userProfileView
import com.saveourtool.frontend.common.components.views.usersettings.userSettingsView
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.withRouter

import org.w3c.dom.url.URLSearchParams
import react.*
import react.router.*

/**
 * Just put a map: View -> Route URL to this list
 */
val basicRouting: FC<UserInfoAwareMutablePropsWithChildren> = FC { props ->
    useUserStatusRedirects(props.userInfo?.status)

    val organizationView = withRouter { location, params ->
        organizationView {
            organizationName = params["owner"]!!
            currentUserInfo = props.userInfo
            organizationType = CosvOrganizationType
        }
    }

    val userProfileView = withRouter { _, params ->
        userProfileView {
            userName = params["name"]!!
            currentUserInfo = props.userInfo
        }
    }

    val vulnerabilityCollectionView = withRouter { location, _ ->
        vulnerabilityCollectionView {
            currentUserInfo = props.userInfo
            filter = URLSearchParams(location.search).toVulnerabilitiesFilter()
        }
    }

    val vulnerabilityView = withRouter { _, params ->
        vulnerabilityView {
            identifier = requireNotNull(params["identifier"])
            currentUserInfo = props.userInfo
        }
    }

    Routes {
        listOf(
            registrationView.create {
                userInfo = props.userInfo
                userInfoSetter = props.userInfoSetter
            } to REGISTRATION,
            vulnWelcomeView.create { userInfo = props.userInfo } to "/",
            FallbackView::class.react.create {
                bigText = "404"
                smallText = "Page not found"
                withRouterLink = true
            } to ERROR_404,
            vulnerabilityCollectionView.create() to "$VULN/list/:params?",
            createVulnerabilityView.create() to VULN_CREATE,
            uploadVulnerabilityView.create() to VULN_UPLOAD,
            vulnerabilityView.create() to "$VULNERABILITY_SINGLE/:identifier",
            cosvSchemaView.create() to VULN_COSV_SCHEMA,
            topRatingView.create() to VULN_TOP_RATING,

            createOrganizationView.create() to CREATE_ORGANIZATION,
            organizationView.create() to ":owner",
            userProfileView.create() to "$PROFILE/:name",
            thanksForRegistrationView.create() to THANKS_FOR_REGISTRATION,

            userSettingsView.create {
                this.userInfoSetter = props.userInfoSetter
                userInfo = props.userInfo
                type = FrontendRoutes.SETTINGS_PROFILE
            } to SETTINGS_PROFILE,

            userSettingsView.create {
                this.userInfoSetter = props.userInfoSetter
                userInfo = props.userInfo
                type = FrontendRoutes.SETTINGS_EMAIL
            } to SETTINGS_EMAIL,

            userSettingsView.create {
                userInfo = props.userInfo
                type = FrontendRoutes.SETTINGS_TOKEN
            } to SETTINGS_TOKEN,

            userSettingsView.create {
                userInfo = props.userInfo
                type = FrontendRoutes.SETTINGS_ORGANIZATIONS
            } to SETTINGS_ORGANIZATIONS,

            userSettingsView.create {
                userInfo = props.userInfo
                type = FrontendRoutes.SETTINGS_DELETE
            } to SETTINGS_DELETE,

        ).forEach { (view, route) ->
            PathRoute {
                this.element = view
                this.path = "/$route"
            }
        }

        props.userInfo?.name?.run {
            PathRoute {
                path = "/$this"
                element = Navigate.create { to = "/$this/$SETTINGS_PROFILE" }
            }
        }

        PathRoute {
            path = "*"
            element = FallbackView::class.react.create {
                bigText = "404"
                smallText = "Page not found"
                withRouterLink = true
            }
        }
    }
}

private val fallbackNode = FallbackView::class.react.create {
    bigText = "404"
    smallText = "Page not found"
    withRouterLink = false
}

/**
 * @param view
 * @return a view or a fallback of user info is null
 */
fun UserInfoAwareMutablePropsWithChildren.viewWithFallBack(view: ReactElement<*>) = this.userInfo?.name?.let {
    view
} ?: fallbackNode
