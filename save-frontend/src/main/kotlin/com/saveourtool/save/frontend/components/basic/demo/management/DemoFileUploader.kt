/**
 * File uploader of demo management card
 */

package com.saveourtool.save.frontend.components.basic.demo.management

import com.saveourtool.common.demo.DemoDto
import com.saveourtool.common.domain.ProjectCoordinates
import com.saveourtool.common.entities.FileDto
import com.saveourtool.frontend.common.utils.apiUrl
import com.saveourtool.save.frontend.components.basic.fileuploader.FileDtosSetter
import com.saveourtool.save.frontend.components.basic.fileuploader.simpleFileUploader

import react.ChildrenBuilder
import react.StateSetter
import react.dom.html.AutoComplete
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import web.cssom.ClassName

/**
 * Display file uploader of demo management card
 *
 * @param demoDto currently configured [DemoDto]
 * @param setDemoDto callback to update state of [demoDto]
 * @param isDisabled flag that defines if input forms are disabled or not
 * @param githubProjectCoordinates GitHub's organization and project name wrapped into [ProjectCoordinates]
 * @param setGithubProjectCoordinates callback that updates [githubProjectCoordinates] state
 * @param setSelectedFileDtos update state of list of [FileDto] that should be connected with this demo
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "TOO_MANY_PARAMETERS",
    "LongParameterList"
)
internal fun ChildrenBuilder.renderFileUploading(
    demoDto: DemoDto,
    setDemoDto: StateSetter<DemoDto>,
    isDisabled: Boolean,
    githubProjectCoordinates: ProjectCoordinates,
    setGithubProjectCoordinates: StateSetter<ProjectCoordinates>,
    setSelectedFileDtos: FileDtosSetter,
) {
    div {
        className = ClassName("d-flex justify-content-between align-items-center")
        div {
            className = ClassName("col pl-0")
            asDynamic()["data-toggle"] = "tooltip"
            asDynamic()["data-placement"] = "left"
            title = "Unfortunately, GitHub tool download is temporary disabled. Please, upload the tool manually."
            input {
                className = ClassName("form-control col mb-2")
                autoComplete = AutoComplete.off
                placeholder = "GitHub organization name"
                value = githubProjectCoordinates.organizationName
                disabled = true
                onChange = { event ->
                    setGithubProjectCoordinates {
                        it.copy(organizationName = event.target.value)
                    }
                }
            }
            input {
                className = ClassName("form-control col mb-2")
                autoComplete = AutoComplete.off
                placeholder = "GitHub project name"
                value = githubProjectCoordinates.projectName
                disabled = true
                onChange = { event ->
                    setGithubProjectCoordinates {
                        it.copy(projectName = event.target.value)
                    }
                }
            }
            input {
                className = ClassName("form-control col")
                autoComplete = AutoComplete.off
                placeholder = "Release tag"
                value = demoDto.vcsTagName
                disabled = true
                onChange = { event ->
                    setDemoDto { request ->
                        request.copy(vcsTagName = event.target.value)
                    }
                }
            }
        }
        div {
            +" or "
        }
        div {
            className = ClassName("col pr-0")
            div {
                simpleFileUploader {
                    getUrlForAvailableFilesFetch = {
                        with(demoDto) { "$apiUrl/files/$projectCoordinates/list" }
                    }
                    getUrlForSelectedFilesFetch = {
                        with(demoDto) { "$apiUrl/demo/$projectCoordinates/list-file" }
                    }
                    fileDtosSetter = setSelectedFileDtos
                    this.isDisabled = isDisabled
                }
            }
        }
    }
}
