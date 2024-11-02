/**
 * Buttons of demo management card
 */

package com.saveourtool.save.frontend.components.basic.demo.management

import com.saveourtool.common.demo.DemoStatus
import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.domain.Role
import com.saveourtool.frontend.common.utils.buttonBuilder

import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import web.cssom.ClassName

/**
 * Display buttons of demo management card
 *
 * @param projectCoordinates saveourtool [ProjectCoordinates]
 * @param demoStatus [DemoStatus] of this demo
 * @param userRole current user role, required for button disabling
 * @param createOrUpdateDemo callback that sends creation request to backend
 * @param refreshDemo callback that fetches [DemoStatus] of this demo
 * @param startDemo callback that sends request to start demo pod
 * @param stopDemo callback that sends request to stop demo pod
 * @param deleteDemo callback that sends request to delete created demo
 */
@Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
internal fun ChildrenBuilder.renderButtons(
    projectCoordinates: ProjectCoordinates,
    demoStatus: DemoStatus,
    userRole: Role,
    createOrUpdateDemo: () -> Unit,
    refreshDemo: () -> Unit,
    startDemo: () -> Unit,
    stopDemo: () -> Unit,
    deleteDemo: () -> Unit,
) {
    div {
        className = ClassName("flex-wrap d-flex justify-content-around")
        when (demoStatus) {
            DemoStatus.NOT_CREATED -> buttonBuilder("Create", isDisabled = userRole.isLowerThan(Role.OWNER)) {
                createOrUpdateDemo()
            }

            DemoStatus.STARTING -> {
                buttonBuilder("Stop", style = "warning", isDisabled = userRole.isLowerThan(Role.ADMIN)) {
                    stopDemo()
                }
                buttonBuilder("Reload", style = "secondary", isDisabled = userRole.isLowerThan(Role.VIEWER)) {
                    refreshDemo()
                }
            }

            DemoStatus.RUNNING -> {
                buttonBuilder("Stop", style = "warning", isDisabled = userRole.isLowerThan(Role.ADMIN)) {
                    stopDemo()
                }
                buttonBuilder("Delete", style = "danger", isDisabled = userRole.isLowerThan(Role.OWNER)) {
                    deleteDemo()
                }
            }

            DemoStatus.ERROR -> {
                buttonBuilder("Run", isDisabled = userRole.isLowerThan(Role.ADMIN)) {
                    startDemo()
                }
                buttonBuilder("Delete", style = "danger", isDisabled = userRole.isLowerThan(Role.OWNER)) {
                    deleteDemo()
                }
            }

            DemoStatus.STOPPED -> {
                buttonBuilder("Run", isDisabled = userRole.isLowerThan(Role.ADMIN)) {
                    startDemo()
                }
                buttonBuilder("Update configuration", style = "info", isDisabled = userRole.isLowerThan(Role.ADMIN)) {
                    createOrUpdateDemo()
                }
                buttonBuilder("Delete", style = "danger", isDisabled = userRole.isLowerThan(Role.OWNER)) {
                    deleteDemo()
                }
            }

            DemoStatus.STOPPING -> buttonBuilder("Reload", style = "secondary", isDisabled = userRole.isLowerThan(Role.VIEWER)) { refreshDemo() }
        }
        demoContainerLogButton {
            this.projectCoordinates = projectCoordinates
            isDisabled = demoStatus == DemoStatus.NOT_CREATED
        }
    }
}
