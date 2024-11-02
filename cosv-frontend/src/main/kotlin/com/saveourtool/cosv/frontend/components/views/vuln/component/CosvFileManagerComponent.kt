@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.cosv.frontend.components.views.vuln.component

import com.saveourtool.common.entities.OrganizationDto
import com.saveourtool.common.entities.cosv.RawCosvFileDto
import com.saveourtool.common.entities.cosv.RawCosvFileDto.Companion.isDuplicate
import com.saveourtool.common.entities.cosv.RawCosvFileDto.Companion.isHasErrors
import com.saveourtool.common.entities.cosv.RawCosvFileDto.Companion.isPendingRemoved
import com.saveourtool.common.entities.cosv.RawCosvFileDto.Companion.isProcessing
import com.saveourtool.common.entities.cosv.RawCosvFileDto.Companion.isUploadedJsonFile
import com.saveourtool.common.entities.cosv.RawCosvFileDto.Companion.isZipArchive
import com.saveourtool.common.entities.cosv.RawCosvFileStatisticsDto
import com.saveourtool.common.entities.cosv.RawCosvFileStreamingResponse
import com.saveourtool.common.utils.FILE_PART_NAME
import com.saveourtool.common.utils.toKilobytes
import com.saveourtool.common.validation.isValidName
import com.saveourtool.frontend.common.components.basic.fileuploader.defaultProgressBarComponent
import com.saveourtool.frontend.common.components.basic.fileuploader.deleteFileButton
import com.saveourtool.frontend.common.components.basic.fileuploader.downloadFileButton
import com.saveourtool.frontend.common.components.basic.selectFormRequired
import com.saveourtool.frontend.common.components.inputform.InputTypes
import com.saveourtool.frontend.common.components.inputform.dragAndDropForm
import com.saveourtool.frontend.common.externals.fontawesome.faBoxOpen
import com.saveourtool.frontend.common.externals.fontawesome.faReload
import com.saveourtool.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.frontend.common.externals.i18next.useTranslation
import com.saveourtool.frontend.common.utils.*

import js.core.asList
import js.core.jso
import org.w3c.fetch.Headers
import react.FC
import react.Props
import react.dom.html.ReactHTML.b
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.ul
import react.useState
import web.cssom.ClassName
import web.file.File
import web.html.ButtonType
import web.http.FormData

import kotlinx.browser.window
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.serialization.json.Json

private const val DEFAULT_SIZE = 10

