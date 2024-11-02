/**
 * Particles - is a special animation that makes background of pages dynamic.
 * https://github.com/matteobruni/tsparticles
 */

@file:JsModule("react-tsparticles")
@file:JsNonModule

package com.saveourtool.frontend.common.externals.animations

import react.*

@JsName("default")
external class Particles : Component<ParticlesProps, State> {
    override fun render(): ReactElement<ParticlesProps>?
}

@JsName("IParticlesProps")
external interface ParticlesProps : PropsWithChildren {
    var id: String
    var url: String
}
