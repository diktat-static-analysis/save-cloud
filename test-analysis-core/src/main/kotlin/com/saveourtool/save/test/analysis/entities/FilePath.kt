package com.saveourtool.save.test.analysis.entities

import com.saveourtool.common.entities.Test

/**
 * File path, intended to be assignment-incompatible with the regular string.
 *
 * @property value the underlying string value.
 */
@JvmInline
value class FilePath(val value: String) {
    override fun toString(): String =
            value
}

/**
 * @return the file path of this test.
 */
fun Test.filePath(): FilePath =
        FilePath(filePath)
