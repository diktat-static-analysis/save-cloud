@file:Suppress(
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "MagicNumber",
)

package com.saveourtool.common.cvsscalculator.v2

@Suppress("VARIABLE_NAME_INCORRECT_FORMAT")
val accessV = mapOf(
    AccessVectorType.NETWORK.value to 1.0f,
    AccessVectorType.ADJACENT_NETWORK.value to 0.646f,
    AccessVectorType.LOCAL.value to 0.395f,
)

@Suppress("VARIABLE_NAME_INCORRECT_FORMAT")
val accessC = mapOf(
    AccessComplexityType.LOW.value to 0.71f,
    AccessComplexityType.MEDIUM.value to 0.61f,
    AccessComplexityType.HIGH.value to 0.35f,
)

val auth = mapOf(
    AuthenticationType.MULTIPLE.value to 0.45f,
    AuthenticationType.SINGLE.value to 0.56f,
    AuthenticationType.NONE.value to 0.704f,
)

val ciaImpact = mapOf(
    CiaTypeV2.NONE.value to 0f,
    CiaTypeV2.PARTIAL.value to 0.275f,
    CiaTypeV2.COMPLETE.value to 0.66f,
)
