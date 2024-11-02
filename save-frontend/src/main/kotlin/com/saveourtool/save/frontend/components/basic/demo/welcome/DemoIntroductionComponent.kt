/**
 * Component for save-demo brief info rendering
 */

package com.saveourtool.save.frontend.components.basic.demo.welcome

import com.saveourtool.frontend.common.components.basic.markdown

import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.strong
import web.cssom.ClassName
import web.cssom.rem

private val saveDemoIntroMd = """
    |## save-demo
    |We are happy yo introduce you a new service - **Static Analyzers Online Demo**, which will help developers of code analysis tools demonstrate their capabilities. 
""".trimMargin()

private val saveDemoHowToMd = """
    |### How to create demo?
    |1. Create saveourtool [organization](/create-organization) and [project](/create-project);
    |2. Go to your project's demo tab;
    |3. Upload your tool, select sdk and set run commands;
    |4. Make sure all the inputted filenames match filenames in run commands; 
    |5. Make sure that run commands are enough to start your tool;
    |5. Create demo and run demo container.
    |
    |Save platform will automatically prepare and run a container with your tool and configurations.
    |In case of any error feel free to [contact us](https://github.com/saveourtool/save-cloud/issues/new).
""".trimMargin()

@Suppress("MAGIC_NUMBER")
internal val introductionSection: FC<Props> = FC {
    div {
        className = ClassName("card flex-md-column mb-1 box-shadow")
        style = jso { minHeight = 25.rem }
        div {
            className = ClassName("card-body d-flex align-items-start")
            div {
                strong {
                    className = ClassName("d-inline-block mb-2 text-info")
                    +"Introducing"
                }
                markdown(saveDemoIntroMd)
            }
            div {
                className = ClassName("card-img-right flex-column d-none d-md-block")
                img {
                    className = ClassName("img-fluid")
                    src = "/img/undraw_happy_announcement_re_tsm0.svg"
                }
            }
        }
        div {
            className = ClassName("card-body pt-0 pb-1")
            markdown(saveDemoHowToMd)
        }
    }
}
