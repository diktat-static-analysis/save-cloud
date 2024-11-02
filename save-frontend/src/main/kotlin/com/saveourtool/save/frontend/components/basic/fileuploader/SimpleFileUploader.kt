/**
 * Component for uploading files (FileDtos)
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.fileuploader

import com.saveourtool.common.entities.FileDto
import com.saveourtool.frontend.common.externals.fontawesome.*
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.noopLoadingHandler

import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import web.cssom.ClassName

@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
    "ComplexMethod",
)
val simpleFileUploader: FC<SimpleFileUploaderProps> = FC { props ->
    useTooltip()
    val (selectedFiles, setSelectedFiles) = useState<List<FileDto>>(emptyList())
    val (availableFiles, setAvailableFiles) = useState<List<FileDto>>(emptyList())

    useEffect(selectedFiles) { props.fileDtosSetter { selectedFiles } }

    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    useRequest {
        props.getUrlForSelectedFilesFetch?.let {
            val response = get(
                it(),
                jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )
            if (response.ok) {
                setSelectedFiles(response.decodeFromJsonString<List<FileDto>>())
            }
        }
    }

    useRequest(arrayOf(selectedFiles)) {
        props.getUrlForAvailableFilesFetch?.invoke()?.let { url ->
            val response = get(
                url,
                jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )
            if (response.ok) {
                val presentNames = selectedFiles.map { it.name }
                response.decodeFromJsonString<List<FileDto>>()
                    .let { fileDtos -> fileDtos.filter { fileDto -> fileDto.name !in presentNames }.distinctBy { it.name } }
                    .let(setAvailableFiles::invoke)
            }
        }
    }
    div {
        ul {
            className = ClassName("list-group")

            // ===== SELECTOR =====
            li {
                className = ClassName("list-group-item d-flex justify-content-between align-items-center")
                selectorBuilder(
                    "Select a file from existing",
                    availableFiles.map { it.name }.plus("Select a file from existing"),
                    classes = "form-control custom-select",
                    isDisabled = props.isDisabled,
                ) { event ->
                    val availableFile = availableFiles.first { it.name == event.target.value }
                    setSelectedFiles { it.plus(availableFile) }
                    setAvailableFiles { it.minus(availableFile) }
                }
            }

            // ===== SELECTED FILES =====
            selectedFiles.map { file ->
                li {
                    className = ClassName("list-group-item")
                    buttonBuilder(faTimes, null, isDisabled = props.isDisabled) {
                        setSelectedFiles { it.minus(file) }
                        setAvailableFiles { files -> files.plus(file) }
                    }
                    +file.name
                }
            }
        }
    }
}

typealias FileDtosSetter = StateSetter<List<FileDto>>

/**
 * Props for simpleFileUploader
 */
external interface SimpleFileUploaderProps : Props {
    /**
     * Callback to get url to get available files
     */
    var getUrlForAvailableFilesFetch: (() -> String)?

    /**
     * Callback to get url to get files that are already selected
     */
    var getUrlForSelectedFilesFetch: (() -> String)?

    /**
     * Callback to update list of selected file ids
     */
    var fileDtosSetter: FileDtosSetter

    /**
     * Flag that defines if the uploader is disabled
     */
    var isDisabled: Boolean
}
