@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic.codeeditor

import com.saveourtool.common.utils.Languages
import com.saveourtool.save.frontend.externals.reactace.*

import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import web.cssom.ClassName

/**
 * Component with pure code editor
 */
val codeEditorComponent: FC<CodeEditorComponentProps> = FC { props ->
    div {
        props.editorTitle?.let { editorTitle ->
            h6 {
                className = ClassName("text-center text-primary")
                +editorTitle
            }
        }

        val additionalMarkers = if (props.aceMarkers != undefined) {
            props.aceMarkers
        } else {
            emptyArray()
        }

        aceBuilder(
            props.draftText,
            props.selectedMode,
            props.selectedTheme,
            getAceMarkers(props.savedText, props.draftText) + additionalMarkers,
            props.isDisabled,
        ) {
            props.onDraftTextUpdate(it)
        }
    }
}

/**
 * CodeEditor functional component [Props]
 */
external interface CodeEditorComponentProps : Props {
    /**
     * Title of an editor
     */
    var editorTitle: String?

    /**
     * Currently selected [AceThemes]
     */
    var selectedTheme: AceThemes

    /**
     * Currently selected [Languages]
     */
    var selectedMode: Languages

    /**
     * Text that is considered to be saved
     */
    var savedText: String

    /**
     * Text that is considered to be unsaved
     */
    var draftText: String

    /**
     * Update [draftText]
     */
    var onDraftTextUpdate: (String) -> Unit

    /**
     * Flag to disable form editing
     */
    var isDisabled: Boolean

    /**
     * Additional [AceMarkers]
     */
    var aceMarkers: AceMarkers
}
