/**
 * Carousel extension function
 */

package com.saveourtool.frontend.common.components.basic

import com.saveourtool.frontend.common.themes.Colors
import js.core.jso
import react.CSSProperties
import react.ChildrenBuilder
import react.dom.aria.AriaRole
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ol
import react.dom.html.ReactHTML.span
import web.cssom.*

private const val INVERT_TO_OPPOSITE = 100

/**
 * @param items
 * @param carouselBodyId
 * @param styles
 * @param outerClasses
 * @param isIndicated if true, carousel card indicator is displayed in a bottom of a carousel
 * @param displayItem
 */
@Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
fun <T : Any> ChildrenBuilder.carousel(
    items: List<T>,
    carouselBodyId: String,
    styles: CSSProperties? = null,
    outerClasses: String = "",
    isIndicated: Boolean = true,
    displayItem: ChildrenBuilder.(T) -> Unit,
) {
    div {
        className = ClassName("carousel slide flex-md-row box-shadow $outerClasses")
        if (isIndicated && items.size > 1) {
            ol {
                className = ClassName("carousel-indicators mt-2 mb-2")
                items.forEachIndexed { index, _ ->
                    li {
                        if (index == 0) {
                            className = ClassName("active")
                        }
                        style = jso { this.backgroundColor = Colors.GREY.unsafeCast<Color>() }
                        asDynamic()["data-target"] = "#$carouselBodyId"
                        asDynamic()["data-slide-to"] = index
                        style = jso {
                            borderRadius = 1.em
                            height = 0.em
                            width = 0.em
                            border = "0.25rem solid #808080".unsafeCast<Border>()
                        }
                    }
                }
            }
        }
        style = styles
        id = carouselBodyId
        asDynamic()["data-ride"] = "carousel"

        div {
            className = ClassName("carousel-inner my-auto")
            items.forEachIndexed { i, item ->
                val classes = if (i == 0) "active" else ""
                slide(classes) { displayItem(item) }
            }
        }
        if (items.size > 1) {
            carouselArrows(carouselBodyId)
        }
    }
}

/**
 * @param classes
 * @param displayItem
 */
fun ChildrenBuilder.slide(classes: String, displayItem: ChildrenBuilder.() -> Unit) {
    div {
        className = ClassName("carousel-item $classes")
        div {
            className = ClassName("row mt-auto")
            displayItem()
        }
    }
}

/**
 * @param carouselBodyId
 */
fun ChildrenBuilder.carouselArrows(carouselBodyId: String) {
    a {
        style = jso { filter = invert(INVERT_TO_OPPOSITE) }
        className = ClassName("carousel-control-prev ")
        href = "#$carouselBodyId"
        role = "button".unsafeCast<AriaRole>()
        asDynamic()["data-slide"] = "prev"
        span { className = ClassName("carousel-control-prev-icon") }
    }
    a {
        style = jso { filter = invert(INVERT_TO_OPPOSITE) }
        className = ClassName("carousel-control-next")
        href = "#$carouselBodyId"
        role = "button".unsafeCast<AriaRole>()
        asDynamic()["data-slide"] = "next"
        span { className = ClassName("carousel-control-next-icon") }
    }
}
