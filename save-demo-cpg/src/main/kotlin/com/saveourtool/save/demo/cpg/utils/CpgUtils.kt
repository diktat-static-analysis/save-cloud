@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.demo.cpg.utils

import com.saveourtool.common.demo.cpg.*
import com.saveourtool.save.demo.cpg.entity.TreeSitterNode

import de.fraunhofer.aisec.cpg.graph.Node
import org.neo4j.ogm.response.model.NodeModel
import org.neo4j.ogm.response.model.RelationshipModel

import kotlinx.serialization.ExperimentalSerializationApi

/**
 * @return [CpgNode] from [Node]
 */
@ExperimentalSerializationApi
fun Node.toCpgNode() = CpgNode(
    id.toString(),
    CpgNodeAttributes(
        name.localName,
        additionalInfo = CpgNodeAdditionalInfo(
            code = code,
            comment = comment,
            location = location?.toString(),
            file = file,
            isInferred = isInferred,
            isImplicit = isImplicit,
            argumentIndex = argumentIndex,
        )
    ),
)

/**
 * @return [CpgNode] from [NodeModel]
 */
@ExperimentalSerializationApi
fun TreeSitterNode.toCpgNode() = CpgNode(
    id.toString(),
    CpgNodeAttributes(
        localName,
        additionalInfo = CpgNodeAdditionalInfo(
            code = code,
            comment = "N/A",
            location = location.toString(),
            file = location.fileName,
            isInferred = false,
            isImplicit = false,
            argumentIndex = -1,
        )
    ),
)

/**
 * @return [CpgEdge] from [RelationshipModel]
 */
@ExperimentalSerializationApi
fun RelationshipModel.toCpgEdge() = CpgEdge(
    id.toString(),
    startNode.toString(),
    endNode.toString(),
    CpgEdgeAttributes(
        this.type,
    ),
)
