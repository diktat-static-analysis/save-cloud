package com.saveourtool.common.utils

/**
 * Adds support for the
 * [`Server-Timing`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing)
 * header.
 *
 * @property response the HTTP response.
 * @property timings the server-side timings.
 * @see ResponseWithTiming.Companion.invoke
 */
data class ResponseWithTiming<T : Any>(
    val response: T,
    val timings: Array<out ServerTiming>
) {
    init {
        require(timings.isNotEmpty()) {
            "At least one timing required"
        }
    }

    override fun equals(other: Any?): Boolean =
            other is ResponseWithTiming<*> &&
                    response == other.response &&
                    timings contentEquals other.timings

    override fun hashCode(): Int =
            response.hashCode() xor timings.contentHashCode()

    companion object {
        /**
         * Adds support for the
         * [`Server-Timing`](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing)
         * header.
         *
         * @param response the HTTP response.
         * @param timings the server-side timings.
         * @return the [response] with [timings] added.
         */
        operator fun <T : Any> invoke(
            response: T,
            vararg timings: ServerTiming,
        ): ResponseWithTiming<T> =
                ResponseWithTiming(response, timings)
    }
}
