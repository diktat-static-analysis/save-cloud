/**
 * Component for managing files on project level
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.fileuploader

import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.entities.FileDto
import com.saveourtool.common.utils.toMegabytes
import com.saveourtool.frontend.common.components.basic.fileuploader.deleteFileButton
import com.saveourtool.frontend.common.components.basic.fileuploader.downloadFileButton
import com.saveourtool.frontend.common.components.inputform.dragAndDropForm
import com.saveourtool.frontend.common.http.postUploadFile
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.noopLoadingHandler

import js.core.asList
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import web.cssom.ClassName
import web.file.File

/**
 * [FC] for file uploading
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
    "ComplexMethod",
)
val fileManagerComponent: FC<FileManagerProps> = FC { props ->
    useTooltip()
    val (availableFiles, setAvailableFiles) = useState<List<FileDto>>(emptyList())
    val (storageBytes, setStorageBytes) = useState(0L)

    val (uploadBytesReceived, setUploadBytesReceived) = useState(0L)
    val (uploadBytesTotal, setUploadBytesTotal) = useState(0L)

    useRequest {
        val fileDtos: List<FileDto> = get(
            "$apiUrl/files/${props.projectCoordinates}/list",
            jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap { it.decodeFromJsonString() }
        setAvailableFiles(fileDtos)
    }

    useEffect(availableFiles) { setStorageBytes(availableFiles.sumOf { it.sizeBytes }) }

    val (fileToDelete, setFileToDelete) = useState<FileDto?>(null)
    val deleteFile = useDeferredRequest {
        fileToDelete?.let { file ->
            val response = delete(
                "$apiUrl/files/delete?fileId=${file.requiredId()}",
                jsonHeaders,
                loadingHandler = ::loadingHandler,
            )

            if (response.ok) {
                setAvailableFiles { it.minus(file) }
                setFileToDelete(null)
            }
        }
    }

    val (filesForUploading, setFilesForUploading) = useState<List<File>>(emptyList())
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    val uploadFiles = useDeferredRequest {
        filesForUploading.forEach { fileForUploading ->
            val uploadedFile: FileDto = postUploadFile(
                url = "$apiUrl/files/${props.projectCoordinates}/upload",
                file = fileForUploading,
                loadingHandler = ::noopLoadingHandler,
            )
                .decodeFromJsonString()
            setUploadBytesReceived { it.plus(uploadedFile.sizeBytes) }
            setAvailableFiles { it.plus(uploadedFile) }
        }
    }

    div {
        ul {
            className = ClassName("list-group shadow")
            @Suppress("MAGIC_NUMBER", "MagicNumber")
            li {
                val storageSizeMegabytes = storageBytes.toDouble().toMegabytes()
                // todo: storage size shouldn't be more then 500MB per project
                val textColor = when {
                    storageSizeMegabytes > 450 -> "text-danger"
                    storageSizeMegabytes > 350 -> "text-warning"
                    else -> "text-dark"
                }
                className = ClassName("list-group-item d-flex justify-content-center $textColor")
                +"Your storage is filled with ${storageSizeMegabytes.toFixedStr(2)} MB / 500MB."
            }
            // ===== SELECTED FILES =====
            availableFiles.map { file ->
                li {
                    className = ClassName("list-group-item")
                    downloadFileButton(file, FileDto::name) {
                        "$apiUrl/files/download?fileId=${it.requiredId()}"
                    }
                    deleteFileButton(file, FileDto::name) {
                        setFileToDelete(it)
                        deleteFile()
                    }

                    +file.prettyPrint()
                }
            }

            // ===== UPLOAD FILES BUTTON =====
            li {
                className = ClassName("list-group-item p-0 d-flex bg-light")
                dragAndDropForm {
                    isDisabled = false
                    isMultipleFilesSupported = true
                    tooltipMessage = "Regular files/Executable files/ZIP Archives"
                    onChangeEventHandler = { files ->
                        files!!.asList()
                            .also { fileList -> setUploadBytesTotal(fileList.sumOf { it.size }.toLong()) }
                            .let { setFilesForUploading(it) }
                        uploadFiles()
                    }
                }
            }
            progressBarComponent {
                current = uploadBytesReceived
                total = uploadBytesTotal
                flushCounters = {
                    setUploadBytesTotal(0)
                    setUploadBytesReceived(0)
                }
            }
        }
    }
}

/**
 * Props for [fileManagerComponent]
 */
external interface FileManagerProps : Props {
    /**
     * [ProjectCoordinates] of current project
     */
    var projectCoordinates: ProjectCoordinates
}
