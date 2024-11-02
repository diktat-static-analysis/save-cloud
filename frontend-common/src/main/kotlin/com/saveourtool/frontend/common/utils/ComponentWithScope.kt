package com.saveourtool.frontend.common.utils

import react.Props
import react.State

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

/**
 * Base class for react components with CoroutineScope, that will be cancelled on unmounting.
 */
abstract class ComponentWithScope<P : Props, S : State> : CComponent<P, S>() {
    /**
     * A [CoroutineScope] that should be used by implementing classes. Will be cancelled on unmounting.
     */
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    override fun componentWillUnmount() {
        if (scope.isActive) {
            scope.cancel()
        }
    }
}
