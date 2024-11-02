@file:Suppress("FILE_NAME_MATCH_CLASS")
@file:JsModule("react-markdown")
@file:JsNonModule

package com.saveourtool.frontend.common.externals.markdown

import react.*

/**
 * Options for [reactMarkdown]
 */
@JsName("ReactMarkdownOptions")
external interface ReactMarkdownProps : PropsWithChildren

/**
 * External declaration of ReactMarkdown react component
 *
 * @param options
 * @return special div that can be filled with text
 */
@JsName("default")
external fun reactMarkdown(options: dynamic = definedExternally): ReactElement<ReactMarkdownProps>?
