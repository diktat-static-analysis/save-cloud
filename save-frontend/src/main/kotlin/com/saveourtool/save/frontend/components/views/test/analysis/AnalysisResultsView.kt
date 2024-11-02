@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.test.analysis

import com.saveourtool.common.test.analysis.results.AnalysisResult

import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import web.cssom.ClassName

/**
 * Displays a list of [AnalysisResult].
 *
 * @see AnalysisResult
 * @see AnalysisResultsProps
 */
val analysisResultsView: FC<AnalysisResultsProps> = FC { props ->
    div {
        className = ClassName("analysisResults")

        val results = props.analysisResults

        when (results.size) {
            0 -> noDataAvailable()

            1 -> analysisResultView {
                analysisResult = results[0]
            }

            else -> ul {
                style = listStyle

                results.forEach { result ->
                    li {
                        analysisResultView {
                            analysisResult = result
                        }
                    }
                }
            }
        }
    }
}

/**
 * Properties of [analysisResultsView].
 *
 * @see analysisResultsView
 */
external interface AnalysisResultsProps : Props {
    /**
     * The analysis result to render.
     */
    var analysisResults: List<AnalysisResult>
}
