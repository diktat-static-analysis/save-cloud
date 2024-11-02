package com.saveourtool.common.demo.cpg

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @property cpgGraph graph obtained from Cpg tool run
 * @property query query id SQL request to a NEO4
 * @property logs execution logs
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class CpgResult(
    val cpgGraph: CpgGraph,
    val query: String,
    val logs: List<String>,
) {
    companion object {
        val empty = CpgResult(CpgGraph.placeholder, "", emptyList())
    }
}
