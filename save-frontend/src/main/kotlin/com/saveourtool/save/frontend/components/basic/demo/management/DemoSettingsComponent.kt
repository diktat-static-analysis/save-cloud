/**
 * Demo file names input forms
 */

package com.saveourtool.save.frontend.components.basic.demo.management

import com.saveourtool.common.demo.DemoDto

import react.*
import react.dom.html.AutoComplete
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import web.cssom.ClassName

/**
 * Display demo file names input forms such as config filename form, input filename form etc
 *
 * @param demoDto currently configured [DemoDto]
 * @param setDemoDto callback to update [demoDto] state
 * @param isDisabled flag that defines if input forms are disabled or not
 */
internal fun ChildrenBuilder.renderDemoSettings(demoDto: DemoDto, setDemoDto: StateSetter<DemoDto>, isDisabled: Boolean) {
    div {
        div {
            input {
                className = ClassName("form-control col mb-2")
                autoComplete = AutoComplete.off
                placeholder = "Test file name"
                value = demoDto.fileName
                this.disabled = isDisabled
                asDynamic()["data-toggle"] = "tooltip"
                asDynamic()["data-placement"] = "right"
                title = "Name of a file that would contain input code. It should match the name used in run command!"
                onChange = { event ->
                    setDemoDto { request ->
                        request.copy(fileName = event.target.value)
                    }
                }
            }
        }
        div {
            className = ClassName("d-flex justify-content-between")
            input {
                className = ClassName("form-control col mr-1")
                autoComplete = AutoComplete.off
                placeholder = "Output file name"
                value = demoDto.outputFileName
                disabled = isDisabled
                asDynamic()["data-toggle"] = "tooltip"
                asDynamic()["data-placement"] = "left"
                title = "Name of a file that contains the output of a tool e.g. list of warnings."
                onChange = { event ->
                    setDemoDto { request ->
                        request.copy(outputFileName = event.target.value.ifBlank { null })
                    }
                }
            }
            input {
                className = ClassName("form-control col ml-1")
                autoComplete = AutoComplete.off
                placeholder = "Config name"
                value = demoDto.configName
                disabled = isDisabled
                asDynamic()["data-toggle"] = "tooltip"
                asDynamic()["data-placement"] = "right"
                title = "Name of config file for your tool. You may leave it empty if none is required."
                onChange = { event ->
                    setDemoDto { request ->
                        request.copy(configName = event.target.value.ifBlank { null })
                    }
                }
            }
        }
    }
}
