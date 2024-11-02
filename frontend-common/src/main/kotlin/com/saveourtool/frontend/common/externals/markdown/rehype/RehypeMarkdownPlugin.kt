@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")
@file:JsModule("rehype-highlight")
@file:JsNonModule

package com.saveourtool.frontend.common.externals.markdown.rehype

/**
 * Plugin for syntax highlighting in code blocks
 *
 * @param options
 * @return dynamic
 */
@JsName("default")
external fun rehypeHighlightPlugin(options: dynamic = definedExternally): dynamic
