/**
 * Component for approved demos render
 */

package com.saveourtool.save.frontend.components.basic.demo.welcome

import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.frontend.components.basic.carousel
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.v1

import react.VFC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.strong
import react.useEffect
import react.useState
import web.cssom.ClassName

internal val featuredDemos = VFC {
    val (featuredDemos, setFeaturedDemos) = useState(
        listOf(
            DemoDto.emptyForProject("saveourtool", "Diktat"),
            DemoDto.emptyForProject("Pinterest", "ktlint")
        )
    )
    useRequest {
        val demos: List<DemoDto> = get(
            url = "$demoApiUrl/stats/featured",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        ).decodeFromJsonString()
        setFeaturedDemos(demos)
    }

    val (avatars, setAvatars) = useState<Map<String, String>>(emptyMap())
    useEffect(featuredDemos) {
        setAvatars {
            featuredDemos.associate { demoDto ->
                with(demoDto.projectCoordinates) {
                    organizationName to "/api/$v1/avatar/organizations/$organizationName"
                }
            }
        }
    }

    carousel(featuredDemos, "demoCarousel", outerClasses = "p-2 card") { demoDto ->
        div {
            className = ClassName("col-3 ml-auto justify-content-center")
            img {
                className = ClassName("img-fluid")
                // FixMe: we need to have information about the programming language in demo in order to show label
                src = avatars.getOrElse(demoDto.projectCoordinates.organizationName) {
                    "img/undraw_join_re_w1lh.svg"
                }
            }
        }
        div {
            className = ClassName("col-6 mr-auto d-flex align-items-center")
            div {
                className = ClassName("card-body d-flex flex-column")
                strong {
                    className = ClassName("d-inline-block mb-2 text-info")
                    +"Featured Demo"
                }
                h3 {
                    className = ClassName("mb-0")
                    a {
                        className = ClassName("text-dark")
                        href = "#/demo/${demoDto.projectCoordinates}"
                        +demoDto.projectCoordinates.toString()
                    }
                }
                h1 {
                    +demoDto.runCommands.keys.joinToString(", ")
                }
            }
        }
    }
}
