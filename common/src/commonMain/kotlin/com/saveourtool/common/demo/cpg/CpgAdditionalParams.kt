package com.saveourtool.common.demo.cpg

import com.saveourtool.common.utils.Languages
import kotlinx.serialization.Serializable

/**
 * Data class that represents all additional params required to run CpgDemo
 *
 * @property engine CPG engine
 * @property language language that the code for demo was written in
 */
@Serializable
data class CpgAdditionalParams(
    val engine: CpgEngine,
    val language: Languages = Languages.KOTLIN,
)
