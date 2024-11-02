@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.cosv.frontend.components.views.vuln.component

import com.saveourtool.common.cvsscalculator.*
import com.saveourtool.common.cvsscalculator.v3.*
import com.saveourtool.frontend.common.utils.buttonBuilder

import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.events.FormEventHandler
import react.dom.html.AutoComplete
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.dt
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.onChange
import react.useState
import web.cssom.*
import web.html.HTMLLabelElement
import web.html.InputType

import kotlin.Float

val cvssBaseScoreCalculator: FC<CvssBaseScoreCalculatorProps> = FC { props ->
    val (baseMetrics, setBaseMetrics) = useState(props.baseMetricsOfVulnerability ?: BaseMetricsV3.empty)

    div {
        className = ClassName("col-12 text-center")

        div {
            className = ClassName("row mt-4")

            div {
                className = ClassName("col-3")

                div {
                    className = ClassName("btn-group-vertical btn-group-toggle")

                    style = jso {
                        width = "100%".unsafeCast<Width>()
                    }

                    header("Attack vector")

                    radioButton(baseMetrics.attackVector == AttackVectorType.NETWORK, "Network", "danger") {
                        setBaseMetrics { it.copy(attackVector = AttackVectorType.NETWORK) }
                    }

                    radioButton(baseMetrics.attackVector == AttackVectorType.ADJACENT_NETWORK, "Adjacent", "warning") {
                        setBaseMetrics { it.copy(attackVector = AttackVectorType.ADJACENT_NETWORK) }
                    }

                    radioButton(baseMetrics.attackVector == AttackVectorType.LOCAL, "Local", "", true) {
                        setBaseMetrics { it.copy(attackVector = AttackVectorType.LOCAL) }
                    }

                    radioButton(baseMetrics.attackVector == AttackVectorType.PHYSICAL, "Physical", "", true) {
                        setBaseMetrics { it.copy(attackVector = AttackVectorType.PHYSICAL) }
                    }
                }
            }

            div {
                className = ClassName("col-3")

                div {
                    className = ClassName("btn-group-vertical btn-group-toggle")

                    style = jso {
                        width = "100%".unsafeCast<Width>()
                    }

                    header("Attack complexity")

                    radioButton(baseMetrics.attackComplexity == AttackComplexityType.LOW, "Low", "danger") {
                        setBaseMetrics { it.copy(attackComplexity = AttackComplexityType.LOW) }
                    }

                    radioButton(baseMetrics.attackComplexity == AttackComplexityType.HIGH, "High", "warning", true) {
                        setBaseMetrics { it.copy(attackComplexity = AttackComplexityType.HIGH) }
                    }
                }
            }

            div {
                className = ClassName("col-3")

                div {
                    className = ClassName("btn-group-vertical btn-group-toggle")

                    style = jso {
                        width = "100%".unsafeCast<Width>()
                    }

                    header("Privileges required")

                    radioButton(baseMetrics.privilegeRequired == PrivilegesRequiredType.NONE, "None", "danger") {
                        setBaseMetrics { it.copy(privilegeRequired = PrivilegesRequiredType.NONE) }
                    }

                    radioButton(baseMetrics.privilegeRequired == PrivilegesRequiredType.LOW, "Low", "warning") {
                        setBaseMetrics { it.copy(privilegeRequired = PrivilegesRequiredType.LOW) }
                    }

                    radioButton(baseMetrics.privilegeRequired == PrivilegesRequiredType.HIGH, "High", "", true) {
                        setBaseMetrics { it.copy(privilegeRequired = PrivilegesRequiredType.HIGH) }
                    }
                }
            }

            div {
                className = ClassName("col-3")

                div {
                    className = ClassName("btn-group-vertical btn-group-toggle")

                    style = jso {
                        width = "100%".unsafeCast<Width>()
                    }

                    header("User interaction")

                    radioButton(baseMetrics.userInteraction == UserInteractionType.NONE, "None", "danger") {
                        setBaseMetrics { it.copy(userInteraction = UserInteractionType.NONE) }
                    }

                    radioButton(baseMetrics.userInteraction == UserInteractionType.REQUIRED, "Required", "warning", true) {
                        setBaseMetrics { it.copy(userInteraction = UserInteractionType.REQUIRED) }
                    }
                }
            }
        }

        div {
            className = ClassName("row mt-4")

            div {
                className = ClassName("col-3")

                div {
                    className = ClassName("btn-group-vertical btn-group-toggle")

                    style = jso {
                        width = "100%".unsafeCast<Width>()
                    }

                    header("Scope")

                    radioButton(baseMetrics.scopeMetric == ScopeType.CHANGED, "Changed", "danger") {
                        setBaseMetrics { it.copy(scopeMetric = ScopeType.CHANGED) }
                    }

                    radioButton(baseMetrics.scopeMetric == ScopeType.UNCHANGED, "Unchanged", "", true) {
                        setBaseMetrics { it.copy(scopeMetric = ScopeType.UNCHANGED) }
                    }
                }
            }

            div {
                className = ClassName("col-3")

                div {
                    className = ClassName("btn-group-vertical btn-group-toggle")

                    style = jso {
                        width = "100%".unsafeCast<Width>()
                    }

                    header("Confidentiality")

                    radioButton(baseMetrics.confidentiality == CiaType.HIGH, "High", "danger") {
                        setBaseMetrics { it.copy(confidentiality = CiaType.HIGH) }
                    }

                    radioButton(baseMetrics.confidentiality == CiaType.LOW, "Low", "warning") {
                        setBaseMetrics { it.copy(confidentiality = CiaType.LOW) }
                    }

                    radioButton(baseMetrics.confidentiality == CiaType.NONE, "None", "success") {
                        setBaseMetrics { it.copy(confidentiality = CiaType.NONE) }
                    }
                }
            }

            div {
                className = ClassName("col-3")

                div {
                    className = ClassName("btn-group-vertical btn-group-toggle")

                    style = jso {
                        width = "100%".unsafeCast<Width>()
                    }

                    header("Integrity")

                    radioButton(baseMetrics.integrity == CiaType.HIGH, "High", "danger") {
                        setBaseMetrics { it.copy(integrity = CiaType.HIGH) }
                    }

                    radioButton(baseMetrics.integrity == CiaType.LOW, "Low", "warning") {
                        setBaseMetrics { it.copy(integrity = CiaType.LOW) }
                    }

                    radioButton(baseMetrics.integrity == CiaType.NONE, "None", "success") {
                        setBaseMetrics { it.copy(integrity = CiaType.NONE) }
                    }
                }
            }

            div {
                className = ClassName("col-3")

                div {
                    className = ClassName("btn-group-vertical btn-group-toggle")

                    style = jso {
                        width = "100%".unsafeCast<Width>()
                    }

                    header("Availability")

                    radioButton(baseMetrics.availability == CiaType.HIGH, "High", "danger") {
                        setBaseMetrics { it.copy(availability = CiaType.HIGH) }
                    }

                    radioButton(baseMetrics.availability == CiaType.LOW, "Low", "warning") {
                        setBaseMetrics { it.copy(availability = CiaType.LOW) }
                    }

                    radioButton(baseMetrics.availability == CiaType.NONE, "None", "success") {
                        setBaseMetrics { it.copy(availability = CiaType.NONE) }
                    }
                }
            }
        }

        div {
            className = ClassName("row mt-4")
            div {
                className = ClassName("col-12")

                header("Severity score vector")

                li {
                    className = ClassName("list-group-item d-flex justify-content-center")
                    val score = CvssVectorV3(CvssVersion.CVSS_V3_1, baseMetrics)
                    +"${score.calculateValidBaseScore()?.let { "${getCriticality(it)} $it " } ?: ""}${score.scoreVectorString()}"
                }
            }
        }

        div {
            className = ClassName("mt-4 modal-footer")
            buttonBuilder("Ok", "success", classes = "mr-2", isDisabled = !baseMetrics.isValid()) {
                props.onCloseButtonPassed(baseMetrics)
                props.onCloseButton()
            }
            buttonBuilder("Close", "secondary") {
                props.onCloseButton()
            }
        }
    }
}

