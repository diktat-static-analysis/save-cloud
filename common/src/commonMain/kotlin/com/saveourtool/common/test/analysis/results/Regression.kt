package com.saveourtool.common.test.analysis.results

import kotlinx.serialization.Serializable

/**
 * @property detailMessage the detail message string of this result.
 */
@Serializable
data class Regression(override val detailMessage: String = "") : IrregularTest()
