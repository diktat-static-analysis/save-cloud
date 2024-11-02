package com.saveourtool.save.orchestrator.kafka

import com.saveourtool.common.kafka.KafkaMsg
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.requestreply.CorrelationKey
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.support.SendResult
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.util.concurrent.ListenableFutureCallback
import java.util.concurrent.atomic.AtomicLong

/**
 * Kafka sender
 */
class KafkaSender<V : KafkaMsg>(
    private val template: KafkaTemplate<String?, V>,
    private val topic: String
) {
    /**
     * @param msg
     * @return send result future
     */
    @Suppress("TYPE_ALIAS", "GENERIC_VARIABLE_WRONG_DECLARATION")
    fun sendMessage(msg: V): ListenableFuture<SendResult<String?, V>> {
        val messageId = msg.messageId
        log.info("Sending a message with id {} to topic: {}", messageId, topic)
        log.debug("The message to be sent is: {}", msg)
        val kafkaRecord = ProducerRecord<String?, V>(topic, messageId, msg)
        return template.send(kafkaRecord)
    }

    /**
     * @param topic
     * @param partition
     * @param corrKey
     * @param msg
     * @return send result future
     */
    @Suppress("TYPE_ALIAS")
    fun sendMessage(
        topic: String?,
        partition: Int,
        corrKey: CorrelationKey?,
        msg: V
    ): ListenableFuture<SendResult<String?, V>> {
        val messageId = msg.messageId
        log.info(
            "Sending a message with id $messageId and correlation key to topic: $topic partition $partition"
        )
        log.debug("The message to be sent is: $msg")
        val kafkaRecord: ProducerRecord<String?, V> = ProducerRecord(topic, partition, messageId, msg)
        corrKey?.let {
            kafkaRecord.headers().add(RecordHeader(KafkaHeaders.CORRELATION_ID, corrKey.getCorrelationId()))
        }
        return template.send(kafkaRecord)
    }

    /**
     * @param msg
     * @param successCount
     * @param failedRecords
     */
    @Suppress("TYPE_ALIAS", "TooGenericExceptionCaught")
    fun sendMessage(msg: V, successCount: AtomicLong, failedRecords: MutableSet<String?>) {
        val messageId = msg.messageId
        try {
            val future: ListenableFuture<SendResult<String?, V>> = sendMessage(msg)
            future.addCallback(object : ListenableFutureCallback<SendResult<String?, V>> {
                override fun onSuccess(result: SendResult<String?, V>?) {
                    log.info(
                        "Msg id={} was successfully sent with offset: {}",
                        result?.getProducerRecord()?.key(), result?.getRecordMetadata()?.offset()
                    )
                    successCount.incrementAndGet()
                }

                override fun onFailure(ex: Throwable) {
                    log.error("Unable to send message=[{}].", messageId, ex)
                    failedRecords.add(messageId)
                }
            })
        } catch (e: Exception) {
            log.error("Unable to send task: {}", messageId, e)
            failedRecords.add(messageId)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaSender::class.java)
    }
}
