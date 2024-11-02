/**
 * [1 page] Main information about SAVE-cloud
 */

package com.saveourtool.frontend.common.components.views.welcome.pagers.save

import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.p
import web.cssom.*

private const val SAVE_CLI_TEXT = """
     Imagine, that you are a developer of some complex tool, for example a Compiler or a Static Analyzer. 
     What are first things that you will do after (or even before) coding your initial functionality?
     Correct! You will start writing a test framework with functional tests to validate and fixate the code to avoid 
     regressions. The logic for these frameworks is very simple: they are running the tool on some text file and validate 
     the output. We decided to propose a universal native test framework (SAVE-cli) that will have it's own DSL and will 
     be covering common testing scenarios that developer of such group of tools can face.
    """

private const val EASY_CI_TEXT = """
     Same is done by hundreds of teams that create system programming tools - they are reinventing the quick-and-dirty 
     wheel again and again to run their tests somehow. If the project becomes mature and big enough, the number of such 
     tests grows exponentially (some compilers have more than 500k tests). In this case Continuous Integration becomes a 
     hard challenge as the time spent on running such tests and analyzing results tends to infinity. SAVE-cloud provides 
     you a distributed cloud cluster for the parallelization of SAVE-cli runs, dashboards for results and historical 
     information for the detection of regressions.
    """

private const val BENCHMARKS_TEXT = """
     SAVE can run several open benchmarks for validation of your tools, for example MISRA, NIST JULIET, 
     etc for static analyzers. But if you feel that your test pack is good enough to become a some sort of a standard benchmark 
     - you can easily share it with the community, just follow SAVE format and provide git URL to your tests. 
     SAVE will automatically download it, prepare a snapshot for each commit and community will get a chance to 
     use it for testing and even certifying of their tools. If you don't like to share sources, 
     SAVE provides a possibility to share private tests, that will be used for a black-box testing: only results 
     of testing (pass-rate) will be shown. We think that it is the best variant to avoid doing the same work and 
     writing same tests again and again.
    """

private const val CONTESTS_TEXT = """
     If CI platform, sharing of your tests with community or the usage of existing benchmarks are not enough for your 
     project, then you can challenge other tools in SAVE contests. In these contests you need to pass as much tests 
     as possible to get the highest rating and become the champion. In each contest you get one open example of 
     benchmark and multiple closed benchmarks from the same category. For example - if the contest is related to 
     static analysis, you will get a pack of tests where your tool should find NPE and one open code snippet with possible 
     null pointer. If participating is not enough for you - then you can also create your own contests with your own challenges. 
    """

/**
 * rendering of 4 paragraphs with info about SAVE
 */
fun ChildrenBuilder.renderGeneralInfoPage() {
    div {
        style = jso {
            color = "rgb(6, 7, 89)".unsafeCast<Color>()
        }
        className = ClassName("row justify-content-center")

        div {
            className = ClassName("row justify-content-center mt-5")
            text("Framework", SAVE_CLI_TEXT)
            text("Easy CI", EASY_CI_TEXT)
        }

        div {
            className = ClassName("row justify-content-center")
            text("Benchmarks", BENCHMARKS_TEXT)
            text("Contests", CONTESTS_TEXT)
        }
    }
}

private fun ChildrenBuilder.text(title: String, textStr: String) {
    div {
        className = ClassName("col-3  mx-3")
        h1 {
            style = jso {
                textAlign = TextAlign.center
            }
            +title
        }
        p {
            +textStr
        }
    }
}
