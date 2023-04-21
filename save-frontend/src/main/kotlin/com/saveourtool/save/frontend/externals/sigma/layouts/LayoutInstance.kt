package com.saveourtool.save.frontend.externals.sigma.layouts

/**
 * Wrapper for useLayoutRandom returning value to allow unpacking a pair (has [component1] and [component2])
 */
class LayoutInstance private constructor() {
    /**
     * @return a function that execute the layout on the sigma's graph (but doesn't modify it)
     * and returns you a map of position where the key is the node key
     */
    operator fun component1(): () -> dynamic = asDynamic()["positions"] as Function0<dynamic>

    /**
     * @return a function that execute the layout on the sigma's graph and save the position of nodes in it
     */
    operator fun component2(): () -> Unit = asDynamic()["assign"] as Function0<Unit>
}
