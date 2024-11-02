/**
 * Very impressive and beautiful library with animation for scrolling:
 * https://github.com/1000ship/react-scroll-motion
 */

@file:JsModule("react-scroll-motion")
@file:JsNonModule

package com.saveourtool.frontend.common.externals.animations

import react.FC
import react.PropsWithChildren

@JsName("ScrollContainer")
external val scrollContainer: FC<ScrollContainerProps>

@JsName("ScrollPage")
external val scrollPage: FC<ScrollPageProps>

@JsName("Animator")
external val animator: FC<AnimatorProps>

// =================core==========================
@JsName("ScrollContainerProps")
external interface ScrollContainerProps : PropsWithChildren {
    var snap: String
    var scrollParent: dynamic
}

@JsName("ScrollPageProps")
external interface ScrollPageProps : PropsWithChildren {
    var debugBorder: Boolean
    var page: dynamic
}

@JsName("AnimatorProps")
external interface AnimatorProps : PropsWithChildren {
    var animation: Animation
}

@JsName("Animation")
external interface Animation

/**
 * @param left position on the page (x)
 * @param top position on the page (y)
 * @return animation
 */
// =================sticky animation==========================
@JsName("Sticky")
external fun sticky(
    left: Number = definedExternally,
    top: Number = definedExternally,
): Animation

/**
 * @param left position on the page (x)
 * @param top position on the page (y)
 * @return animation
 */
@JsName("StickyIn")
external fun stickyIn(
    left: Number = definedExternally,
    top: Number = definedExternally,
): Animation

/**
 * @param left position on the page (x)
 * @param top position on the page (y)
 * @return animation
 */
@JsName("StickyOut")
external fun stickyOut(
    left: Number = definedExternally,
    top: Number = definedExternally,
): Animation

// =================fade animation==========================

/**
 * @param from initial opacity
 * @param to final opacity
 * @return animation
 */
@JsName("Fade")
external fun fade(
    from: Number = definedExternally,
    to: Number = definedExternally,
): Animation

/**
 * @param from initial opacity
 * @param to final opacity
 * @return animation
 */
@JsName("FadeIn")
external fun fadeIn(
    from: Number = definedExternally,
    to: Number = definedExternally
): Animation

/**
 * @param from initial opacity
 * @param to final opacity
 * @return animation
 */
@JsName("FadeOut")
external fun fadeOut(
    from: Number = definedExternally,
    to: Number = definedExternally
): Animation

// =================move animation==========================

/**
 * @param dx initial x coordinate
 * @param dy initial y coordinate
 * @param outDx target x coordinate
 * @param outDy target y coordinate
 * @return animation
 */
@JsName("Move")
external fun move(
    dx: Number = definedExternally,
    dy: Number = definedExternally,
    outDx: Number = definedExternally,
    outDy: Number = definedExternally
): Animation

/**
 * @param dx initial x coordinate
 * @param dy initial y coordinate
 * @return animation
 */
@JsName("MoveIn")
external fun moveIn(
    dx: Number = definedExternally,
    dy: Number = definedExternally
): Animation

/**
 * @param dx initial x coordinate
 * @param dy initial y coordinate
 * @return animation
 */
@JsName("MoveOut")
external fun moveOut(
    dx: Number = definedExternally,
    dy: Number = definedExternally
): Animation

// =================zoom animation==========================

/**
 * @param from initial zoom size
 * @param to final zoom size
 * @return animation
 */
@JsName("Zoom")
external fun zoom(
    from: Number = definedExternally,
    to: Number = definedExternally,
): Animation

/**
 * @param from initial zoom size
 * @param to final zoom size
 * @return animation
 */
@JsName("ZoomIn")
external fun zoomIn(
    from: Number = definedExternally,
    to: Number = definedExternally,
): Animation

/**
 * @param from initial zoom size
 * @param to final zoom size
 * @return animation
 */
@JsName("ZoomOut")
external fun zoomOut(
    from: Number = definedExternally,
    to: Number = definedExternally,
): Animation

/**
 * @param animations
 * @return batched and merged animation from the list of several animations
 */
@JsName("batch")
external fun batch(vararg animations: Animation): Animation
