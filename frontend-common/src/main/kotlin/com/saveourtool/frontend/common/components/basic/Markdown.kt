/**
 * Simple markdown render
 */

package com.saveourtool.frontend.common.components.basic

import com.saveourtool.frontend.common.externals.markdown.reactMarkdown
import js.core.jso
import react.ChildrenBuilder

/**
 * Simple [ChildrenBuilder] extension function to display [text] as markdown
 *
 * @param text text that should be interpreted as text in Markdown format
 * @param classes class names that should be applied to high-level div
 */
fun ChildrenBuilder.markdown(text: String, classes: String? = null) {
    +reactMarkdown(jso {
        this.children = text
        this.className = classes
    })
}
