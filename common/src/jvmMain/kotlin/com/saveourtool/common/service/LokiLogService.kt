package com.saveourtool.common.service

import com.saveourtool.common.utils.StringList
import com.saveourtool.common.utils.debug
import com.saveourtool.common.utils.getLogger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.Logger
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

/**
 * AgentLogService from Loki
 */
class LokiLogService(
    private val config: LokiConfig,
) : LogService {
    private val webClient = WebClient.create(config.url)

    override fun getByContainerName(
        containerName: String,
        from: Instant,
        to: Instant,
        limit: Int,
    ): Mono<StringList> = processQuery(listOf(config.labels.agentContainerName exact containerName), from, to, limit)

    override fun getByApplicationName(
        applicationName: String,
        from: Instant,
        to: Instant,
        limit: Int,
    ): Mono<StringList> = processQuery(
        listOf((config.labels.applicationName?.let { it exact applicationName })
            ?: (config.labels.agentContainerName regex applicationName)),
        from,
        to,
        limit,
    )

    override fun getByExactLabels(
        labels: Map<String, String>,
        from: Instant,
        to: Instant,
        limit: Int,
    ): Mono<StringList> = processQuery(labels.map { (key, value) -> key exact value }, from, to, limit)

    private fun processQuery(
        builtLabels: List<String>,
        from: Instant,
        to: Instant,
        limit: Int,
    ): Mono<StringList> = doQueryRange("{${builtLabels.joinToString(", ")}}", from, to, limit)

    private infix fun String.exact(value: String) = "$this = \"$value\""
    private infix fun String.regex(value: String) = "$this =~ \"$value\""
    private infix fun String.notEqual(value: String) = "$this != \"$value\""
    private infix fun String.regexNotMatch(value: String) = "$this !~ \"$value\""

    private fun doQueryRange(
        query: String,
        start: Instant,
        end: Instant,
        limit: Int,
    ): Mono<StringList> = LokiRequest(query, start, end, limit)
        .doQueryRange(previousResponse = null)
        .expand { previousLokiResponse ->
            previousLokiResponse.nextRequest()
                ?.doQueryRange(previousLokiResponse)
                ?: Mono.empty()
        }
        .flatMapIterable { it.entries.map(LogEntry::toString) }
        .collectList()

    private fun LokiRequest.doQueryRange(
        previousResponse: LokiResponse?,
    ): Mono<LokiResponse> = webClient.get()
        .uri(
            "/loki/api/v1/query_range?query={query}&start={start}&end={end}&direction=forward&limit={limit}",
            query,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(start),
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(end),
            batchSize(),
        )
        .also {
            log.debug {
                "Make #$number request to loki: $this"
            }
        }
        .retrieve()
        .bodyToMono<ObjectNode>()
        .map { it.extractResult(previousResponse?.lastEntries.orEmpty()) }
        .filter { it.isNotEmpty() }
        .map { LokiResponse(this, it) }

    private fun ObjectNode.extractResult(logEntriesToSkip: Set<LogEntry>): List<LogEntry> = this
        .validateFieldValue("status", "success")
        .get("data")
        .validateFieldValue("resultType", "streams")
        .elementsAsSequenceFrom("result")
        .flatMap { it.elementsAsSequenceFrom("values") }
        .map { jsonNode ->
            val (timestampNode, msgNode) = jsonNode.elementsAsSequence().toList()
                .also { child ->
                    require(child.size == 2) {
                        "Only two values are expected in each elements in [values]"
                    }
                }
            val timestamp = timestampNode.asText()
                .fromEpochNanoStr()
            val msg = msgNode.asText()
            LogEntry(timestamp, msg)
        }
        .filterNot(logEntriesToSkip::contains)
        .sorted()  // it's not clear why loki/logcli sorts output -- so we will sort too
        .toList()

    private fun JsonNode.validateFieldValue(fieldName: String, expectedValue: String): JsonNode = also { jsonNode ->
        val actualValue = jsonNode[fieldName].asText()
        require(actualValue == expectedValue) {
            "Invalid $fieldName: $actualValue"
        }
    }

    private fun JsonNode.elementsAsSequence(): Sequence<JsonNode> = (this as ArrayNode).elements().asSequence()

    private fun JsonNode.elementsAsSequenceFrom(fieldName: String): Sequence<JsonNode> = (this[fieldName] as ArrayNode)
        .elementsAsSequence()

    private fun Instant.toEpochNanoStr(): String = "$epochSecond${nano.toString().padStart(NANO_COUNT, '0')}"

    private fun String.fromEpochNanoStr(): Instant {
        val epochSecond = substring(0, length - NANO_COUNT)
        val nano = substring(length - NANO_COUNT)
        return Instant.ofEpochSecond(epochSecond.toLong(), nano.toLong())
    }

    /**
     * @property query
     * @property start
     * @property end
     * @property limit
     * @property number
     */
    private data class LokiRequest(
        val query: String,
        val start: Instant,
        val end: Instant,
        val limit: Int,
        val number: Int = 1,
    ) {
        /**
         * @return batch size for current request
         */
        fun batchSize(): Int = Integer.min(limit, LOKI_LIMIT)
    }

    /**
     * @property request
     * @property entries
     */
    private data class LokiResponse(
        val request: LokiRequest,
        val entries: List<LogEntry>,
    ) {
        val lastEntries: Set<LogEntry> = entries
            .lastOrNull()
            ?.timestamp
            ?.let { lastTimestamp -> entries.filterTo(HashSet()) { it.timestamp == lastTimestamp } }
            ?.also { result ->
                require(result.size < LOKI_LIMIT) {
                    "Need to increase limit in request to loki"
                }
            }
            .orEmpty()

        /**
         * @return next request if it's required
         */
        fun nextRequest(): LokiRequest? = this
            .takeIf { it.entries.isNotEmpty() }
            ?.let {
                request.copy(
                    start = this.lastEntries.first().timestamp,
                    limit = (request.limit - this.entries.size).takeIf { it > 0 } ?: 0,
                    number = request.number + 1
                )
            }
            ?.takeIf { it.limit > 0 }
    }

    /**
     * @property timestamp
     * @property msg
     */
    private data class LogEntry(
        val timestamp: Instant,
        val msg: String,
    ) : Comparable<LogEntry> {
        override fun compareTo(other: LogEntry): Int = timestamp.compareTo(other.timestamp)

        override fun toString(): String = "${dateTimeFormatter.format(timestamp.atZone(ZoneId.systemDefault()))} $msg"
    }

    companion object {
        private val log: Logger = getLogger<LokiLogService>()
        private const val LOKI_LIMIT = 100
        private const val NANO_COUNT = 9
        private val dateTimeFormatter = DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendFraction(ChronoField.NANO_OF_SECOND, 9, 9, true)
            .toFormatter()

        /**
         * @param lokiConfig config of loki service for logging
         * @return [LokiLogService] or [LogService.stub] if config is not provided
         */
        fun createOrStub(lokiConfig: LokiConfig?): LogService =
                lokiConfig?.let { LokiLogService(it) } ?: LogService.stub
    }
}
