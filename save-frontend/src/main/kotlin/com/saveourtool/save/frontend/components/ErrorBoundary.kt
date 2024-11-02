/**
 * Support for [error boundaries](https://reactjs.org/docs/error-boundaries.html)
 */

package com.saveourtool.save.frontend.components

import com.saveourtool.frontend.common.components.footer
import com.saveourtool.frontend.common.components.views.FallbackView
import com.saveourtool.save.frontend.components.topbar.topBarComponent

import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import web.cssom.ClassName

/**
 * State of error boundary component
 */
external interface ErrorBoundaryState : State {
    /**
     * The error message
     */
    var errorMessage: String?

    /**
     * True is there is an error in the wrapped component tree
     */
    var hasError: Boolean?
}

/**
 * Component to act as React Error Boundary
 */
class ErrorBoundary : Component<PropsWithChildren, ErrorBoundaryState>() {
    init {
        state = jso {
            errorMessage = null
            hasError = false
        }
    }

    override fun render(): ReactNode? = if (state.hasError == true) {
        FC {
            div {
                className = ClassName("container-fluid")
                topBarComponent()
                FallbackView::class.react {
                    bigText = "Error"
                    smallText = "Something went wrong: ${state.errorMessage ?: "Unknown error"}"
                }
                @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
                footer { }
            }
        }.create()
    } else {
        props.children
    }

    companion object : RStatics<PropsWithChildren, ErrorBoundaryState, ErrorBoundary, Nothing>(ErrorBoundary::class) {
        init {
            /*
             * From [React docs](https://reactjs.org/docs/error-boundaries.html):
             * 'A class component becomes an error boundary if it defines either (or both) of the lifecycle methods static getDerivedStateFromError() or componentDidCatch()'
             */
            getDerivedStateFromError = { ex ->
                jso {
                    errorMessage = ex.message
                    hasError = true
                }
            }
        }
    }
}