/**
 * [Props] for [cvssBaseScoreCalculator]
 */
external interface CvssBaseScoreCalculatorProps : Props {
    /**
     * Base metrics of CVSS
     */
    var baseMetricsOfVulnerability: BaseMetricsV3?

    /**
     * Callback to close window
     */
    var onCloseButton: () -> Unit

    /**
     * Callback to update base metrics
     */
    var onCloseButtonPassed: (BaseMetricsV3) -> Unit
}

private fun ChildrenBuilder.radioButton(
    isActive: Boolean,
    buttonName: String = "",
    activeStyle: String = "outline-dark",
    isCustomColor: Boolean = false,
    onClickFun: FormEventHandler<HTMLLabelElement>,
) {
    label {
        className = ClassName("btn ${if (isActive && isCustomColor) {
            ""
        } else {
            "btn-${if (isActive) "$activeStyle active" else "outline-dark"}"
        }}")

        style = jso {
            if (isActive && isCustomColor) {
                color = "#212529".unsafeCast<Color>()
                backgroundColor = "#FFFF00".unsafeCast<BackgroundColor>()
            }
            borderColor = "#212529".unsafeCast<BorderColor>()
        }

        input {
            name = "options"
            type = InputType.radio
            autoComplete = AutoComplete.off
        }
        onChange = onClickFun
        +buttonName
    }
}

private fun ChildrenBuilder.header(
    name: String
) {
    dt {
        className = ClassName("text-xs")
        style = jso {
            width = "100%".unsafeCast<Width>()
            color = "#ffffff".unsafeCast<BorderColor>()
            backgroundColor = "#404040".unsafeCast<BorderColor>()
            borderColor = "#000000".unsafeCast<BorderColor>()
        }
        +name
    }
}

private fun getCriticality(value: Float): String = when (value) {
    0f -> "None"
    in 0.1f..3.9f -> "Low"
    in 3.9f..6.9f -> "Medium"
    in 6.9f..8.9f -> "High"
    in 8.9f..10f -> "Critical"
    else -> throw IllegalStateException("Progress should be in [0; 10.0], got $value")
}
