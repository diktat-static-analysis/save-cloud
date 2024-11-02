@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")
@file:JsModule("react-json-view")
@file:JsNonModule

package com.saveourtool.frontend.common.externals.jsonview

import react.FC
import react.Props
import kotlin.js.Json

/**
 * External declaration of [reactJson] react component
 */
@JsName("default")
external val reactJson: FC<ReactJsonViewProps>

/**
 * Props of [ReactJsonViewProps]
 */
external interface ReactJsonViewProps : Props {
    /**
     * Input JSON
     */
    var src: Json

    /**
     * name : {} or no name in case of false
     */
    var name: Boolean

    /**
     * theme for the background
     */
    var theme: String
    // FixMe:   onAdd?: ((add: InteractionProps) => false | any) | false;
    // FixMe:   onEdit?: ((edit: InteractionProps) => false | any) | false;
    // FixMe:   onDelete?: ((del: InteractionProps) => false | any) | false;
}
