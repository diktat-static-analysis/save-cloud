package com.saveourtool.frontend.common.components.basic.testsuitespermissions

/**
 * Enum class that defines current state of [manageTestSuitePermissionsComponent] (mostly state of the modal inside component)
 * @property title
 * @property purpose
 */
enum class PermissionManagerMode(val title: String? = null, val purpose: String? = null) {
    /**
     * State when success (or error) message is shown.
     */
    MESSAGE,

    /**
     * Make test suites public or private
     */
    PUBLISH(
        title = "Visibility mode",
        purpose = "Make test suites private or public",
    ),

    /**
     * Select test suites that should be managed in case of visibility.
     */
    SUITE_SELECTOR_FOR_PUBLISH,

    /**
     * Select test suites that should be managed in case of rights.
     */
    SUITE_SELECTOR_FOR_RIGHTS,

    /**
     * State when a modal with three input forms is shown: what, where and how to add.
     */
    TRANSFER(
        title = "Transfer mode",
        purpose = "Share test suites with selected organization",
    ),
    ;
}
