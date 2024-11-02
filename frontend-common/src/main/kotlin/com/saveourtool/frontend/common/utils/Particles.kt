/**
 * Particles - effect with flying objects on the background
 */

package com.saveourtool.frontend.common.utils

import com.saveourtool.frontend.common.externals.animations.Particles
import react.ChildrenBuilder
import react.react

/**
 * @param enabled true if particles enabled
 */
fun ChildrenBuilder.particles(
    enabled: Boolean = true,
) {
    if (enabled) {
        // FixMe: Note that they block user interactions.
        // Particles are superimposed on top of the view in some transitions
        // https://github.com/matteobruni/tsparticles/discussions/4489
        Particles::class.react {
            id = "tsparticles"
            url = "${kotlinx.browser.window.location.origin}/particles.json"
        }
    }
}
