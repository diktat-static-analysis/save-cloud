package com.saveourtool.save.backend.repository

import com.saveourtool.common.entities.Agent
import com.saveourtool.common.entities.AgentStatus
import com.saveourtool.common.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

/**
 * JPA repository for agent statuses.
 */
@Repository
interface AgentStatusRepository : BaseEntityRepository<AgentStatus> {
    /**
     * Find latest [AgentStatus] for agent with provided [containerId]
     *
     * @param containerId id of an agent
     * @return [AgentStatus] of an agent
     */
    fun findTopByAgentContainerIdOrderByEndTimeDescIdDesc(containerId: String): AgentStatus?

    /**
     * Find [AgentStatus] by [Agent] which is first by [AgentStatus.startTime]
     *
     * @param agent
     * @return [AgentStatus] which fits to query
     */
    fun findTopByAgentOrderByStartTimeAsc(agent: Agent): AgentStatus?

    /**
     * Find [AgentStatus] by [Agent] which is last by [AgentStatus.endTime]
     *
     * @param agent
     * @return [AgentStatus] which fits to query
     */
    fun findTopByAgentOrderByEndTimeDesc(agent: Agent): AgentStatus?

    /**
     * @param ids list of agent id
     */
    @Transactional
    fun deleteByAgentIdIn(ids: List<Long>)
}
