@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic.codeeditor

import com.saveourtool.common.utils.Languages
import com.saveourtool.frontend.common.components.basic.cardComponent
import com.saveourtool.frontend.common.externals.fontawesome.*
import com.saveourtool.frontend.common.utils.buttonBuilder
import com.saveourtool.frontend.common.utils.selectorBuilder
import com.saveourtool.save.frontend.externals.reactace.AceThemes

import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import web.cssom.ClassName

private val toolbarCard = cardComponent(isBordered = true)

/**
 * @param selectedMode
 * @param selectedTheme
 * @param selectedFileType
 * @param hasUncommittedChanges
 * @param setSelectedMode
 * @param setSelectedTheme
 * @param onUploadChanges
 * @param onReloadChanges
 * @param onResultReload
 * @param onRunExecution
 * @param onFileTypeChange
 */
@Suppress(
    "TOO_MANY_PARAMETERS",
    "LongParameterList",
    "TOO_LONG_FUNCTION",
    "LongMethod",
)
fun ChildrenBuilder.displayCodeEditorToolbar(
    selectedMode: Languages,
    selectedTheme: AceThemes,
    selectedFileType: FileType?,
    hasUncommittedChanges: Map<FileType, Boolean>,
    setSelectedMode: (String) -> Unit,
    setSelectedTheme: (String) -> Unit,
    onUploadChanges: () -> Unit,
    onReloadChanges: () -> Unit,
    onResultReload: () -> Unit,
    onRunExecution: () -> Unit,
    onFileTypeChange: (FileType) -> Unit,
) {
    toolbarCard {
        div {
            className = ClassName("input-group")
            div {
                className = ClassName("input-group-prepend")
                buttonBuilder(faUpload, isOutline = true, title = "Save your code snippet on the server") {
                    onUploadChanges()
                }
                buttonBuilder(faDownload, isOutline = true, title = "Download your previously saved snippet from the server") {
                    onReloadChanges()
                }
                FileType.values().forEach { fileType ->
                    val buttonStyle = if (hasUncommittedChanges.getValue(fileType)) {
                        "warning"
                    } else {
                        "primary"
                    }
                    buttonBuilder(
                        fileType.fileName,
                        buttonStyle,
                        isOutline = true,
                        isActive = selectedFileType == fileType,
                    ) {
                        onFileTypeChange(fileType)
                    }
                }
            }
            selectorBuilder(
                selectedFileType?.editorMode?.modeName ?: selectedMode.modeName,
                Languages.values().map { it.modeName },
                "custom-select",
                selectedFileType?.editorMode != null,
            ) { event ->
                setSelectedMode(event.target.value)
            }
            selectorBuilder(
                selectedTheme.themeName,
                AceThemes.values().map { it.themeName },
                "custom-select",
                false,
            ) { event ->
                setSelectedTheme(event.target.value)
            }
            div {
                className = ClassName("input-group-append")
                buttonBuilder(faReload, style = "info", isOutline = true, title = "Fetch debug info") {
                    onResultReload()
                }
                buttonBuilder(faCaretSquareRight, style = "success", isOutline = true, title = "Run execution") {
                    onRunExecution()
                }
            }
        }
    }
}
