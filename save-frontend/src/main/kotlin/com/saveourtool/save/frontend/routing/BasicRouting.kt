/**
 * All routs for the mobile version of the frontend
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "EXTENSION_FUNCTION_WITH_CLASS")

package com.saveourtool.save.frontend.routing

import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.domain.TestResultStatus
import com.saveourtool.common.filters.TestExecutionFilter
import com.saveourtool.common.validation.FrontendRoutes.*
import com.saveourtool.frontend.common.components.views.AboutUsView
import com.saveourtool.frontend.common.components.views.FallbackView
import com.saveourtool.frontend.common.components.views.organization.createOrganizationView
import com.saveourtool.frontend.common.components.views.organization.organizationView
import com.saveourtool.frontend.common.components.views.registrationView
import com.saveourtool.frontend.common.components.views.thanksForRegistrationView
import com.saveourtool.frontend.common.components.views.userprofile.userProfileView
import com.saveourtool.frontend.common.components.views.usersettings.*
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.isSuperAdmin
import com.saveourtool.save.frontend.components.basic.projects.createProjectProblem
import com.saveourtool.save.frontend.components.basic.projects.projectProblem
import com.saveourtool.save.frontend.components.views.*
import com.saveourtool.save.frontend.components.views.agreements.cookieTermsOfUse
import com.saveourtool.save.frontend.components.views.agreements.termsOfUsageView
import com.saveourtool.save.frontend.components.views.contests.*
import com.saveourtool.save.frontend.components.views.demo.cpgView
import com.saveourtool.save.frontend.components.views.demo.demoCollectionView
import com.saveourtool.save.frontend.components.views.demo.demoView
import com.saveourtool.save.frontend.components.views.index.indexView
import com.saveourtool.save.frontend.components.views.projectcollection.CollectionView
import com.saveourtool.save.frontend.components.views.welcome.saveWelcomeView
import com.saveourtool.save.frontend.utils.SaveOrganizationType

import org.w3c.dom.url.URLSearchParams
import react.*
import react.router.*

/**
 * Just put a map: View -> Route URL to this list
 */
