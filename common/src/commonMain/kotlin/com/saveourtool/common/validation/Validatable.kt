/**
 * Module that defines an interface for objects validation.
 */

package com.saveourtool.common.validation

/**
 * [Validatable] should be implemented by all the classes where we need validation
 */
interface Validatable {
    /**
     * @return true if object is valid, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun validate(): Boolean = true
}
