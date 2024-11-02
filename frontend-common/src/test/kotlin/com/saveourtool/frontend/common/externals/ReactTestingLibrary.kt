/**
 * Declarations from https://github.com/testing-library/dom-testing-library
 * https://github.com/testing-library/react-testing-library/blob/main/types/index.d.ts
 */

@file:JsModule("@testing-library/react")
@file:JsNonModule
@file:Suppress(
    "USE_DATA_CLASS",
    "MISSING_KDOC_ON_FUNCTION",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_TOP_LEVEL",
    "KDOC_NO_EMPTY_TAGS",
    "KDOC_WITHOUT_PARAM_TAG",
    "KDOC_WITHOUT_RETURN_TAG",
)

package com.saveourtool.frontend.common.externals

import web.html.HTMLElement
import react.Props
import react.ReactElement
import kotlin.js.Promise

/**
 * https://testing-library.com/docs/queries/about/#screen
 * https://github.com/testing-library/dom-testing-library/blob/main/types/screen.d.ts
 */
external val screen: BoundFunctions

external class RenderResult {
    var container: dynamic
}

/**
 * https://github.com/testing-library/dom-testing-library/blob/main/types/queries.d.ts
 */
external class BoundFunctions {
    fun getByText(text: String, options: dynamic = definedExternally): HTMLElement

    /**
     * https://testing-library.com/docs/queries/byrole
     */
    fun getByRole(vararg args: dynamic): HTMLElement

    fun findByText(text: String, options: dynamic = definedExternally, waitForOptions: dynamic = definedExternally): Promise<HTMLElement>

    fun queryByText(text: String, options: dynamic = definedExternally): HTMLElement?
}

external fun <P : Props> render(ui: ReactElement<P>, options: dynamic = definedExternally): RenderResult

external fun <T> waitForElementToBeRemoved(elem: T, options: dynamic = definedExternally): Promise<Unit>
