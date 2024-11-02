@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.common.demo.DemoCreationRequest
import com.saveourtool.common.demo.DemoDto
import com.saveourtool.common.demo.DemoStatus
import com.saveourtool.common.demo.RunCommandPair
import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.domain.Role
import com.saveourtool.common.domain.Sdk
import com.saveourtool.common.domain.orEmpty
import com.saveourtool.common.entities.FileDto
import com.saveourtool.frontend.common.components.basic.cardComponent
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.components.basic.demo.*
import com.saveourtool.save.frontend.components.basic.demo.management.*
import com.saveourtool.save.frontend.components.basic.demo.management.renderButtons
import com.saveourtool.save.frontend.components.basic.demo.management.renderFileUploading

import io.ktor.http.*
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.hr
import web.cssom.ClassName

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val demoInfoCard = cardComponent(isBordered = true, hasBg = true, isNoPadding = false)

/**
 * Project demo menu
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "EMPTY_BLOCK_STRUCTURE_ERROR",
    "MAGIC_NUMBER"
)
val projectDemoMenu: FC<ProjectDemoMenuProps> = FC { props ->
    val (demoDto, setDemoDto) = useState(
        DemoDto.emptyForProject(props.organizationName, props.projectName)
    )
    val (demoStatus, setDemoStatus) = useState(DemoStatus.NOT_CREATED)
    val (githubProjectCoordinates, setGithubProjectCoordinates) = useState(ProjectCoordinates.empty)
    val (selectedSdk, setSelectedSdk) = useState<Sdk>(Sdk.Default)

    val getDemoDto = useDeferredRequest {
        val dtoResponse = get(
            "$demoApiUrl/manager/${props.organizationName}/${props.projectName}",
            jsonHeaders,
            ::loadingHandler,
            ::noopResponseHandler,
        )
        if (dtoResponse.ok) {
            val dto: DemoDto = dtoResponse.decodeFromJsonString()
            setDemoDto(dto)
            setSelectedSdk(dto.sdk)
            setGithubProjectCoordinates(dto.githubProjectCoordinates.orEmpty())
        } else if (dtoResponse.status != 404.toShort()) {
            props.updateErrorMessage(dtoResponse.statusText, dtoResponse.unpackMessage())
            setDemoStatus(DemoStatus.ERROR)
        } else {
            demoDto.copy(githubProjectCoordinates = githubProjectCoordinates)
        }
    }

    val deleteDemo = useDeferredRequest {
        post(
            "$apiUrl/demo/${props.organizationName}/${props.projectName}/delete",
            jsonHeaders,
            Unit,
            loadingHandler = ::loadingHandler,
        ).let {
            if (it.ok) {
                window.alert(it.body as String)
                setDemoStatus(DemoStatus.NOT_CREATED)
                setDemoDto(DemoDto.emptyForProject(props.organizationName, props.projectName))
                setGithubProjectCoordinates(ProjectCoordinates.empty)
            }
        }
    }

    val getDemoStatus = useDeferredRequest {
        val statusResponse = get(
            "$demoApiUrl/manager/${props.organizationName}/${props.projectName}/status",
            jsonHeaders,
            ::loadingHandler,
            ::noopResponseHandler,
        )
        if (statusResponse.ok) {
            setDemoStatus(statusResponse.decodeFromJsonString<DemoStatus>())
        } else {
            props.updateErrorMessage(statusResponse.statusText, statusResponse.unpackMessage())
            setDemoStatus(DemoStatus.ERROR)
        }
    }

    val (selectedFileDtos, setSelectedFileDtos) = useState(emptyList<FileDto>())
    val createOrUpdateDemo = useDeferredRequest {
        if (githubProjectCoordinates.consideredBlank()) {
            demoDto.copy(githubProjectCoordinates = null)
        } else {
            demoDto.copy(githubProjectCoordinates = githubProjectCoordinates)
        }
            .copy(sdk = selectedSdk)
            .let { demoRequest ->
                post(
                    "$apiUrl/demo/${props.organizationName}/${props.projectName}/save-or-update",
                    jsonHeaders,
                    Json.encodeToString(
                        DemoCreationRequest(
                            demoRequest,
                            selectedFileDtos,
                        )
                    ),
                    ::loadingHandler,
                    ::noopResponseHandler,
                )
                    .let {
                        if (it.ok) {
                            setDemoStatus(DemoStatus.STOPPED)
                            window.alert(it.body as String)
                        } else {
                            props.updateErrorMessage(it.statusText, it.unpackMessage())
                        }
                    }
            }
    }

    val startDemo = useDeferredRequest {
        post(
            "$apiUrl/demo/${props.organizationName}/${props.projectName}/start",
            jsonHeaders,
            Unit,
            ::loadingHandler,
        ).let {
            if (it.ok) {
                setDemoStatus(DemoStatus.STARTING)
                window.alert(it.body as String)
            }
        }
    }

    val stopDemo = useDeferredRequest {
        post(
            "$apiUrl/demo/${props.organizationName}/${props.projectName}/stop",
            jsonHeaders,
            Unit,
            ::loadingHandler,
        ).let {
            if (it.ok) {
                setDemoStatus(DemoStatus.STOPPING)
                window.alert(it.body as String)
            }
        }
    }

    val getDemoFiles = useDeferredRequest {
        get(
            "$apiUrl/demo/${props.organizationName}/${props.projectName}/list-file?version=manual",
            jsonHeaders,
            ::loadingHandler,
            ::noopResponseHandler,
        ).let { response ->
            if (response.ok) {
                setSelectedFileDtos(response.decodeFromJsonString<List<FileDto>>())
            }
        }
    }

    useOnce {
        getDemoDto()
        getDemoStatus()
        getDemoFiles()
    }

    useTooltip()

    val (selectedModeCommand, setSelectedModeCommand) = useState<RunCommandPair?>(null)
    val isDisabled = demoStatus != DemoStatus.STOPPED && demoStatus != DemoStatus.NOT_CREATED
    demoModeModal(selectedModeCommand, setSelectedModeCommand, isDisabled, setDemoDto)

    div {
        className = ClassName("d-flex justify-content-center align-items-center p-2")
        div {
            className = ClassName("col-5")
            demoInfoCard {
                div {
                    className = ClassName("d-flex justify-content-center")
                    renderStatusLabel(demoStatus)
                }
                hr { }
                renderDemoSettings(demoDto, setDemoDto, isDisabled)
                hr { }
                renderRunCommand(demoDto, setDemoDto, isDisabled, setSelectedModeCommand)
                hr { }
                renderFileUploading(demoDto, setDemoDto, isDisabled, githubProjectCoordinates, setGithubProjectCoordinates, setSelectedFileDtos)
                hr { }
                sdkSelection {
                    title = ""
                    this.isDisabled = isDisabled
                    this.selectedSdk = selectedSdk
                    onSdkChange = setSelectedSdk::invoke
                }
                hr { }
                renderButtons(
                    ProjectCoordinates(props.organizationName, props.projectName),
                    demoStatus,
                    props.userProjectRole,
                    createOrUpdateDemo,
                    {
                        getDemoStatus()
                        getDemoFiles()
                    },
                    startDemo,
                    stopDemo
                ) {
                    if (window.confirm("Delete demo of ${props.organizationName}/${props.projectName}?")) {
                        deleteDemo()
                        setDemoStatus(DemoStatus.STOPPING)
                    }
                }
            }
        }
    }
}

/**
 * [projectDemoMenu] component [Props]
 */
external interface ProjectDemoMenuProps : Props {
    /**
     * Project name
     */
    var projectName: String

    /**
     * Organization name
     */
    var organizationName: String

    /**
     * User role in project
     */
    var userProjectRole: Role

    /**
     * Callback to show error message
     */
    @Suppress("TYPE_ALIAS")
    var updateErrorMessage: (String, String) -> Unit
}
