package com.saveourtool.common.demo

import kotlinx.serialization.Serializable

private const val CONTACT_US_HTML_LINK = "<a href=\"https://github.com/saveourtool/save-cloud/issues/new\">contact us</a>"

/**
 * Enum that defines demo status
 * @property statusTooltip
 */
@Serializable
enum class DemoStatus(val statusTooltip: String) {
    /**
     * Demo is created but stopped by error
     */
    ERROR("Oops, some issue has occurred. Please, check logs and $CONTACT_US_HTML_LINK."),

    /**
     * No demo created yet
     */
    NOT_CREATED("Your demo is not created yet. Time to fix it!"),

    /**
     * Demo is ready for use
     */
    RUNNING("Demo is successfully started and ready to use!"),

    /**
     * Demo is already created but not ready for use yet
     */
    STARTING("We need some time to start your demo. Give us a moment and it's gonna be ready!"),

    /**
     * Demo is created but stopped by owner
     */
    STOPPED("Demo is created, but was stopped/not started yet. You can reconfigure it and start."),

    /**
     * Demo stop request is already sent, but container is not stopped yet.
     */
    STOPPING("Demo already requested to be stopped, but we need some time to stop it. Press the refresh button."),
    ;
}
