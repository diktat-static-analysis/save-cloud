/**
 * Modal for better run command management
 */

package com.saveourtool.save.frontend.components.basic.demo.management

import com.saveourtool.common.demo.DemoDto
import com.saveourtool.common.demo.RunCommandPair
import com.saveourtool.common.utils.isNotNull
import com.saveourtool.frontend.common.components.modal.mediumTransparentModalStyle
import com.saveourtool.frontend.common.components.modal.modal
import com.saveourtool.frontend.common.components.modal.modalBuilder
import com.saveourtool.frontend.common.utils.buttonBuilder

import react.*
import react.dom.html.ReactHTML.div
import web.cssom.ClassName

/**
 * Display modal for better run command management
 *
 * @param selectedOption currently selected [RunCommandPair]
 * @param setSelectedOption callback to update state of [selectedOption]
 * @param disabled flag that defines whether forms should be disabled or not
 * @param setDemoDto callback to update currently configured [DemoDto]
 */
internal fun ChildrenBuilder.demoModeModal(
    selectedOption: RunCommandPair?,
    setSelectedOption: StateSetter<RunCommandPair?>,
    disabled: Boolean,
    setDemoDto: StateSetter<DemoDto>,
) {
    val selectedDemoMode = selectedOption?.first
    val selectedRunCommand = selectedOption?.second
    modal { props ->
        props.isOpen = selectedOption.isNotNull()
        props.style = mediumTransparentModalStyle
        modalBuilder(
            "Demo Mode",
            onCloseButtonPressed = { setSelectedOption(null) },
            bodyBuilder = {
                div {
                    className = ClassName("d-flex justify-content-center")
                    demoRunCommandEditor {
                        this.setDemoDto = setDemoDto
                        this.disabled = disabled
                        this.defaultModeName = selectedDemoMode
                        this.defaultRunCommand = selectedRunCommand
                        this.isEdit = true
                        this.setSelectedOption = setSelectedOption
                    }
                }
            },
        ) {
            buttonBuilder("Delete", "danger", isDisabled = disabled) {
                setDemoDto { demoDto ->
                    demoDto.copy(runCommands = demoDto.runCommands.filter { (mode, _) ->
                        mode != selectedDemoMode
                    })
                }
                setSelectedOption(null)
            }
        }
    }
}
