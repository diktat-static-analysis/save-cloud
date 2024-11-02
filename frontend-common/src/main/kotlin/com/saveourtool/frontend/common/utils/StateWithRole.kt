package com.saveourtool.frontend.common.utils

import com.saveourtool.common.domain.Role
import react.State

/**
 * State with role of current user
 */
external interface StateWithRole : State {
    /**
     * Role of a user that is seeing this view
     */
    var selfRole: Role
}
