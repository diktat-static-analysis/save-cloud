/**
 * Component for uploading files
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.domain.FileInfo
import org.cqfn.save.frontend.externals.fontawesome.faTimesCircle
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.frontend.utils.toPrettyString

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.PropsWithChildren
import react.dom.attrs
import react.dom.button
import react.dom.div
import react.dom.h6
import react.dom.input
import react.dom.label
import react.dom.li
import react.dom.option
import react.dom.select
import react.dom.strong
import react.dom.ul
import react.fc

import kotlinx.html.InputType
import kotlinx.html.hidden
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.cqfn.save.frontend.externals.fontawesome.faFile
import org.cqfn.save.frontend.externals.fontawesome.faUpload
import org.cqfn.save.frontend.externals.switch.rSwitch
import react.buildElement
import react.createElement
import react.dom.span

/**
 * Props for file uploader
 */
external interface UploaderProps : PropsWithChildren {
    /**
     * List of files available on server side
     */
    var availableFiles: List<FileInfo>

    /**
     * List of provided files
     */
    var files: List<FileInfo>
}

/**
 * @param onFileInput invoked on `input` change events
 * @param onFileSelect invoked when a file is selected from [UploaderProps.availableFiles]
 * @param onFileRemove invoked when a file is removed from selection by pushing a button
 * @return a RComponent
 */
@Suppress("TOO_LONG_FUNCTION")
fun fileUploader(
    onFileSelect: (HTMLSelectElement) -> Unit,
    onFileRemove: (FileInfo) -> Unit,
    onFileInput: (HTMLInputElement) -> Unit,
    onExecutableChange: (file: FileInfo, checked: Boolean) -> Unit,
) = fc<UploaderProps> { props ->
    div("mb-3") {
        h6(classes = "d-inline mr-3") {
            +"Select files:"
        }
        div {
            ul(classes = "list-group") {
                props.files.map { fileInfo ->
                    li(classes = "list-group-item") {
                        button(classes = "btn") {
                            fontAwesomeIcon {
                                attrs.icon = faTimesCircle
                            }
                            attrs.onClickFunction = {
                                onFileRemove(fileInfo)
                            }
                        }
                        rSwitch {
                            attrs.checked = fileInfo.isExecutable
                            attrs.onChange = { checked, _, _ ->
                                onExecutableChange(fileInfo, checked)
                            }
                            attrs.uncheckedIcon = buildElement {
                                span("fa-layers") {
                                    fontAwesomeIcon(icon = faFile)
                                }
                            }
                            attrs.checkedIcon = buildElement {
                                span("fa-layers") {
                                    fontAwesomeIcon(icon = faFile)
                                    span("fa-layers-text file-extension fa-inverse") {
                                        +"exe"
                                    }
                                }
                            }
                        }
                        +fileInfo.toPrettyString()
                    }
                }
                li("list-group-item d-flex justify-content-between align-items-center") {
                    select(classes = "form-control") {
                        attrs.value = "default"
                        option {
                            attrs.value = "default"
                            attrs.disabled = true
                            +"Select a file from existing"
                        }
                        props.availableFiles.sortedByDescending { it.uploadedMillis }.map {
                            option("list-group-item") {
                                attrs.value = it.name
                                +it.toPrettyString()
                            }
                        }
                        attrs.onChangeFunction = {
                            onFileSelect(it.target as HTMLSelectElement)
                        }
                    }
                }
                li("list-group-item d-flex justify-content-between align-items-center") {
                    label {
                        input(type = InputType.file) {
                            attrs.multiple = true
                            attrs.hidden = true
                            attrs {
                                onChangeFunction = { event ->
                                    val target = event.target as HTMLInputElement
                                    onFileInput(target)
                                }
                            }
                        }
                        fontAwesomeIcon(icon = faUpload)
                        strong { +"Upload files:" }
                    }
                }
            }
        }
    }
}
