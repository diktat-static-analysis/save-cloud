@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.views.organization

import com.saveourtool.common.entities.OrganizationDto
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import web.cssom.ClassName
import web.html.InputType

val renderAdminTab: FC<RenderAdminTabProps> = FC { props ->

    div {
        className = ClassName("row justify-content-center mb-2 text-gray-900")
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Main settings"
            }
            div {
                className = ClassName("card card-body mt-0 p-0")
                div {
                    className = ClassName("d-sm-flex justify-content-center form-check pl-3 pr-3 pt-3")
                    div {
                        input {
                            className = ClassName("form-check-input")
                            type = InputType.checkbox
                            value = props.organization.canCreateContests.toString()
                            id = "canCreateContestsCheckbox"
                            checked = props.organization.canCreateContests
                            onChange = {
                                props.onCanCreateContestsChange(!props.organization.canCreateContests)
                            }
                        }
                    }
                    div {
                        label {
                            className = ClassName("form-check-label")
                            htmlFor = "canCreateContestsCheckbox"
                            +"Can create contests"
                        }
                    }
                }

                div {
                    className = ClassName("d-sm-flex justify-content-center form-check pl-3 pr-3 pt-3")
                    div {
                        input {
                            className = ClassName("form-check-input")
                            type = InputType.checkbox
                            value = props.organization.canBulkUpload.toString()
                            id = "canBulkUploadCosvFilesCheckbox"
                            checked = props.organization.canBulkUpload
                            onChange = {
                                props.onCanBulkUploadCosvFilesChange(!props.organization.canBulkUpload)
                            }
                        }
                    }
                    div {
                        label {
                            className = ClassName("form-check-label")
                            htmlFor = "canBulkUploadCosvFilesCheckbox"
                            +"Can bulk upload COSV files"
                        }
                    }
                }
            }
        }
    }
}

/**
 * RenderInfoTab component props
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface RenderAdminTabProps : Props {
    /**
     * Organization
     */
    var organization: OrganizationDto

    /**
     * Callback invoked in order to change canCreateContests flag
     */
    var onCanCreateContestsChange: (Boolean) -> Unit

    /**
     * Callback invoked in order to change canBulkUpload flag
     */
    var onCanBulkUploadCosvFilesChange: (Boolean) -> Unit
}
