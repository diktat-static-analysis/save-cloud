@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")
@file:JsModule("react-diff-viewer-continued")
@file:JsNonModule

package com.saveourtool.frontend.common.externals.diffviewer

import react.FC
import react.Props

/**
 * External declaration of [reactDiffViewer] react component
 */
@JsName("default")
external val reactDiffViewer: FC<ReactDiffViewerProps>

/**
 * Props of [ReactDiffViewerProps]
 */
external interface ReactDiffViewerProps : Props {
    /**
     * Old value as string.
     */
    var oldValue: String

    /**
     * New value as string.
     */
    var newValue: String

    /**
     * Switch between unified and split view.
     */
    var splitView: Boolean

    /**
     * Show and hide word diff in a diff line.
     */
    var disableWordDiff: Boolean

    /**
     * Show and hide line numbers.
     */
    var hideLineNumbers: Boolean

    /**
     * Shows only the diffed lines and folds the unchanged lines
     */
    var showDiffOnly: Boolean

    /**
     * Number of extra unchanged lines surrounding the diff. Works along with [showDiffOnly].
     */
    var extraLinesSurroundingDiff: Int

    /**
     * To enable/disable dark theme.
     */
    var useDarkTheme: Boolean

    /**
     * Column title for left section of the diff in split view. This will be used as the only title in inline view.
     */
    var leftTitle: String

    /**
     * Column title for right section of the diff in split view. This will be ignored in inline view.
     */
    var rightTitle: String

    /**
     * Number to start count code lines from.
     */
    var linesOffset: Int
}
