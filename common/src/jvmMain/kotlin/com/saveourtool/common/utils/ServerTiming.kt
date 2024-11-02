package com.saveourtool.common.utils

import kotlin.time.Duration

/**
 * @property id the unique identifier of this metric. Can't contain `';'`, `','`,
 *   or `' '`.
 * @property description the description that will be displayed in the browser's
 *   _Development Tools_, under Network -> Timing. If the [description] is `null`,
 *   the _Development Tools_ will display the [id] instead.
 * @property duration the server-side duration.
 */
data class ServerTiming(
    val id: String,
    val description: String? = null,
    val duration: Duration
) {
    init {
        require(id.asSequence().none { char ->
            char in sequenceOf(FIELD_SEPARATOR, ',', ' ')
        })
    }

    /**
     * @return the string representation of this timing, either
     *   `id;desc="Description";dur=123.456`, or
     *   `id;dur=123.456`. The duration is given in milliseconds, with an
     *   optional fractional part.
     */
    @Suppress(
        "MagicNumber",
        "FLOAT_IN_ACCURATE_CALCULATIONS",
    )
    override fun toString(): String =
            listOf(
                id,
                description?.let { "desc=\"$it\"" },
                "dur=${duration.inWholeMicroseconds / 1e3}"
            )
                .asSequence()
                .filterNotNull()
                .joinToString(separator = ";")

    private companion object {
        private const val FIELD_SEPARATOR = ';'
    }
}
