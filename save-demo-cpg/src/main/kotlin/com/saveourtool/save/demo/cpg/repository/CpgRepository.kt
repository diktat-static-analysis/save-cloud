package com.saveourtool.save.demo.cpg.repository

import com.saveourtool.common.demo.cpg.CpgGraph
import com.saveourtool.common.utils.getLogger
import com.saveourtool.common.utils.info
import com.saveourtool.save.demo.cpg.config.ConfigProperties
import com.saveourtool.save.demo.cpg.entity.DemoQuery
import com.saveourtool.save.demo.cpg.entity.TreeSitterNode
import com.saveourtool.save.demo.cpg.utils.*

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import org.neo4j.ogm.response.model.RelationshipModel
import org.neo4j.ogm.session.Session
import org.neo4j.ogm.session.event.Event
import org.neo4j.ogm.session.event.EventListener
import org.neo4j.ogm.session.event.EventListenerAdapter
import org.slf4j.Logger
import org.springframework.stereotype.Repository

import kotlinx.serialization.ExperimentalSerializationApi

/**
 * @property configProperties
 */
@Repository
class CpgRepository(
    val configProperties: ConfigProperties,
) {
    /**
     * @param result CPG result to save it in NEO4J
     * @return ID of [DemoQuery]
     */
    fun save(result: TranslationResult): Long {
        log.info { "Using import depth: $DEFAULT_SAVE_DEPTH" }
        log.info {
            "Count base nodes to save [components: ${result.components.size}, additionalNode: ${result.additionalNodes.size}]"
        }

        return doSave { session ->
            session.save(result.components, DEFAULT_SAVE_DEPTH)
            session.save(result.additionalNodes, DEFAULT_SAVE_DEPTH)
        }
    }

    /**
     * @param result CPG result to save it in NEO4J
     * @return ID of [DemoQuery]
     */
    fun save(result: Collection<TreeSitterNode>): Long {
        log.info { "Using import depth: $DEFAULT_SAVE_DEPTH" }
        log.info {
            "Count base nodes to save [tree-sitter: ${result.size}]"
        }

        return doSave { session ->
            session.save(result, DEFAULT_SAVE_DEPTH)
        }
    }

    private fun doSave(saveAction: (Session) -> Unit): Long = connect().use { session ->
        session.beginTransaction().use { transaction ->
            val nodeIds: MutableSet<Long> = mutableSetOf()
            val eventListener = createEventListener(nodeIds)
            session.register(eventListener)
            saveAction(session)
            val demoQuery = DemoQuery(nodeIds = nodeIds)
            session.dispose(eventListener)
            session.save(demoQuery)
            transaction?.commit()
            demoQuery.requiredId()
        }
    }

    /**
     * @param queryId ID of [DemoQuery]
     * @return result of CPG
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun getGraph(queryId: Long): CpgGraph {
        val (nodes, edges) = connect().use { session ->
            session.getNodes<Node>(queryId) to session.getEdges(queryId)
        }
        return CpgGraph(nodes = nodes.map { it.toCpgNode() }.toList(), edges = edges.map { it.toCpgEdge() }.toList())
    }

    /**
     * @param queryId ID of [DemoQuery]
     * @return result of CPG
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun getGraphForTreeSitter(queryId: Long): CpgGraph {
        val (nodes, edges) = connect().use { session ->
            session.getNodes<TreeSitterNode>(queryId) to session.getEdges(queryId)
        }
        return CpgGraph(nodes = nodes.map { it.toCpgNode() }.toList(), edges = edges.map { it.toCpgEdge() }.toList())
    }

    private inline fun <reified T : Any> Session.getNodes(queryId: Long) = query(
        T::class.java, queryNodes, mapOf(QUERY_ID_PARAMETER_NAME to queryId)
    ).toList()

    private fun Session.getEdges(queryId: Long) = query("""
        CALL {
          MATCH (q:DemoQuery)
          WHERE ID(q) = $QUERY_ID_PARAMETER_PLACEHOLDER
          RETURN q.nodeIds AS nodeIds
        }
        WITH nodeIds
        MATCH (n1)-[r]->(n2)
        WHERE ID(n1) IN nodeIds AND ID(n2) IN nodeIds
        RETURN r, n1, n2
    """.trimIndent(), mapOf(QUERY_ID_PARAMETER_NAME to queryId)
    )
        .asSequence()
        .map {
            it.values
        }
        .flatten()
        .filterIsInstance<RelationshipModel>()

    private fun connect(): SessionWithFactory = retry(MAX_RETRIES, TIME_BETWEEN_CONNECTION_TRIES) {
        tryConnect(
            configProperties.uri,
            configProperties.authentication.username,
            configProperties.authentication.password,
            Node::class.java.packageName, Language::class.java.packageName,
            DemoQuery::class.java.packageName,
        )
    }

    private fun createEventListener(
        nodeIds: MutableSet<Long>,
    ): EventListener = object : EventListenerAdapter() {
        override fun onPostSave(event: Event?) {
            requireNotNull(event?.`object`) {
                "Missed event or object in ${EventListenerAdapter::class.simpleName}"
            }
                .let { entity ->
                    when (entity) {
                        is Node -> nodeIds.add(requireNotNull(entity.id) {
                            "Object (${entity.javaClass.simpleName}) $entity doesn't have ID"
                        })
                        is TreeSitterNode -> nodeIds.add(entity.requiredId())
                        is PropertyEdge<*> -> log.debug("Skip for now ${entity.javaClass.simpleName}")
                        else -> log.error("Object type ${entity.javaClass.simpleName} is not supported")
                    }
                }
        }
    }

    companion object {
        private val log: Logger = getLogger<CpgRepository>()
        private const val DEFAULT_SAVE_DEPTH = -1
        private const val MAX_RETRIES = 10
        private const val QUERY_ID_PARAMETER_NAME = "queryId"
        private const val QUERY_ID_PARAMETER_PLACEHOLDER = "\$$QUERY_ID_PARAMETER_NAME"
        private const val TIME_BETWEEN_CONNECTION_TRIES: Long = 6000
        private val queryNodes = """
            CALL {
              MATCH (q:DemoQuery)
              WHERE ID(q) = $QUERY_ID_PARAMETER_PLACEHOLDER
              RETURN q.nodeIds AS nodeIds
            }
            WITH nodeIds
            MATCH (n)
            WHERE ID(n) IN nodeIds
            RETURN n
        """.trimIndent()

        /**
         * @param queryId
         * @return a query for nodes by queryId
         */
        fun getQueryForNodes(queryId: Long): String = queryNodes.replace(QUERY_ID_PARAMETER_PLACEHOLDER, queryId.toString())
            .lineSequence()
            .joinToString(" ")
            .replace("  ", " ")
    }
}