val basicRouting: FC<UserInfoAwareMutablePropsWithChildren> = FC { props ->
    useUserStatusRedirects(props.userInfo?.status)
    val userProfileView = withRouter { _, params ->
        userProfileView {
            userName = params["name"]!!
            currentUserInfo = props.userInfo
        }
    }

    val contestView = withRouter { location, params ->
        ContestView::class.react {
            currentUserInfo = props.userInfo
            currentContestName = params["contestName"]
            this.location = location
        }
    }

    val contestExecutionView = withRouter { _, params ->
        ContestExecutionView::class.react {
            currentUserInfo = props.userInfo
            contestName = params["contestName"]!!
            organizationName = params["organizationName"]!!
            projectName = params["projectName"]!!
        }
    }

    val projectView = withRouter { location, params ->
        ProjectView::class.react {
            name = params["name"]!!
            owner = params["owner"]!!
            currentUserInfo = props.userInfo
            this.location = location
        }
    }

    val historyView = withRouter { _, params ->
        HistoryView::class.react {
            name = params["name"]!!
            organizationName = params["owner"]!!
        }
    }

    val executionView = withRouter { location, params ->
        ExecutionView::class.react {
            organization = params["organization"]!!
            project = params["project"]!!
            executionId = params["executionId"]!!
            filters = web.url.URLSearchParams(location.search).let { params ->
                TestExecutionFilter(
                    status = params["status"]?.let { TestResultStatus.valueOf(it) },
                    fileName = params["fileName"],
                    testSuite = params["testSuite"],
                    tag = params["tag"]
                )
            }
            testAnalysisEnabled = true
        }
    }

    val organizationView = withRouter { location, params ->
        organizationView {
            organizationName = params["owner"]!!
            currentUserInfo = props.userInfo
            organizationType = SaveOrganizationType
        }
    }

    val awesomeBenchmarksView = withRouter { location, _ ->
        AwesomeBenchmarksView::class.react {
            this.location = location
        }
    }

    val contestGlobalRatingView = withRouter { location, _ ->
        ContestGlobalRatingView::class.react {
            organizationName = URLSearchParams(location.search).get("organizationName")
            projectName = URLSearchParams(location.search).get("projectName")
            this.location = location
        }
    }

    val contestTemplateView = withRouter { _, params ->
        contestTemplateView {
            id = requireNotNull(params["id"]).toLong()
            currentUserInfo = props.userInfo
        }
    }

    val demoView = withRouter { _, params ->
        demoView {
            projectCoordinates = ProjectCoordinates(
                requireNotNull(params["organizationName"]),
                requireNotNull(params["projectName"]),
            )
        }
    }

    val createProjectProblemView = withRouter { _, params ->
        createProjectProblem {
            organizationName = requireNotNull(params["owner"])
            projectName = requireNotNull(params["name"])
        }
    }

    val projectProblemView = withRouter { _, params ->
        projectProblem {
            organizationName = requireNotNull(params["owner"])
            projectName = requireNotNull(params["name"])
            projectProblemId = requireNotNull(params["id"]).toLong()
        }
    }

    Routes {
        listOf(
            indexView.create { userInfo = props.userInfo } to "/",
            saveWelcomeView.create { userInfo = props.userInfo } to SAVE,
            AboutUsView::class.react.create() to ABOUT_US,
            createOrganizationView.create() to CREATE_ORGANIZATION,
            registrationView.create {
                userInfo = props.userInfo
                userInfoSetter = props.userInfoSetter
            } to REGISTRATION,
            CollectionView::class.react.create { currentUserInfo = props.userInfo } to PROJECTS,
            contestListView.create { currentUserInfo = props.userInfo } to CONTESTS,

            FallbackView::class.react.create {
                bigText = "404"
                smallText = "Page not found"
                withRouterLink = true
            } to ERROR_404,
            banView.create { userInfo = props.userInfo } to BAN,
            contestGlobalRatingView.create() to CONTESTS_GLOBAL_RATING,
            contestView.create() to "$CONTESTS/:contestName",
            createContestTemplateView.create() to CREATE_CONTESTS_TEMPLATE,
            contestTemplateView.create() to "$CONTESTS_TEMPLATE/:id",
            contestExecutionView.create() to "$CONTESTS/:contestName/:organizationName/:projectName",
            awesomeBenchmarksView.create() to AWESOME_BENCHMARKS,
            createProjectView.create() to "$CREATE_PROJECT/:organization?",
            organizationView.create() to ":owner",
            historyView.create() to ":owner/:name/history",
            projectView.create() to ":owner/:name",
            createProjectProblemView.create() to "project/:owner/:name/security/problems/new",
            projectProblemView.create() to "project/:owner/:name/security/problems/:id",
            executionView.create() to ":organization/:project/history/execution/:executionId",
            demoView.create() to "$DEMO/:organizationName/:projectName",
            cpgView.create() to "$DEMO/cpg",
            testExecutionDetailsView.create() to "/:organization/:project/history/execution/:executionId/test/:testId",
            demoCollectionView.create() to DEMO,
            userProfileView.create() to "$PROFILE/:name",
            termsOfUsageView.create() to TERMS_OF_USE,
            cookieTermsOfUse.create() to COOKIE,
            thanksForRegistrationView.create() to THANKS_FOR_REGISTRATION,

            userSettingsView.create {
                this.userInfoSetter = props.userInfoSetter
                userInfo = props.userInfo
                type = SETTINGS_PROFILE
            } to SETTINGS_PROFILE,

            userSettingsView.create {
                this.userInfoSetter = props.userInfoSetter
                userInfo = props.userInfo
                type = SETTINGS_EMAIL
            } to SETTINGS_EMAIL,

            userSettingsView.create {
                userInfo = props.userInfo
                type = SETTINGS_TOKEN
            } to SETTINGS_TOKEN,

            userSettingsView.create {
                userInfo = props.userInfo
                type = SETTINGS_ORGANIZATIONS
            } to SETTINGS_ORGANIZATIONS,

            userSettingsView.create {
                userInfo = props.userInfo
                type = SETTINGS_DELETE
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
            path = "/$MANAGE_ORGANIZATIONS"
            element = when (props.userInfo.isSuperAdmin()) {
                true -> OrganizationAdminView::class.react.create()
                else -> fallbackNode
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
