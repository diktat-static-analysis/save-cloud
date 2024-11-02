package com.saveourtool.common.test.analysis.results

import kotlinx.serialization.Serializable

/**
 * The analysis result returned by `Algorithm` or `TestAnalysisService.analyze`.
 *
 * Because [Kotlin/kotlinx.serialization#1576](https://github.com/Kotlin/kotlinx.serialization/issues/1576)
 * is only fixed in 1.8.0, this is a `sealed class` (should be a `sealed interface` instead).
 *
 * See also:
 * - [Kotlin/kotlinx.serialization#1576](https://github.com/Kotlin/kotlinx.serialization/issues/1576)
 * - [Kotlin/kotlinx.serialization#1869](https://github.com/Kotlin/kotlinx.serialization/issues/1869)
 */
@Serializable
sealed class AnalysisResult
