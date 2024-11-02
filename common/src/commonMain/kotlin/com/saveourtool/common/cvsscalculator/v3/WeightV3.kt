@file:Suppress(
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "MagicNumber",
)

package com.saveourtool.common.cvsscalculator.v3

val av = mapOf(
    AttackVectorType.NETWORK.value to 0.85f,
    AttackVectorType.ADJACENT_NETWORK.value to 0.62f,
    AttackVectorType.LOCAL.value to 0.55f,
    AttackVectorType.PHYSICAL.value to 0.2f,
)

val ac = mapOf(
    AttackComplexityType.LOW.value to 0.77f,
    AttackComplexityType.HIGH.value to 0.44f,
)

// if Scope / Modified Scope is Unchanged
val prSu = mapOf(
    PrivilegesRequiredType.NONE.value to 0.85f,
    PrivilegesRequiredType.LOW.value to 0.62f,
    PrivilegesRequiredType.HIGH.value to 0.27f,
)

// if Scope / Modified Scope is Changed
val prSc = mapOf(
    PrivilegesRequiredType.NONE.value to 0.85f,
    PrivilegesRequiredType.LOW.value to 0.68f,
    PrivilegesRequiredType.HIGH.value to 0.50f,
)

val scope = mapOf(
    ScopeType.CHANGED.value to prSc,
    ScopeType.UNCHANGED.value to prSu,
)

val ui = mapOf(
    UserInteractionType.NONE.value to 0.85f,
    UserInteractionType.REQUIRED.value to 0.62f,
)

val cia = mapOf(
    CiaType.HIGH.value to 0.56f,
    CiaType.LOW.value to 0.22f,
    CiaType.NONE.value to 0f,
)
