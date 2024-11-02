/**
 * This class contains methods for FileUploader
 */

package com.saveourtool.frontend.common.components.basic.fileuploader

import com.saveourtool.frontend.common.externals.fontawesome.faDownload
import com.saveourtool.frontend.common.externals.fontawesome.faTrash
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon

import react.ChildrenBuilder
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import web.cssom.ClassName
import web.html.ButtonType

import kotlinx.browser.window

/**
 * Creates a button to download a file
 *
 * @param file
 * @param getFileName
 * @param getDownloadUrl
 */
fun <F : Any> ChildrenBuilder.downloadFileButton(
    file: F,
    getFileName: (F) -> String,
    getDownloadUrl: (F) -> String,
) {
    a {
        button {
            type = ButtonType.button
            className = ClassName("btn")
            fontAwesomeIcon(icon = faDownload)
        }
        download = getFileName(file)
        href = getDownloadUrl(file)
    }
}

/**
 * Creates a button to delete a file
 *
 * @param file
 * @param getFileName
 * @param deleteFile
 */
fun <F : Any> ChildrenBuilder.deleteFileButton(
    file: F,
    getFileName: (F) -> String,
    deleteFile: (F) -> Unit,
) {
    button {
        type = ButtonType.button
        className = ClassName("btn")
        fontAwesomeIcon(icon = faTrash)
        onClick = {
            val confirm = window.confirm(
                "Are you sure you want to delete ${getFileName(file)} file?"
            )
            if (confirm) {
                deleteFile(file)
            }
        }
    }
}
