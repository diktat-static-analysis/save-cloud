package com.saveourtool.frontend.common.externals.fontawesome

import react.PropsWithChildren

/**
 * Props of [FontAwesomeIcon]
 */
external interface FontAwesomeIconProps : PropsWithChildren {
    /**
     * Icon. Can be an object, string or array.
     */
    var icon: dynamic

    /**
     * Classes of the element
     */
    var className: String

    /**
     * Color of the element
     */
    var color: String

    /**
     * Icon size, can be t-shirt size (2xs to 2lx) and x-factor size (1x to 10x)
     *
     * @see <a href=https://fontawesome.com/docs/web/use-with/react/style#size>size docs</a>
     */
    var size: String?
}
