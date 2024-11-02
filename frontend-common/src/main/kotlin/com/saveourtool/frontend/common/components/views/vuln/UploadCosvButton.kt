@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.frontend.common.components.views.vuln

import com.saveourtool.common.validation.FrontendCosvRoutes
import com.saveourtool.frontend.common.externals.fontawesome.faFile
import com.saveourtool.frontend.common.externals.i18next.useTranslation
import com.saveourtool.frontend.common.utils.buttonBuilder

import react.FC
import react.Props
import react.router.useNavigate

val uploadCosvButton: FC<UploadCosvButtonProps> = FC { props ->
    val (t) = useTranslation("vulnerability-upload")
    val navigate = useNavigate()

    if (props.isImage) {
        buttonBuilder(faFile, style = "primary", title = "Add new vulnerability from json", classes = "icon-2-5rem", isOutline = true) {
            navigate("/${FrontendCosvRoutes.VULN_UPLOAD}")
        }
    } else {
        buttonBuilder("Upload COSV files".t(), style = "primary", isOutline = true) {
            navigate("/${FrontendCosvRoutes.VULN_UPLOAD}")
        }
    }
}

/**
 * [Props] for [uploadCosvButton]
 */
external interface UploadCosvButtonProps : Props {
    /**
     * Callback invoked on user click
     */
    var isImage: Boolean
}
