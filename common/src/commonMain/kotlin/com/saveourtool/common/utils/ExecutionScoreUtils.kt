/**
 * Utilities for execution score calculation
 */

@file:Suppress(
    "MatchingDeclarationName",
    "MagicNumber",
    "MAGIC_NUMBER",
    "FILE_NAME_MATCH_CLASS"
)

package com.saveourtool.common.utils

import com.saveourtool.common.execution.ExecutionDto
import com.saveourtool.common.execution.TestingType

/**
 * Set of types for different ways of score calculation
 */
enum class ScoreType {
    // TODO: what else algorithms do we need?

    /** Calculate score via F-measure.  */
    F_MEASURE,
    ;
}

/**
 * @return precision rate
 */
fun ExecutionDto.getPrecisionRate() = calculateRate(matchedChecks, matchedChecks + unexpectedChecks) ?: 0

/**
 * @return recall rate
 */
fun ExecutionDto.getRecallRate() = calculateRate(matchedChecks, matchedChecks + unmatchedChecks) ?: 0

/**
 * @param scoreType
 * @return score according execution [type]
 */
fun ExecutionDto.calculateScore(scoreType: ScoreType): Double = when (type) {
    TestingType.CONTEST_MODE -> calculateScoreForContestMode(scoreType)
    // TODO: how to calculate score for other types?
    else -> 0.0
}

/**
 * @return true if value is in range (0, 100); false otherwise
 */
fun Double.isValidScore() = this.toInt().isValidScore()

/**
 * @return true if value is in range (0, 100); false otherwise
 */
fun Int.isValidScore() = this in 0..100

private fun ExecutionDto.calculateScoreForContestMode(scoreType: ScoreType): Double = when (scoreType) {
    ScoreType.F_MEASURE -> calculateFmeasure()
}

private fun ExecutionDto.calculateFmeasure(): Double {
    val denominator = (getPrecisionRate() + getRecallRate())
    return if (denominator == 0) {
        0.0
    } else {
        (2 * getPrecisionRate() * getRecallRate()) / (getPrecisionRate() + getRecallRate()).toDouble()
    }
}

/**
 * @param numerator
 * @param denominator
 * @return rate based on [numerator] and [denominator]
 */
fun calculateRate(numerator: Long, denominator: Long) = denominator.takeIf { it > 0 }
    ?.run { numerator.toDouble() / denominator }
    ?.let { it * 100 }
    ?.toInt()
