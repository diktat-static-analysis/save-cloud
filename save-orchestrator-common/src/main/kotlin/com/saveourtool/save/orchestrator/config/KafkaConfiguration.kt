package com.saveourtool.save.orchestrator.config

import com.saveourtool.common.kafka.TestExecutionTaskDto
import com.saveourtool.save.orchestrator.kafka.AgentKafkaListener
import com.saveourtool.save.orchestrator.kafka.KafkaSender
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.internals.DefaultPartitioner
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.SeekToCurrentErrorHandler
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import java.util.*

/**
 * Kafka producer and consumer configuration
 *
 * @param kafkaProperties
 */
@Configuration
@Profile("dev & kafka")
internal class KafkaConfiguration(
    private val kafkaProperties: KafkaProperties) {
    /**
     * @property requestTopicName
     */
    @Value("\${kafka.test.execution.request.topic.name}")
    lateinit var requestTopicName: String

    /**
     * @property requestTopicNameDlt
     */
    @Value("\${kafka.test.execution.request.topic.name}.DLT")
    lateinit var requestTopicNameDlt: String

    /**
     * @property responseTopicName
     */
    @Value("\${kafka.test.execution.response.topic.name}")
    lateinit var responseTopicName: String

    /**
     * @property consumerGroup
     */
    @Value("\${spring.kafka.consumer.group-id}")
    lateinit var consumerGroup: String

    /**
     * @return kafka producer properties
     */
    @Bean
    fun producerConfig(): Map<String, Any?> {
        val producerProps = HashMap(kafkaProperties.buildProducerProperties())
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java as Object
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java as Object
        producerProps[ProducerConfig.PARTITIONER_CLASS_CONFIG] = DefaultPartitioner::class.java as Object
        return producerProps
    }

    /**
     * @return kafka producer factory
     */
    @Bean
    fun producerFactory(): ProducerFactory<Any, Any> = DefaultKafkaProducerFactory(producerConfig())

    /**
     * @return kafka template
     */
    @Bean
    fun kafkaTemplate(): KafkaTemplate<Any, Any> = KafkaTemplate(producerFactory())

    /**
     * @return kafka consumer properties
     */
    @Bean
    fun consumerConfig(): Map<String, Any?> {
        val consumerProps = HashMap(kafkaProperties.buildConsumerProperties())
        consumerProps[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
        consumerProps[ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG] = "10"
        consumerProps[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = "60000"
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        consumerProps[ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS] = StringDeserializer::class.java
        consumerProps[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = JsonDeserializer::class.java
        consumerProps[ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG] = listOf(CooperativeStickyAssignor::class.java)
        consumerProps[JsonDeserializer.TRUSTED_PACKAGES] = "*"
        return consumerProps
    }

    /**
     * @return consumer factory
     */
    @Bean
    fun consumerFactory(): ConsumerFactory<Any, Any> = DefaultKafkaConsumerFactory(consumerConfig())

    /**
     * @param template
     * @return kafka listener container factory
     */
    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    @Bean
    fun kafkaListenerContainerFactory(template: KafkaTemplate<Any, Any>): ConcurrentKafkaListenerContainerFactory<Any, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
        factory.setConsumerFactory(consumerFactory())
        factory.setConcurrency(1)
        factory.getContainerProperties().setPollTimeout(LISTENER_POLL_TIMEOUT)
        factory.setErrorHandler(
            SeekToCurrentErrorHandler(
                DeadLetterPublishingRecoverer(template)
            )
        )
        return factory
    }

    /**
     * @return kafka agent tasks topic bean
     */
    @Bean
    fun saveAgentTasks(): NewTopic {
        log.info("Create topic: $requestTopicName.")
        return NewTopic(requestTopicName, Optional.empty(), Optional.empty())
    }

    /**
     * @param template
     * @return test execution sender
     */
    @Bean
    fun testExecutionSender(template: KafkaTemplate<Any, Any>): KafkaSender<TestExecutionTaskDto> {
        log.info("Create sender for {} topic.", requestTopicName)
        return KafkaSender(template as KafkaTemplate<String?, TestExecutionTaskDto>, requestTopicName)
    }

    /**
     * @return agentKafkaListener
     */
    @Bean
    fun agentKafkaListener(): AgentKafkaListener {
        log.info("Create listener for {} topic.", responseTopicName)
        return AgentKafkaListener(requestTopicName, consumerGroup)
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaConfiguration::class.java)

        /**
         * @property listenerPollTimeout
         */
        private const val LISTENER_POLL_TIMEOUT = 500L
    }
}