val cosvFileManagerComponent: FC<Props> = FC {
    useTooltip()
    val (t) = useTranslation("vulnerability-upload")

    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    val organizationSelectForm = selectFormRequired<String>()

    val (statistics, setStatistics) = useState(RawCosvFileStatisticsDto.empty)
    val (lastPage, setLastPage) = useState(0)
    val (availableFiles, setAvailableFiles) = useState<List<RawCosvFileDto>>(emptyList())
    val (filesForUploading, setFilesForUploading) = useState<List<File>>(emptyList())

    val leftAvailableFilesCount = statistics.allAvailableFilesCount - lastPage * DEFAULT_SIZE

    val (userOrganizations, setUserOrganizations) = useState(emptyList<OrganizationDto>())
    val (selectedOrganization, setSelectedOrganization) = useState<String>()

    val (fileToDelete, setFileToDelete) = useState<RawCosvFileDto>()
    val (fileToUnzip, setFileToUnzip) = useState<RawCosvFileDto>()

    val (currentProgress, setCurrentProgress) = useState(-1)
    val (currentProgressMessage, setCurrentProgressMessage) = useState("")
    val resetCurrentProgress = {
        setCurrentProgress(-1)
        setCurrentProgressMessage("")
    }

    val (isStreamingOperationActive, setStreamingOperationActive) = useState(false)

    val deleteFile = useDeferredRequest {
        fileToDelete?.let { file ->
            val response = delete(
                "$apiUrl/raw-cosv/$selectedOrganization/delete/${file.requiredId()}",
                headers = Headers().withAcceptJson(),
                loadingHandler = ::noopLoadingHandler,
            )

            if (response.ok) {
                setAvailableFiles { it.minus(file) }
                setStatistics { it.copy(allAvailableFilesCount = statistics.allAvailableFilesCount.dec()) }
                when {
                    file.isZipArchive() -> setStatistics { it.copy(uploadedArchivesCount = statistics.uploadedArchivesCount.dec()) }
                    file.isUploadedJsonFile() -> setStatistics { it.copy(uploadedJsonFilesCount = statistics.uploadedJsonFilesCount.dec()) }
                    file.isProcessing() -> setStatistics { it.copy(processingFilesCount = statistics.processingFilesCount.dec()) }
                    file.isPendingRemoved() -> setStatistics { it.copy(pendingRemovedFilesCount = statistics.pendingRemovedFilesCount.dec()) }
                    file.isDuplicate() -> setStatistics { it.copy(duplicateFilesCount = statistics.duplicateFilesCount.dec()) }
                    file.isHasErrors() -> setStatistics { it.copy(errorFilesCount = statistics.errorFilesCount.dec()) }
                }
                setFileToDelete(null)
            } else {
                window.alert("Failed to delete file due to ${response.unpackMessageOrHttpStatus()}")
            }
        }
    }

    useRequest {
        val organizations = get(
            url = "$apiUrl/organizations/with-allow-bulk-upload",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<List<OrganizationDto>>()
            }

        setUserOrganizations(organizations)
    }

    val getStatistics = useDeferredRequest {
        selectedOrganization?.let {
            val response = get(
                url = "$apiUrl/raw-cosv/$selectedOrganization/statistics",
                jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler
            )
            when {
                response.ok -> setStatistics(response.unsafeMap { it.decodeFromJsonString<RawCosvFileStatisticsDto>() })
                else -> window.alert("Failed to get statistics data: ${response.unpackMessageOrNull().orEmpty()}")
            }
        }
    }

    val fetchMoreFiles = useDeferredRequest {
        selectedOrganization?.let {
            getStatistics()
            val newPage = lastPage.inc()
            val response = get(
                url = "$apiUrl/raw-cosv/$selectedOrganization/list",
                params = jso<dynamic> {
                    page = newPage - 1
                    size = DEFAULT_SIZE
                },
                headers = Headers().withAcceptNdjson().withContentTypeJson(),
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler
            )
            when {
                response.ok -> {
                    setStreamingOperationActive(true)
                    response
                        .readLines()
                        .filter(String::isNotEmpty)
                        .onCompletion {
                            setStreamingOperationActive(false)
                            setLastPage(newPage)
                        }
                        .collect { message ->
                            val uploadedFile: RawCosvFileDto = Json.decodeFromString(message)
                            setAvailableFiles { it.plus(uploadedFile) }
                        }
                }
                else -> window.alert("Failed to fetch next page: ${response.unpackMessageOrNull().orEmpty()}")
            }
        }
    }

    val reFetchFiles = useDeferredRequest {
        selectedOrganization?.let {
            setAvailableFiles(emptyList())
            setLastPage(0)
            fetchMoreFiles()
        }
    }

    val deleteAllDuplicatedCosvFiles = useDeferredRequest {
        val response = delete(
            url = "$apiUrl/raw-cosv/$selectedOrganization/delete-all-duplicated-files",
            jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler
        )
        when {
            response.ok -> reFetchFiles()
            else -> window.alert("Failed to delete duplicated files")
        }
    }

    var processedBytes by useState(0L)
    val uploadFiles = useDeferredRequest {
        setStreamingOperationActive(true)
        setCurrentProgress(0)
        setCurrentProgressMessage("Initializing...")
        val totalBytes = filesForUploading.sumOf { it.size.toLong() }
        val response = post(
            url = "$apiUrl/raw-cosv/$selectedOrganization/batch-upload",
            headers = Headers().withAcceptNdjson(),
            body = FormData().apply { filesForUploading.forEach { append(FILE_PART_NAME, it) } },
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        )
        when {
            response.ok -> response
                .readLines()
                .filter(String::isNotEmpty)
                .onCompletion {
                    setStreamingOperationActive(false)
                    reFetchFiles()
                }
                .collect { message ->
                    val uploadedFile: RawCosvFileDto = Json.decodeFromString(message)
                    processedBytes += uploadedFile.requiredContentLength()
                    if (processedBytes == totalBytes) {
                        setCurrentProgress(((processedBytes / totalBytes) * 100).toInt())
                        setCurrentProgressMessage("${processedBytes.toKilobytes()} / ${totalBytes.toKilobytes()} KB")
                    } else {
                        setCurrentProgress(100)
                        setCurrentProgressMessage("Successfully uploaded ${totalBytes.toKilobytes()} KB.")
                    }
                }
            else -> {
                setStreamingOperationActive(false)
                resetCurrentProgress()
                window.alert(response.unpackMessageOrNull().orEmpty())
            }
        }
    }

    val unzipFile = useDeferredRequest {
        fileToUnzip?.let { file ->
            setStreamingOperationActive(true)
            val response = post(
                "$apiUrl/raw-cosv/$selectedOrganization/unzip/${file.requiredId()}",
                headers = Headers().withContentTypeJson().withAcceptNdjson(),
                body = undefined,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )

            when {
                response.ok -> {
                    setAvailableFiles {
                        it.minus(file)
                    }
                    response
                        .readLines()
                        .filter(String::isNotEmpty)
                        .onCompletion {
                            setStreamingOperationActive(false)
                            setFileToUnzip(null)
                            reFetchFiles()
                        }
                        .collect { message ->
                            val entryResponse: RawCosvFileStreamingResponse = Json.decodeFromString(message)
                            setCurrentProgress(entryResponse.progress)
                            setCurrentProgressMessage(entryResponse.progressMessage)
                        }
                }
                else -> {
                    setStreamingOperationActive(false)
                    resetCurrentProgress()
                    window.alert("Failed to unzip ${file.fileName}: ${response.unpackMessageOrNull().orEmpty()}")
                }
            }
        }
    }

    val submitAllUploadedCosvFiles = useDeferredRequest {
        val response = post(
            url = "$apiUrl/raw-cosv/$selectedOrganization/submit-all-uploaded-to-process",
            jsonHeaders,
            body = undefined,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler
        )
        if (response.ok) {
            reFetchFiles()
            setCurrentProgress(100)
            setCurrentProgressMessage("All uploaded files submitted to be processed")
        }
    }

    div {
        if (selectedOrganization.isNullOrEmpty()) {
            div {
                className = ClassName("mx-auto")
                b {
                    +"${"Organization that has permission".t()}!"
                }
            }
        }

        organizationSelectForm {
            selectClasses = "custom-select"
            formType = InputTypes.ORGANIZATION_NAME
            validInput = !selectedOrganization.isNullOrEmpty() && selectedOrganization.isValidName()
            classes = "mb-3"
            formName = "Organization"
            getData = { userOrganizations.map { it.name } }
            dataToString = { it }
            selectedValue = selectedOrganization.orEmpty()
            disabled = false
            onChangeFun = { value ->
                setSelectedOrganization(value)
                reFetchFiles()
            }
        }

        ul {
            className = ClassName("list-group")

            // ===== UPLOAD FILES FIELD =====
            li {
                className = ClassName("list-group-item p-0 d-flex bg-light")
                dragAndDropForm {
                    isDisabled = selectedOrganization.isNullOrEmpty() || isStreamingOperationActive
                    isMultipleFilesSupported = true
                    tooltipMessage = "Only JSON files or ZIP archives"
                    onChangeEventHandler = { files ->
                        setFilesForUploading(files!!.asList())
                        uploadFiles()
                    }
                }
            }

            // ===== SUBMIT BUTTONS =====
            li {
                className = ClassName("list-group-item p-1 d-flex bg-light justify-content-center")
                buttonBuilder("Submit all uploaded", classes = "mr-1", isDisabled = statistics.uploadedJsonFilesCount == 0 || isStreamingOperationActive) {
                    if (window.confirm("Processed files will be removed. Do you want to continue?")) {
                        submitAllUploadedCosvFiles()
                    }
                }
                buttonBuilder("Delete all duplicates", classes = "mr-1", isDisabled = statistics.duplicateFilesCount == 0 || isStreamingOperationActive) {
                    if (window.confirm("Duplicated files will be removed. Do you want to continue?")) {
                        deleteAllDuplicatedCosvFiles()
                    }
                }
                buttonBuilder(faReload, isDisabled = isStreamingOperationActive) {
                    reFetchFiles()
                }
            }

            // ===== STATUS BAR =====
            with(statistics) {
                if (!isStreamingOperationActive && allAvailableFilesCount > 0) {
                    li {
                        className = ClassName("list-group-item p-1 d-flex bg-light justify-content-center")

                        when {
                            uploadedJsonFilesCount > 0 && uploadedArchivesCount > 0 -> +"Uploaded $uploadedJsonFilesCount new json files and $uploadedArchivesCount archives. "
                            uploadedJsonFilesCount > 0 -> +"Uploaded $uploadedJsonFilesCount new json files. "
                            uploadedArchivesCount > 0 -> +"Uploaded $uploadedArchivesCount new archives. "
                        }

                        if (processingFilesCount > 0) {
                            +"Still processing $processingFilesCount files. "
                        }

                        if (pendingRemovedFilesCount > 0) {
                            +"Pending to be removed $pendingRemovedFilesCount files. "
                        }

                        when {
                            duplicateFilesCount > 0 && errorFilesCount > 0 -> +"Failed with $duplicateFilesCount duplicates, $errorFilesCount files with another errors."
                            duplicateFilesCount > 0 -> +"Failed with $duplicateFilesCount duplicates."
                            errorFilesCount > 0 -> +"Failed $errorFilesCount files with errors."
                        }
                    }
                }
            }

            // ===== PROGRESS BAR =====
            defaultProgressBarComponent {
                this.currentProgress = currentProgress
                this.currentProgressMessage = currentProgressMessage
                reset = {
                    setCurrentProgress(-1)
                    setCurrentProgressMessage("")
                }
            }

            // ===== AVAILABLE FILES =====
            availableFiles.map { file ->
                li {
                    val highlightZipArchive = when {
                        file.isZipArchive() -> "font-weight-bold"
                        else -> ""
                    }
                    val fileColor = when {
                        file.isZipArchive() -> "primary"
                        file.isUploadedJsonFile() -> "success"
                        file.isProcessing() -> "secondary"
                        file.isPendingRemoved() -> "light"
                        file.isDuplicate() -> "warning"
                        file.isHasErrors() -> "danger"
                        else -> "primary"
                    }
                    className = ClassName("list-group-item $highlightZipArchive text-left list-group-item-$fileColor")
                    asDynamic()["data-toggle"] = "tooltip"
                    asDynamic()["data-placement"] = "left"
                    title = when {
                        file.isZipArchive() -> "It's a ZIP archive, please unzip to get JSON files"
                        file.isUploadedJsonFile() -> "It's a JSON file, you can submit it"
                        file.isProcessing() -> "In progress, please wait"
                        file.isPendingRemoved() -> "Already processed, will be deleted shortly"
                        file.isDuplicate() -> "Duplicate, the vulnerability with such ID already uploaded: ${file.statusMessage.orEmpty()}"
                        file.isHasErrors() -> "This JSON file has error: ${file.statusMessage.orEmpty()}"
                        else -> ""
                    }
                    if (file.isZipArchive()) {
                        button {
                            type = ButtonType.button
                            className = ClassName("btn")
                            fontAwesomeIcon(icon = faBoxOpen)
                            disabled = isStreamingOperationActive
                            onClick = {
                                val confirm = window.confirm(
                                    "Are you sure you want to unzip and then remove ${file.fileName} file?"
                                )
                                if (confirm) {
                                    setFileToUnzip(file)
                                    unzipFile()
                                }
                            }
                        }
                    }
                    downloadFileButton(file, RawCosvFileDto::fileName) {
                        "$apiUrl/raw-cosv/$selectedOrganization/download/${file.requiredId()}"
                    }
                    deleteFileButton(file, RawCosvFileDto::fileName) {
                        setFileToDelete(it)
                        deleteFile()
                    }
                    +"${file.fileName} "
                    span {
                        className = ClassName("font-weight-bold text-justify")
                        +when {
                            file.isProcessing() -> " (in progress)"
                            file.isPendingRemoved() -> " (processed, will be deleted shortly)"
                            file.isDuplicate() -> " (duplicate)"
                            file.isHasErrors() -> " (error)"
                            else -> " "
                        }
                    }
                }
            }

            if (leftAvailableFilesCount > 0) {
                li {
                    className = ClassName("list-group-item p-1 d-flex bg-light justify-content-center")
                    buttonBuilder("Load more (left $leftAvailableFilesCount)", isDisabled = isStreamingOperationActive) {
                        fetchMoreFiles()
                    }
                }
            }
        }
    }
}
