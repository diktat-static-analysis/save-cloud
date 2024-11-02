/**
 * Run command editor section of projectDemoMenu
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.demo.management

import com.saveourtool.common.demo.DemoDto
import com.saveourtool.common.demo.RunCommandPair
import com.saveourtool.frontend.common.externals.fontawesome.faEdit
import com.saveourtool.frontend.common.externals.fontawesome.faPlus
import com.saveourtool.frontend.common.utils.buttonBuilder
import com.saveourtool.frontend.common.utils.useTooltip

import react.*
import react.dom.html.AutoComplete
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import web.cssom.ClassName

/**
 * A pair of input forms for Mode name and run command with "Apply" button
 */
@Suppress("TOO_LONG_FUNCTION")
val demoRunCommandEditor: FC<DemoSettingsAdder> = FC { props ->
    useTooltip()
    val (modeName, setModeName) = useState(props.defaultModeName.orEmpty())
    val (runCommand, setRunCommand) = useState(props.defaultRunCommand.orEmpty())
    div {
        className = ClassName("input-group mb-2")
        input {
            className = ClassName("form-control col-3")
            autoComplete = AutoComplete.off
            placeholder = "Mode name"
            asDynamic()["data-toggle"] = "tooltip"
            asDynamic()["data-placement"] = "left"
            title = "Name of a mode that will be displayed on frontend (e.g. Warn, Fix)."
            value = modeName
            disabled = props.disabled
            onChange = { event -> setModeName(event.target.value) }
        }
        input {
            className = ClassName("form-control col")
            autoComplete = AutoComplete.off
            placeholder = "Run command"
            asDynamic()["data-toggle"] = "tooltip"
            asDynamic()["data-placement"] = "bottom"
            title = "Command that will be executed on request to run your tool in this mode."
            value = runCommand
            disabled = props.disabled
            onChange = { event -> setRunCommand(event.target.value) }
        }
        div {
            className = ClassName("input-group-append")
            val icon = if (props.isEdit) faEdit else faPlus
            buttonBuilder(
                icon,
                isDisabled = props.disabled || modeName.isBlank() || runCommand.isBlank(),
                isOutline = true,
            ) {
                props.setDemoDto { demoDto ->
                    val newRunCommands = if (props.isEdit) {
                        demoDto.runCommands.minus(props.defaultModeName.orEmpty()).plus(modeName to runCommand)
                    } else {
                        demoDto.runCommands.plus(modeName to runCommand)
                    }
                    demoDto.copy(
                        runCommands = newRunCommands
                    )
                }
                props.setSelectedOption?.invoke(null)
                setRunCommand("")
                setModeName("")
            }
        }
    }
}

/**
 * [Props] of [demoRunCommandEditor]
 */
external interface DemoSettingsAdder : Props {
    /**
     * Callback to update [DemoDto] state
     */
    var setDemoDto: StateSetter<DemoDto>

    /**
     * Flag that defines whether in forms should be disabled or not
     */
    var disabled: Boolean

    /**
     * Default value of modeName input form, if null, empty string is used
     */
    var defaultModeName: String?

    /**
     * Default value of runCommand input form, if null, empty string is used
     */
    var defaultRunCommand: String?

    /**
     * Flag that defined if [demoRunCommandEditor] run in edit mode or not
     */
    var isEdit: Boolean

    /**
     * Callback to update currently selected [RunCommandPair]
     */
    var setSelectedOption: StateSetter<RunCommandPair?>?
}

/**
 * Display run command editor section of projectDemoMenu
 *
 * @param demoDto currently configured [DemoDto]
 * @param setDemoDto callback to update [demoDto] state
 * @param isDisabled flag that defines if input forms are disabled or not
 * @param onOptionClickCallback callback that selects [RunCommandPair] and opens extended run command editor
 */
internal fun ChildrenBuilder.renderRunCommand(
    demoDto: DemoDto,
    setDemoDto: StateSetter<DemoDto>,
    isDisabled: Boolean,
    onOptionClickCallback: StateSetter<RunCommandPair?>,
) {
    div {
        div {
            demoRunCommandEditor {
                this.setDemoDto = setDemoDto
                this.disabled = isDisabled
                this.defaultModeName = ""
                this.defaultRunCommand = ""
                this.isEdit = false
            }
            div {
                className = ClassName("d-flex justify-content-start")
                demoDto.runCommands.map { (mode, command) ->
                    demoModeLabel {
                        this.modeName = mode
                        this.runCommand = command
                        this.classes = "m-2"
                        this.onClickCallback = { runCommandPair -> onOptionClickCallback(runCommandPair) }
                    }
                }
            }
        }
    }
}
