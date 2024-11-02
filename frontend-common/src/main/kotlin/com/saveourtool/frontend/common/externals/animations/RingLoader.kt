@file:Suppress("FILE_NAME_MATCH_CLASS")
@file:JsModule("react-spinners")
@file:JsNonModule

package com.saveourtool.frontend.common.externals.animations

import react.*

@JsName("LoaderSizeProps")
external interface LoaderSizeProps : PropsWithChildren

/**
 * @param options
 * @return react element with a ring loader
 */
@JsName("RingLoader")
external fun ringLoader(options: dynamic = definedExternally): ReactElement<LoaderSizeProps>?
