package com.saveourtool.frontend.common.utils

import react.StateInstance
import react.useState

/**
 * Util class which stores state about window openness
 *
 * @param isOpenState [StateInstance] with [Boolean] value
 */
class WindowOpenness(
    private val isOpenState: StateInstance<Boolean>
) {
    /**
     * @return current state of window
     */
    fun isOpen(): Boolean {
        val (isOpen, _) = isOpenState
        return isOpen
    }

    /**
     * Open window
     */
    fun openWindow() {
        setIsOpen(true)
    }

    /**
     * @return action to open window
     */
    fun openWindowAction(): () -> Unit = { openWindow() }

    /**
     * Close window
     */
    fun closeWindow() {
        setIsOpen(false)
    }

    /**
     * @return action to close window
     */
    fun closeWindowAction(): () -> Unit = { closeWindow() }

    private fun setIsOpen(value: Boolean) {
        val (_, setIsOpenState) = isOpenState
        setIsOpenState(value)
    }
}

/**
 * @return [WindowOpenness] with closed state by default
 */
fun useWindowOpenness() = WindowOpenness(useState(false))
