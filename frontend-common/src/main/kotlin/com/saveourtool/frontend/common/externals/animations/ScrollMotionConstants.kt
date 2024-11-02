/**
 * Prepared animation styles
 */

@file:Suppress("MAGIC_NUMBER")

package com.saveourtool.frontend.common.externals.animations

val zoomInScrollOut = batch(fade(), zoomIn(), sticky())

val fadeUpTopLeft = batch(fade(), move(-300, 0, -300, 0), sticky(40, 25))
val fadeUpBottomLeft = batch(fade(), move(-300, 0, -300, 0), sticky(40, 75))

val fadeUpTopRight = batch(fade(), move(300, 0, 300, 0), sticky(70, 25))
val fadeUpBottomRight = batch(fade(), move(300, 0, 300, 0), sticky(70, 75))

val moveUpFromBottom = batch(fade(), move(dy = 1000), sticky(55))

val simplyFade = batch(fade(), sticky())
