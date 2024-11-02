package com.saveourtool.save.orchestrator.kafka

import com.saveourtool.common.kafka.TestExecutionTaskDto
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload

/**
 * @property topic
 * @property groupId
 */
@KafkaListener(id = "#{__listener.topic}.listener", topics = ["#{__listener.topic}"], groupId = "#{__listener.groupId}")
class AgentKafkaListener(
    val topic: String,
    val groupId: String
) {
    /**
     * @param data
     * @param messageId
     * @param partition
     * @param topic
     * @param ts
     */
    @KafkaHandler
    fun listen(
        @Payload data: TestExecutionTaskDto?,
        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) messageId: String?,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String?,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) ts: Long
    ) {
        log.info(
            "Received request. messageId: {} , partition: {} , topic: {}, ts: {}, payload: {}",
            messageId,
            partition,
            topic,
            ts,
            data
        )
    }
    companion object {
        private val log = LoggerFactory.getLogger(AgentKafkaListener::class.java)
    }
}
