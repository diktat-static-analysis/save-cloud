@file:Suppress("FILE_NAME_INCORRECT")

package com.saveourtool.frontend.common.utils

import js.core.jso
import kotlinext.js.assign
import react.*
import react.Fragment

/**
 * Base class that inherit [Component] class in order to provide [ChildrenBuilder] API in class components.
 */
@Suppress("CLASS_NAME_INCORRECT")
abstract class CComponent<P : Props, S : State> : Component<P, S> {
    constructor() : super() {
        state = jso { init() }
    }
    constructor(props: P) : super(props) {
        state = jso { init(props) }
    }

    @Suppress(
        "WRONG_OVERLOADING_FUNCTION_ARGUMENTS",
        "EMPTY_BLOCK_STRUCTURE_ERROR",
        "MISSING_KDOC_CLASS_ELEMENTS",
        "MISSING_KDOC_ON_FUNCTION",
    )
    open fun S.init() {}

    /**
     * @param props
     */
    @Suppress(
        "WRONG_OVERLOADING_FUNCTION_ARGUMENTS",
        "EMPTY_BLOCK_STRUCTURE_ERROR",
        "MISSING_KDOC_CLASS_ELEMENTS"
    )
    open fun S.init(props: P) {}

    /**
     *  Wrapper for convenient use of `ChildrenBuilder#render()`
     */
    override fun render(): ReactNode? = Fragment.create {
        render()
    }

    /**
     * Method that should be overridden in order to render the component
     */
    abstract fun ChildrenBuilder.render()

    /**
     * State setter
     *
     * @param stateSetter lambda to set a state
     */
    fun setState(stateSetter: S.() -> Unit) {
        super.setState({ assign(it, stateSetter) })
    }
}
