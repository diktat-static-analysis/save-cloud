package com.saveourtool.common.service

import com.saveourtool.common.utils.StringList
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * A service which provides logs from container or application
 */
interface LogService {
    /**
     * @param containerName name of container
     * @param from query is from this timestamp in UTC
     * @param to query is to this timestamp in UTC
     * @param limit limit for result
     * @return logs as [Mono] of [String]s
     */
    fun getByContainerName(
        containerName: String,
        from: Instant,
        to: Instant,
        limit: Int,
    ): Mono<StringList>

    /**
     * @param applicationName name of application
     * @param from query is from this timestamp in UTC
     * @param to query is to this timestamp in UTC
     * @param limit limit for result
     * @return logs as [Mono] of [String]s
     */
    fun getByApplicationName(
        applicationName: String,
        from: Instant,
        to: Instant,
        limit: Int,
    ): Mono<StringList>

    /**
     * @param labels [Map] where keys are label names, values are label values
     * @param from query is from this timestamp in UTC
     * @param to query is to this timestamp in UTC
     * @param limit limit for result
     * @return logs as [Mono] of [String]s
     */
    fun getByExactLabels(
        labels: Map<String, String>,
        from: Instant,
        to: Instant,
        limit: Int,
    ): Mono<StringList>

    companion object {
        /**
         * Number of lines that should be fetched from log processor by default
         */
        const val LOG_SIZE_LIMIT_DEFAULT = "1000"

        /**
         * Stub implementation of [LogService]
         */
        val stub: LogService = object : LogService {
            override fun getByContainerName(
                containerName: String,
                from: Instant,
                to: Instant,
                limit: Int,
            ): Mono<StringList> = Mono.just(
                listOf("Stub implementation: requested logs by container name for $containerName from $from to $to with limit $limit")
            )

            override fun getByApplicationName(
                applicationName: String,
                from: Instant,
                to: Instant,
                limit: Int,
            ): Mono<StringList> = Mono.just(
                listOf("Stub implementation: requested logs by application name for $applicationName from $from to $to with limit $limit")
            )

            override fun getByExactLabels(
                labels: Map<String, String>,
                from: Instant,
                to: Instant,
                limit: Int
            ): Mono<StringList> = Mono.just(
                listOf("Stub implementation: requested logs by labels [$labels] from $from to $to with limit $limit")
            )
        }
    }
}
