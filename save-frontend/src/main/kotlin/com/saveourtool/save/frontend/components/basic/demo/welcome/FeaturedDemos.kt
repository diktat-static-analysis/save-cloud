/**
 * Component for approved demos render
 */

package com.saveourtool.save.frontend.components.basic.demo.welcome

import com.saveourtool.common.demo.DemoDto
import com.saveourtool.frontend.common.components.basic.AVATAR_BASE_URL
import com.saveourtool.frontend.common.components.basic.carousel
import com.saveourtool.frontend.common.utils.*
import com.saveourtool.frontend.common.utils.noopResponseHandler

import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.strong
import react.router.dom.Link
import react.useEffect
import react.useState
import web.cssom.ClassName

internal val featuredDemos: FC<Props> = FC {
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
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        ).decodeFromJsonString()
        setFeaturedDemos(demos)
    }

    val (avatars, setAvatars) = useState<Map<String, String>>(emptyMap())
    useEffect(featuredDemos) {
        setAvatars {
            featuredDemos.associate { demoDto ->
                with(demoDto.projectCoordinates) {
                    organizationName to "$AVATAR_BASE_URL/organizations/$organizationName"
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
                    "/img/undraw_join_re_w1lh.svg"
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
                    Link {
                        className = ClassName("text-dark")
                        to = "/demo/${demoDto.projectCoordinates}"
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
