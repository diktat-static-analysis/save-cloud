/**
 * Kotlin/JS-React wrappers for react-modal library: JS definitions
 */

@file:Suppress("USE_DATA_CLASS")

package com.saveourtool.frontend.common.externals.modal

import react.*
import web.dom.Element
import web.html.HTMLDivElement
import web.html.HTMLElement

/**
 * [PropsWithChildren] of modal component
 */
external interface ModalProps : PropsWithChildren {
    /**
     * Boolean describing if the modal should be shown or not. Defaults to false.
     */
    var isOpen: Boolean

    /**
     * String indicating how the content container should be announced to screenreaders.
     */
    var contentLabel: String

    /**
     * String or object className to be applied to the modal content.
     */
    var className: Classes

    /**
     * String or object className to be applied to the overlay.
     */
    var overlayClassName: Classes

    /**
     *  Object indicating styles to be used for the modal, divided into overlay and content styles.
     */
    var style: Styles

    /**
     * Boolean indicating if the overlay should close the modal. Defaults to true.
     */
    var shouldCloseOnOverlayClick: Boolean?
}

/**
 * Styles of Modal component.
 *
 * @property content css styles for modal content
 * @property overlay css styles for modal overlay
 */
class Styles(
    @JsName("content") val content: CSSProperties? = undefined,
    @JsName("overlay") val overlay: CSSProperties? = undefined,
)

/**
 * The value corresponding to each key is a class name. Please note that specifying a CSS class
 * for the overlay or the content will disable the default styles for that component.
 *
 * @property base This class will always be applied to the component
 * @property afterOpen This class will be applied after the modal has been opened
 * @property beforeClose This class will be applied after the modal has requested to be closed
 * (e.g. when the user presses the escape key or clicks on the overlay).
 * Will have no effect unless the closeTimeoutMS prop is set to a non-zero value, since otherwise the modal will be closed immediately when requested.
 */
class Classes(
    var base: String,
    var afterOpen: String = "$base--after-open",
    var beforeClose: String = "$base--before-close",
)

/** Describes overlay and content element references passed to onAfterOpen function */
external interface OnAfterOpenCallbackOptions {
    /**
     * overlay element reference
     */
    var overlayEl: Element

    /**
     * content element reference
     */
    var contentEl: HTMLDivElement
}

/** Describes unction that will be run after the modal has opened */
external interface OnAfterOpenCallback {
    /**
     * @param obj
     */
    fun onAfterOpen(obj: OnAfterOpenCallbackOptions): Unit
}

/**
 * A wrapper component for a [portal](https://reactjs.org/docs/portals.html) which will contain this modal
 */
@JsModule("react-modal")
@JsNonModule
@JsName("ModalPortal")
external class ReactModalPortal : Component<PropsWithChildren, State> {
    /**
     * Content of the modal portal
     */
    val content: HTMLDivElement

    /**
     * Overlay (part that covers the background) of the modal portal
     */
    val overlay: HTMLDivElement

    override fun render(): ReactElement<*>?
}

/**
 * A main [Component] of react-modal.
 */
@JsModule("react-modal")
@JsNonModule
@JsName("ReactModal")
external class ReactModal : Component<ModalProps, State> {
    /**
     * A portal for modal window.
     */
    var portal: ReactModalPortal? = definedExternally

    override fun render(): ReactElement<ModalProps>?

    companion object {
        /**
         * Override base styles for all instances of this component.
         */
        val defaultStyles: Styles

        /**
         * Call this to properly hide your application from assistive screenreaders
         * and other assistive technologies while the modal is open.
         *
         * @param appElement an [HTMLElement] corresponding to app root
         */
        fun setAppElement(appElement: HTMLElement): Unit
    }
}
